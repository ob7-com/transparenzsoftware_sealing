package com.metabit.custom.safe.web;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

final class KeysController
{
    private KeysController() { }

    static void register(Javalin app)
    {
        app.post("/api/keys/generate", KeysController::handleGenerate);
    }

    private static void handleGenerate(Context ctx)
    {
        int keySize = parseIntOrDefault(ctx.queryParam("keySize"), 2048);
        try
        {
            KeyPair kp = KeyUtil.generateRsaKeyPair(keySize);
            RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
            RSAPrivateKey prv = (RSAPrivateKey) kp.getPrivate();
            ctx.json(Map.of(
                    "publicKeyPem", KeyUtil.toPemPublic(pub),
                    "privateKeyPem", KeyUtil.toPemPrivate(prv)
            ));
        }
        catch (NoSuchAlgorithmException e)
        {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static int parseIntOrDefault(String s, int def)
    {
        try { return s == null ? def : Integer.parseInt(s); } catch (Exception ignored) { return def; }
    }
}


