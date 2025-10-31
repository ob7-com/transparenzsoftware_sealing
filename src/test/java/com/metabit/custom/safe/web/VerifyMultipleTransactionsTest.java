package com.metabit.custom.safe.web;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class VerifyMultipleTransactionsTest
{
    static Javalin app;
    static int PORT = 8083;

    // Test XML files with Begin and End transactions
    private static final String TEST_XML_1 = "testdata/xml_verification/destre10178001-2025-10-31-15_20_46-76421062.xml";
    private static final String TEST_XML_2 = "testdata/xml_verification/destre10207002-2025-10-31-15_20_40-76421718.xml";
    private static final String TEST_XML_3 = "testdata/xml_verification/destre10223002-2025-10-31-15_20_31-76423679.xml";

    @BeforeAll
    static void start() {
        app = ServerMain.create();
        app.start(PORT);
    }

    @AfterAll
    static void stop() {
        if (app != null) app.stop();
    }

    @Test
    void verify_transaction_1_with_begin_and_end() throws Exception
    {
        String xml = readTestResource(TEST_XML_1);
        assertNotNull(xml, "Test XML file 1 not found");

        HttpResponse<String> resp = sendVerifyRequest(xml);
        assertEquals(200, resp.statusCode(), "Expected HTTP 200");

        String responseBody = resp.body();
        assertTrue(extractJsonBoolean(responseBody, "ok"), "Verification should succeed");

        // Check measurement data is present
        assertTrue(responseBody.contains("measurementData"), "Response should contain measurementData");

        // Check transaction ID
        String transactionId = extractJsonString(responseBody, "measurementData", "transactionId");
        assertNotNull(transactionId, "Transaction ID should be present");
        assertEquals("76421062", transactionId, "Transaction ID should match");

        // Check energy delivered (should be around 8.28 kWh)
        String energyDelivered = extractJsonString(responseBody, "measurementData", "energyDelivered");
        assertNotNull(energyDelivered, "Energy delivered should be present");
        double energy = Double.parseDouble(energyDelivered);
        assertTrue(energy > 8.0 && energy < 8.5, "Energy delivered should be around 8.28 kWh, got: " + energy);

        // Check start meter reading
        String startReading = extractNestedJsonString(responseBody, "measurementData", "start", "meterReading");
        assertNotNull(startReading, "Start meter reading should be present");
        double start = Double.parseDouble(startReading);
        assertEquals(0.0, start, 0.01, "Start meter reading should be 0.00");

        // Check end meter reading
        String endReading = extractNestedJsonString(responseBody, "measurementData", "end", "meterReading");
        assertNotNull(endReading, "End meter reading should be present");
        double end = Double.parseDouble(endReading);
        assertTrue(end > 8.0 && end < 8.5, "End meter reading should be around 8.28, got: " + end);

        // Check meter model
        String meterModel = extractJsonString(responseBody, "measurementData", "meterModel");
        assertNotNull(meterModel, "Meter model should be present");
        assertTrue(meterModel.contains("EM340"), "Meter model should contain EM340");

        // Check timestamps are present
        String startTimestamp = extractNestedJsonString(responseBody, "measurementData", "start", "timestamp");
        assertNotNull(startTimestamp, "Start timestamp should be present");

        String endTimestamp = extractNestedJsonString(responseBody, "measurementData", "end", "timestamp");
        assertNotNull(endTimestamp, "End timestamp should be present");

        // Check duration is calculated
        String duration = extractJsonString(responseBody, "measurementData", "duration");
        assertNotNull(duration, "Duration should be calculated");
    }

    @Test
    void verify_transaction_2_with_begin_and_end() throws Exception
    {
        String xml = readTestResource(TEST_XML_2);
        assertNotNull(xml, "Test XML file 2 not found");

        HttpResponse<String> resp = sendVerifyRequest(xml);
        assertEquals(200, resp.statusCode(), "Expected HTTP 200");

        String responseBody = resp.body();
        assertTrue(extractJsonBoolean(responseBody, "ok"), "Verification should succeed");

        // Check transaction ID
        String transactionId = extractJsonString(responseBody, "measurementData", "transactionId");
        assertEquals("76421718", transactionId, "Transaction ID should match");

        // Check energy delivered (should be around 2.60 kWh)
        String energyDelivered = extractJsonString(responseBody, "measurementData", "energyDelivered");
        assertNotNull(energyDelivered, "Energy delivered should be present");
        double energy = Double.parseDouble(energyDelivered);
        assertTrue(energy > 2.5 && energy < 2.7, "Energy delivered should be around 2.60 kWh, got: " + energy);
    }

    @Test
    void verify_transaction_3_with_begin_and_end() throws Exception
    {
        String xml = readTestResource(TEST_XML_3);
        assertNotNull(xml, "Test XML file 3 not found");

        HttpResponse<String> resp = sendVerifyRequest(xml);
        assertEquals(200, resp.statusCode(), "Expected HTTP 200");

        String responseBody = resp.body();
        assertTrue(extractJsonBoolean(responseBody, "ok"), "Verification should succeed");

        // Check transaction ID
        String transactionId = extractJsonString(responseBody, "measurementData", "transactionId");
        assertEquals("76423679", transactionId, "Transaction ID should match");

        // Check energy delivered (should be around 4.09 kWh)
        String energyDelivered = extractJsonString(responseBody, "measurementData", "energyDelivered");
        assertNotNull(energyDelivered, "Energy delivered should be present");
        double energy = Double.parseDouble(energyDelivered);
        assertTrue(energy > 4.0 && energy < 4.2, "Energy delivered should be around 4.09 kWh, got: " + energy);
    }

    // --- Helper methods ---

    private static HttpClient http() { return HttpClient.newHttpClient(); }

    private static String url(String s)
    {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static HttpResponse<String> sendVerifyRequest(String xml) throws Exception
    {
        String body = "xmlText=" + url(xml);
        return http().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/verify"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private static boolean extractJsonBoolean(String json, String key)
    {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher m = p.matcher(json);
        return m.find() && "true".equals(m.group(1));
    }

    private static String extractJsonString(String json, String parentKey, String key)
    {
        // Find the parent object and extract nested key
        Pattern p = Pattern.compile(
                "\"" + Pattern.quote(parentKey) + "\"\\s*:\\s*\\{([^}]+)\\}",
                Pattern.DOTALL);
        Matcher parentMatcher = p.matcher(json);
        if (parentMatcher.find()) {
            String parentContent = parentMatcher.group(1);
            Pattern keyPattern = Pattern.compile(
                    "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher keyMatcher = keyPattern.matcher(parentContent);
            if (keyMatcher.find()) {
                return keyMatcher.group(1);
            }
            // Try number format (without quotes)
            Pattern numPattern = Pattern.compile(
                    "\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9.]+)");
            Matcher numMatcher = numPattern.matcher(parentContent);
            if (numMatcher.find()) {
                return numMatcher.group(1);
            }
        }
        return null;
    }

    private static String extractNestedJsonString(String json, String parentKey, String nestedKey, String key)
    {
        // Find nested object: parentKey -> nestedKey -> key
        Pattern p = Pattern.compile(
                "\"" + Pattern.quote(parentKey) + "\"\\s*:\\s*\\{.*?\"" + Pattern.quote(nestedKey) + 
                "\"\\s*:\\s*\\{([^}]+)\\}",
                Pattern.DOTALL);
        Matcher parentMatcher = p.matcher(json);
        if (parentMatcher.find()) {
            String nestedContent = parentMatcher.group(1);
            Pattern keyPattern = Pattern.compile(
                    "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher keyMatcher = keyPattern.matcher(nestedContent);
            if (keyMatcher.find()) {
                return keyMatcher.group(1);
            }
            // Try number format (without quotes)
            Pattern numPattern = Pattern.compile(
                    "\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9.]+)");
            Matcher numMatcher = numPattern.matcher(nestedContent);
            if (numMatcher.find()) {
                return numMatcher.group(1);
            }
        }
        return null;
    }

    private static String readTestResource(String resourcePath) throws IOException
    {
        try (InputStream is = VerifyMultipleTransactionsTest.class.getClassLoader().getResourceAsStream(resourcePath))
        {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

