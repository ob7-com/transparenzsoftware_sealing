package com.metabit.custom.safe.web;

import com.metabit.custom.safe.transparency.verification.VerificationParser;
import com.metabit.custom.safe.transparency.verification.VerificationParserFactory;
import com.metabit.custom.safe.transparency.verification.VerificationType;
import com.metabit.custom.safe.transparency.verification.format.ocmf.OCMFVerificationParser;
import com.metabit.custom.safe.transparency.verification.result.IntrinsicVerified;
import com.metabit.custom.safe.transparency.verification.result.VerificationResult;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.metabit.custom.safe.transparency.verification.format.ocmf.OCMFVerifiedData;
import com.metabit.custom.safe.transparency.verification.xml.Meter;

final class VerifyController
{
    private VerifyController() { }

    // Basic hard limits to protect the endpoint against very large inputs
    private static final int MAX_XML_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final int MAX_XML_TEXT_CHARS = 2 * 1024 * 1024; // ~2 MB

    static void register(Javalin app)
    {
        app.post("/api/verify", VerifyController::handleVerify);
    }

    private static void handleVerify(Context ctx)
    {
        try
        {
            String xml = null;
            UploadedFile xmlFile = ctx.uploadedFile("xml");
            if (xmlFile != null)
            {
                // basic content-type hint (best-effort)
                String ct = xmlFile.contentType();
                if (ct != null && !(ct.contains("xml") || ct.contains("text")))
                {
                    ctx.status(400).json(Map.of("error", "unsupported content-type for xml: " + ct));
                    return;
                }
                byte[] bytes = xmlFile.content().readAllBytes();
                if (bytes.length > MAX_XML_BYTES)
                {
                    ctx.status(413).json(Map.of("error", "xml exceeds size limit"));
                    return;
                }
                xml = new String(bytes, StandardCharsets.UTF_8);
            }
            if (xml == null)
            {
                xml = ctx.formParam("xmlText");
                if (xml != null && xml.length() > MAX_XML_TEXT_CHARS)
                {
                    ctx.status(413).json(Map.of("error", "xmlText exceeds size limit"));
                    return;
                }
            }
            if (xml == null || xml.isEmpty())
            {
                ctx.status(400).json(Map.of("error", "xml is required (file or xmlText)"));
                return;
            }

            // Extract all value elements to find Begin and End transactions
            List<String> valueElements = extractAllValueElements(xml);
            if (valueElements.isEmpty())
            {
                ctx.status(400).json(Map.of("error", "No value elements found in XML"));
                return;
            }

            // Find Begin and End value elements
            String beginValue = null;
            String endValue = null;
            for (String valueElem : valueElements) {
                String context = extractContext(valueElem);
                if ("Transaction.Begin".equals(context)) {
                    beginValue = valueElem;
                } else if ("Transaction.End".equals(context)) {
                    endValue = valueElem;
                }
            }

            // If no explicit Begin/End, use first and last
            if (beginValue == null && !valueElements.isEmpty()) {
                beginValue = valueElements.get(0);
            }
            if (endValue == null && valueElements.size() > 1) {
                endValue = valueElements.get(valueElements.size() - 1);
            } else if (endValue == null && valueElements.size() == 1) {
                endValue = valueElements.get(0); // Single value - might contain both readings
            }

            // Extract signedData blocks
            String beginSignedData = beginValue != null ? extractSignedData(beginValue) : null;
            String endSignedData = endValue != null && endValue != beginValue ? extractSignedData(endValue) : null;

            if (beginSignedData == null)
            {
                ctx.status(400).json(Map.of("error", "signedData not found in XML"));
                return;
            }

            // Determine public key bytes
            byte[] publicKeyDer = null;

            String providedPem = ctx.formParam("publicKeyPem");
            if (providedPem != null && !providedPem.isEmpty())
            {
                String cleaned = providedPem.trim();
                // Check if it has PEM headers
                boolean hasPemHeaders = cleaned.contains("-----BEGIN") || cleaned.contains("-----END");
                
                if (hasPemHeaders)
                {
                    // Remove PEM headers and whitespace, then decode as Base64
                    cleaned = cleaned.replace("-----BEGIN PUBLIC KEY-----", "")
                                    .replace("-----END PUBLIC KEY-----", "")
                                    .replaceAll("\\s", "");
                    try
                    {
                        publicKeyDer = Base64.getDecoder().decode(cleaned);
                    }
                    catch (IllegalArgumentException e)
                    {
                        // If Base64 fails, this is an error (shouldn't happen with valid PEM)
                        throw new IllegalArgumentException("Invalid PEM format: Base64 decoding failed", e);
                    }
                }
                else
                {
                    // No PEM headers - try to detect format (hex or base64)
                    cleaned = cleaned.replaceAll("\\s", ""); // Remove whitespace
                    
                    // Try Base64 first (common format)
                    try
                    {
                        publicKeyDer = Base64.getDecoder().decode(cleaned);
                    }
                    catch (IllegalArgumentException e)
                    {
                        // Base64 failed, try hex decoding
                        try
                        {
                            publicKeyDer = hexStringToByteArray(cleaned);
                        }
                        catch (Exception hexEx)
                        {
                            throw new IllegalArgumentException(
                                "Public key format not recognized. Expected PEM, Base64, or Hex. " +
                                "Base64 error: " + e.getMessage() + ", Hex error: " + hexEx.getMessage(), e);
                        }
                    }
                }
            }
            if (publicKeyDer == null)
            {
                String providedB64 = ctx.formParam("publicKeyBase64");
                if (providedB64 != null && !providedB64.isEmpty())
                {
                    publicKeyDer = Base64.getDecoder().decode(providedB64.trim());
                }
            }
            if (publicKeyDer == null)
            {
                String fromXml = extractPublicKeyFromXml(xml);
                if (fromXml != null)
                {
                    String encoding = extractPublicKeyEncoding(xml);
                    // Handle hex (plain) and base64 encodings
                    if (encoding != null && encoding.equalsIgnoreCase("plain"))
                    {
                        // Hex encoding - convert hex string to bytes
                        publicKeyDer = hexStringToByteArray(fromXml.trim());
                    }
                    else
                    {
                        // Try Base64 (default)
                        try
                        {
                            publicKeyDer = Base64.getDecoder().decode(fromXml.trim());
                        }
                        catch (IllegalArgumentException e)
                        {
                            // If Base64 fails, try hex
                            publicKeyDer = hexStringToByteArray(fromXml.trim());
                        }
                    }
                }
            }
            if (publicKeyDer == null)
            {
                ctx.status(400).json(Map.of("error", "public key missing (provide publicKeyPem/publicKeyBase64 or include <publicKey> in XML)"));
                return;
            }

            // Verify using Transparenzsoftware OCMF parser
            VerificationParser parser = new OCMFVerificationParser();
            
            // Verify begin transaction
            VerificationResult beginResult = null;
            OCMFVerifiedData beginOcmfData = null;
            if (beginSignedData != null) {
                beginResult = parser.parseAndVerify(beginSignedData, publicKeyDer, IntrinsicVerified.NOT_VERIFIED);
                if (beginResult.isVerified() && beginResult.getVerifiedData() instanceof OCMFVerifiedData) {
                    beginOcmfData = (OCMFVerifiedData) beginResult.getVerifiedData();
                }
            }
            
            // Verify end transaction (if different from begin)
            VerificationResult endResult = null;
            OCMFVerifiedData endOcmfData = null;
            if (endSignedData != null && !endSignedData.equals(beginSignedData)) {
                endResult = parser.parseAndVerify(endSignedData, publicKeyDer, IntrinsicVerified.NOT_VERIFIED);
                if (endResult.isVerified() && endResult.getVerifiedData() instanceof OCMFVerifiedData) {
                    endOcmfData = (OCMFVerifiedData) endResult.getVerifiedData();
                }
            } else if (endSignedData == null && beginOcmfData != null) {
                // Single value element - use same data for both
                endOcmfData = beginOcmfData;
                endResult = beginResult;
            }
            
            // Overall verification result: both must be verified
            boolean overallVerified = (beginResult != null && beginResult.isVerified()) &&
                                     (endResult == null || endResult.isVerified());
            
            Map<String, Object> out = new HashMap<>();
            out.put("ok", overallVerified);
            
            // Combine errors from both verifications
            List<Map<String, Object>> allErrors = new ArrayList<>();
            if (beginResult != null) {
                allErrors.addAll(beginResult.getErrorMessages().stream()
                    .map(e -> Map.<String, Object>of(
                            "type", e.getType().name(),
                            "message", e.getMessage(),
                            "localizedMessageCode", e.getLocalizedMessageCode()))
                    .toList());
            }
            if (endResult != null && endResult != beginResult) {
                allErrors.addAll(endResult.getErrorMessages().stream()
                    .map(e -> Map.<String, Object>of(
                            "type", e.getType().name(),
                            "message", e.getMessage(),
                            "localizedMessageCode", e.getLocalizedMessageCode()))
                    .toList());
            }
            out.put("errors", allErrors);
            out.put("format", VerificationType.OCMF.name());
            
            // Extract measurement data if verification was successful
            if (overallVerified && (beginOcmfData != null || endOcmfData != null)) {
                Map<String, Object> measurementData = new HashMap<>();
                
                // Collect all meters from begin and end
                List<Meter> allMeters = new ArrayList<>();
                if (beginOcmfData != null && beginOcmfData.getMeters() != null) {
                    allMeters.addAll(beginOcmfData.getMeters());
                }
                if (endOcmfData != null && endOcmfData != beginOcmfData && endOcmfData.getMeters() != null) {
                    allMeters.addAll(endOcmfData.getMeters());
                }
                
                if (!allMeters.isEmpty()) {
                    // Find START and STOP meters
                    Meter startMeter = null;
                    Meter stopMeter = null;
                    for (Meter meter : allMeters) {
                        if (meter.getType() == Meter.Type.START) {
                            startMeter = meter;
                        } else if (meter.getType() == Meter.Type.STOP) {
                            stopMeter = meter;
                        }
                    }
                    
                    // If no explicit START/STOP types, use first and last from all meters
                    if (startMeter == null && !allMeters.isEmpty()) {
                        startMeter = allMeters.get(0);
                    }
                    if (stopMeter == null && !allMeters.isEmpty()) {
                        stopMeter = allMeters.get(allMeters.size() - 1);
                    }
                    
                    // Extract start meter reading
                    if (startMeter != null) {
                        Map<String, Object> startData = new HashMap<>();
                        startData.put("meterReading", startMeter.getValue());
                        startData.put("unit", "kWh");
                        if (startMeter.getTimestamp() != null) {
                            startData.put("timestamp", startMeter.getTimestamp().format(
                                DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        }
                        measurementData.put("start", startData);
                    }
                    
                    // Extract stop meter reading
                    if (stopMeter != null) {
                        Map<String, Object> stopData = new HashMap<>();
                        stopData.put("meterReading", stopMeter.getValue());
                        stopData.put("unit", "kWh");
                        if (stopMeter.getTimestamp() != null) {
                            stopData.put("timestamp", stopMeter.getTimestamp().format(
                                DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        }
                        measurementData.put("end", stopData);
                    }
                    
                    // Calculate energy delivered (difference)
                    if (startMeter != null && stopMeter != null) {
                        double energyDelivered = stopMeter.getValue() - startMeter.getValue();
                        measurementData.put("energyDelivered", energyDelivered);
                        measurementData.put("energyUnit", "kWh");
                    }
                    
                    // Calculate charging duration
                    if (startMeter != null && startMeter.getTimestamp() != null &&
                        stopMeter != null && stopMeter.getTimestamp() != null) {
                        Duration duration = Duration.between(startMeter.getTimestamp(), stopMeter.getTimestamp());
                        measurementData.put("duration", formatDuration(duration));
                        measurementData.put("durationSeconds", duration.getSeconds());
                    }
                }
                
                // Transaction ID if available (from XML)
                String transactionId = extractTransactionId(xml);
                if (transactionId != null) {
                    measurementData.put("transactionId", transactionId);
                }
                
                // Additional metadata (prefer end if available, otherwise begin)
                OCMFVerifiedData metadataSource = endOcmfData != null ? endOcmfData : beginOcmfData;
                if (metadataSource != null) {
                    if (metadataSource.getMeterSerialNumber() != null) {
                        measurementData.put("meterSerialNumber", metadataSource.getMeterSerialNumber());
                    }
                    if (metadataSource.getMeterModel() != null) {
                        measurementData.put("meterModel", metadataSource.getMeterModel());
                    }
                }
                
                out.put("measurementData", measurementData);
            }
            
            ctx.json(out);
        }
        catch (Exception e)
        {
            // avoid leaking stacktraces and i18n dependencies
            String msg = e.getMessage();
            // If it's a MissingResourceException from i18n, extract a cleaner message
            if (msg != null && msg.contains("Can't find bundle")) {
                msg = "Internal error: missing resource bundle";
            }
            if (msg == null || msg.isBlank()) msg = "verification failed";
            // Log full exception for debugging, but don't expose it
            e.printStackTrace(); // Can be removed in production
            ctx.status(500).json(Map.of("error", msg));
        }
    }

    private static List<String> extractAllValueElements(String xml)
    {
        List<String> valueElements = new ArrayList<>();
        // Match all <value>...</value> blocks
        Pattern p = Pattern.compile("<value[^>]*>(.*?)</value>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        while (m.find()) {
            valueElements.add(m.group(0)); // Include the full <value>...</value> element
        }
        return valueElements;
    }

    private static String extractContext(String valueElement)
    {
        Pattern p = Pattern.compile("context\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(valueElement);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private static String extractSignedData(String xml)
    {
        // Match <![CDATA[ ... ]]> inside <signedData>
        Pattern p = Pattern.compile("<signedData[^>]*>\\s*<!\\[CDATA\\[(.*?)]]>\\s*</signedData>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private static String extractPublicKeyFromXml(String xml)
    {
        Pattern p = Pattern.compile("<publicKey[^>]*>(.*?)</publicKey>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private static String extractPublicKeyEncoding(String xml)
    {
        Pattern p = Pattern.compile("<publicKey[^>]*encoding\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private static String extractTransactionId(String xml)
    {
        Pattern p = Pattern.compile("transactionId\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private static String formatDuration(Duration duration)
    {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private static byte[] hexStringToByteArray(String hex) throws IllegalArgumentException
    {
        if (hex == null || hex.isEmpty())
        {
            throw new IllegalArgumentException("Hex string is empty");
        }
        
        // Remove any whitespace
        hex = hex.replaceAll("\\s", "");
        
        int len = hex.length();
        if (len % 2 != 0)
        {
            throw new IllegalArgumentException("Hex string must have even length, got: " + len);
        }
        
        // Validate hex characters
        if (!hex.matches("[0-9a-fA-F]+"))
        {
            throw new IllegalArgumentException("Hex string contains invalid characters");
        }
        
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high < 0 || low < 0)
            {
                throw new IllegalArgumentException("Invalid hex character at position " + i);
            }
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }
}


