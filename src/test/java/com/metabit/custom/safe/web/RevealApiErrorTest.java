package com.metabit.custom.safe.web;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class RevealApiErrorTest
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
    void invalidPemReturns400() throws Exception
    {
        String body = "publicKeyPem=INVALID&sealedBase64=QQ==";
        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/reveal"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Invalid RSA key"));
    }
}


