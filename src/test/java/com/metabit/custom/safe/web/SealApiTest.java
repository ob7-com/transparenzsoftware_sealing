package com.metabit.custom.safe.web;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SealApiTest
{
    static Javalin app;
    static int PORT = 8081;

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
    void sealAndRevealRoundtrip() throws Exception
    {
        KeyPair kp = KeyUtil.generateRsaKeyPair(2048);
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey prv = (RSAPrivateKey) kp.getPrivate();
        String pubPem = KeyUtil.toPemPublic(pub);
        String prvPem = KeyUtil.toPemPrivate(prv);

        String payload = "hello web api";

        // Seal
        String sealForm = "privateKeyPem=" + url(prvPem) +
                "&payloadText=" + url(payload) +
                "&algorithmVersion=2&compression=true";
        HttpResponse<String> sealResp = http().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/seal"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(sealForm))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, sealResp.statusCode());
        String sealedBase64 = extractJsonValue(sealResp.body(), "base64");
        assertNotNull(sealedBase64);

        // Reveal
        String revealForm = "publicKeyPem=" + url(pubPem) +
                "&sealedBase64=" + url(sealedBase64) +
                "&algorithmVersion=2";
        HttpResponse<String> revResp = http().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/reveal"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(revealForm))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, revResp.statusCode());
        String payloadBase64 = extractJsonValue(revResp.body(), "payloadBase64");
        assertNotNull(payloadBase64);
        String roundtrip = new String(Base64.getDecoder().decode(payloadBase64), StandardCharsets.UTF_8);
        assertEquals(payload, roundtrip);
    }

    private static HttpClient http() { return HttpClient.newHttpClient(); }

    private static String url(String s)
    {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // Tiny JSON value extractor for flat string fields
    private static String extractJsonValue(String json, String key)
    {
        String q = "\"" + key + "\"\s*:\s*\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(q + "(.*?)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}


