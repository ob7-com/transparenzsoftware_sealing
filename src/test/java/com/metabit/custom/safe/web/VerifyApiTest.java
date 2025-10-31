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
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class VerifyApiTest
{
    static Javalin app;
    static int PORT = 8082;

    // Test data files
    private static final String TEST_XML_FILE = "testdata/ocmf/destre10118001-2025-10-31-11_31_20-76358978.xml";
    private static final String TEST_PUBLIC_KEY_HEX = "3059301306072A8648CE3D020106082A8648CE3D030107034200048a8760ab2c8726788c513584d0cd1cccc40004bb570af5ed1e944685c0648ed40ef98f57b373a66965db565351bfabf01617da5c53147240c113c3946fca786e";

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
    void verify_with_embedded_public_key_succeeds() throws Exception
    {
        String xml = "" +
                "<?xml version=\"1.0\" encoding=\"utf-8\"?><values>" +
                "<value transactionId=\"76007442\">" +
                "<signedData format=\"ocmf\" encoding=\"plain\"><![CDATA[OCMF|{\"FV\":\"1.0\",\"GI\":\"LEM DCBM\",\"GS\":\"1242050810\",\"GV\":\"v1\",\"PG\":\"T61\",\"MV\":\"LEM\",\"MS\":\"1242050810\",\"MF\":\"MU-2.3.0.1_SU-0.1.3.0\",\"IS\":true,\"IL\":\"TRUSTED\",\"IF\":[\"RFID_NONE\",\"OCPP_RS_TLS\",\"ISO15118_NONE\",\"PLMN_NONE\"],\"IT\":\"NONE\",\"ID\":\"O-b9d89940be03-10802\",\"CT\":\"EVSEID\",\"CI\":\"FR*A23*E45B*78D\",\"RD\":[{\"TM\":\"2025-10-26T12:50:00,000+0100 R\",\"TX\":\"B\",\"RV\":1319.135,\"RI\":\"1-0:1.8.0\",\"RU\":\"kWh\",\"RT\":\"DC\",\"EF\":\"\",\"ST\":\"G\",\"UC\":{\"UN\":\"No_Comp\",\"UI\":0,\"UR\":0}},{\"RV\":7.641,\"RI\":\"1-0:2.8.0\",\"RU\":\"kWh\",\"ST\":\"G\"},{\"TM\":\"2025-10-26T13:06:07,000+0100 R\",\"TX\":\"E\",\"RV\":1328.281,\"RI\":\"1-0:1.8.0\",\"RU\":\"kWh\",\"ST\":\"G\"},{\"RV\":7.641,\"RI\":\"1-0:2.8.0\",\"RU\":\"kWh\",\"ST\":\"G\"}]}|{\"SA\":\"ECDSA-secp256r1-SHA256\",\"SD\":\"304502202FFD57E359C7470E8717FBBB0A5BCE0CCF96EC56268CC95B8862FE3E1CD07EA90221009DE6CF26F3D79411D083CD4BEE9715EE852E730DECFCD83036DF0DDF9EFBC0AD\"}]]></signedData>" +
                "<publicKey encoding=\"plain\">MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEkHMaeos/DzzyiEI3lThC8rGWOTQE7cFIW2e05ceyw9W9WTShJIWs62jDQezHBod1w6lT2G1uTHD0rAWqdTMhHA==</publicKey>" +
                "</value></values>";

        String body = "xmlText=" + url(xml);
        HttpResponse<String> resp = http().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/verify"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, resp.statusCode());
        String ok = extractJsonValue(resp.body(), "ok");
        assertNotNull(ok);
        assertEquals("true", ok);
    }

    @Test
    void verify_with_separate_public_key_base64_succeeds() throws Exception
    {
        // Read test XML file
        String xml = readTestResource(TEST_XML_FILE);
        assertNotNull(xml, "Test XML file not found");

        // Extract first signedData block (Transaction.Begin)
        String signedDataXml = extractFirstValue(xml);

        // Convert hex public key to Base64-DER
        byte[] publicKeyBytes = hexStringToByteArray(TEST_PUBLIC_KEY_HEX);
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyBytes);

        // Remove publicKey from XML to test separate key provision
        String xmlWithoutKey = removePublicKeyFromXml(signedDataXml);

        String body = "xmlText=" + url(xmlWithoutKey) + "&publicKeyBase64=" + url(publicKeyBase64);
        HttpResponse<String> resp = http().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/verify"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, resp.statusCode());
        String ok = extractJsonValue(resp.body(), "ok");
        assertNotNull(ok);
        assertEquals("true", ok);
    }

    @Test
    void verify_with_real_xml_file_embedded_key_succeeds() throws Exception
    {
        // Read test XML file
        String xml = readTestResource(TEST_XML_FILE);
        assertNotNull(xml, "Test XML file not found");

        // Extract first value block (Transaction.Begin)
        String firstValue = extractFirstValue(xml);

        String body = "xmlText=" + url(firstValue);
        HttpResponse<String> resp = http().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/verify"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, resp.statusCode());
        String ok = extractJsonValue(resp.body(), "ok");
        assertNotNull(ok);
        assertEquals("true", ok, "Verification should succeed with real XML data");
    }

    @Test
    void verify_with_wrong_public_key_fails() throws Exception
    {
        String xml = readTestResource(TEST_XML_FILE);
        assertNotNull(xml, "Test XML file not found");
        String firstValue = extractFirstValue(xml);
        String xmlWithoutKey = removePublicKeyFromXml(firstValue);

        // Use a wrong public key (different key)
        String wrongKeyHex = "3059301306072A8648CE3D020106082A8648CE3D03010703420004" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
        byte[] wrongKeyBytes = hexStringToByteArray(wrongKeyHex);
        String wrongKeyBase64 = Base64.getEncoder().encodeToString(wrongKeyBytes);

        String body = "xmlText=" + url(xmlWithoutKey) + "&publicKeyBase64=" + url(wrongKeyBase64);
        HttpResponse<String> resp = http().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/verify"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, resp.statusCode());
        String ok = extractJsonValue(resp.body(), "ok");
        assertNotNull(ok);
        assertEquals("false", ok, "Verification should fail with wrong public key");
    }

    @Test
    void verify_with_tampered_signed_data_fails() throws Exception
    {
        String xml = readTestResource(TEST_XML_FILE);
        assertNotNull(xml, "Test XML file not found");
        String firstValue = extractFirstValue(xml);

        // Tamper with the signedData by modifying the JSON payload
        String tamperedXml = firstValue.replace("\"RV\":0.00", "\"RV\":999.99");

        String body = "xmlText=" + url(tamperedXml);
        HttpResponse<String> resp = http().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/verify"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, resp.statusCode());
        String ok = extractJsonValue(resp.body(), "ok");
        assertNotNull(ok);
        assertEquals("false", ok, "Verification should fail with tampered data");
    }

    @Test
    void missing_xml_returns_400() throws Exception
    {
        String body = "publicKeyPem=INVALID";
        HttpResponse<String> resp = http().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/verify"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(400, resp.statusCode());
    }

    @Test
    void missing_public_key_returns_400() throws Exception
    {
        String xml = readTestResource(TEST_XML_FILE);
        assertNotNull(xml, "Test XML file not found");
        String firstValue = extractFirstValue(xml);
        String xmlWithoutKey = removePublicKeyFromXml(firstValue);

        String body = "xmlText=" + url(xmlWithoutKey);
        HttpResponse<String> resp = http().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/verify"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("public key missing"), "Error message should mention missing public key");
    }

    // --- Helper methods ---

    private static HttpClient http() { return HttpClient.newHttpClient(); }

    private static String url(String s)
    {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String extractJsonValue(String json, String key)
    {
        String q = "\"" + key + "\"\\s*:\\s*";
        java.util.regex.Matcher mBool = java.util.regex.Pattern.compile(q + "(true|false)").matcher(json);
        if (mBool.find()) return mBool.group(1);
        java.util.regex.Matcher mStr = java.util.regex.Pattern.compile(q + "\"(.*?)\"").matcher(json);
        return mStr.find() ? mStr.group(1) : null;
    }

    private static String readTestResource(String resourcePath) throws IOException
    {
        try (InputStream is = VerifyApiTest.class.getClassLoader().getResourceAsStream(resourcePath))
        {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extractFirstValue(String xml)
    {
        // Extract first <value>...</value> block
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(<value[^>]*>.*?</value>)", java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(xml);
        if (m.find())
        {
            return "<?xml version=\"1.0\" encoding=\"utf-8\"?><values>" + m.group(1) + "</values>";
        }
        return xml; // fallback
    }

    private static String removePublicKeyFromXml(String xml)
    {
        // Remove <publicKey>...</publicKey> element
        return xml.replaceAll("<publicKey[^>]*>.*?</publicKey>", "")
                .replaceAll("(?s)<publicKey.*?</publicKey>", "");
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


