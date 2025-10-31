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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            // Extract OCMF signedData block (very lenient)
            String signedData = extractSignedData(xml);
            if (signedData == null)
            {
                ctx.status(400).json(Map.of("error", "signedData not found in XML"));
                return;
            }

            // Determine public key bytes
            byte[] publicKeyDer = null;

            String providedPem = ctx.formParam("publicKeyPem");
            if (providedPem != null && !providedPem.isEmpty())
            {
                publicKeyDer = Base64.getDecoder().decode(
                        providedPem.replace("-----BEGIN PUBLIC KEY-----", "")
                                .replace("-----END PUBLIC KEY-----", "")
                                .replaceAll("\\s", "")
                );
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
            VerificationResult result = parser.parseAndVerify(signedData, publicKeyDer, IntrinsicVerified.NOT_VERIFIED);

            Map<String, Object> out = new HashMap<>();
            out.put("ok", result.isVerified());
            // Serialize errors manually to avoid i18n dependency
            out.put("errors", result.getErrorMessages().stream()
                    .map(e -> Map.of(
                            "type", e.getType().name(),
                            "message", e.getMessage(),
                            "localizedMessageCode", e.getLocalizedMessageCode()))
                    .toList());
            out.put("format", VerificationType.OCMF.name());
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

    private static byte[] hexStringToByteArray(String hex)
    {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}


