package com.metabit.custom.safe.web;

import com.metabit.custom.safe.safeseal.SAFESealSealer;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import javax.crypto.BadPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

final class SealController
{
    private SealController() { }

    static void register(Javalin app)
    {
        app.post("/api/seal", SealController::handleSeal);
    }

    private static void handleSeal(Context ctx)
    {
        try
        {
            String privateKeyPem = ctx.formParam("privateKeyPem");
            if (privateKeyPem == null)
            {
                UploadedFile keyFile = ctx.uploadedFile("privateKeyPemFile");
                if (keyFile != null)
                {
                    privateKeyPem = new String(keyFile.content().readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            if (privateKeyPem == null || privateKeyPem.isEmpty())
            {
                ctx.status(400).json(Map.of("error", "privateKeyPem is required (text or file)"));
                return;
            }

            byte[] payload = null;
            UploadedFile payloadFile = ctx.uploadedFile("payload");
            if (payloadFile != null)
            {
                payload = payloadFile.content().readAllBytes();
            }
            if (payload == null)
            {
                String payloadText = ctx.formParam("payloadText");
                if (payloadText != null)
                {
                    payload = payloadText.getBytes(StandardCharsets.UTF_8);
                }
            }
            if (payload == null)
            {
                ctx.status(400).json(Map.of("error", "payload is required (file or payloadText)"));
                return;
            }

            Integer algorithmVersion = parseIntOrDefault(ctx.formParam("algorithmVersion"), 2);
            boolean compression = parseBoolean(ctx.formParam("compression"), true);
            Long uniqueId = parseLongOrNull(ctx.formParam("uniqueId"));

            RSAPrivateKey privateKey = KeyUtil.parsePrivateKeyPem(privateKeyPem);

            SAFESealSealer sealer = new SAFESealSealer(algorithmVersion);
            sealer.setCompressionMode(compression);
            byte[] sealed = sealer.seal(privateKey, null, payload, uniqueId);

            Map<String, Object> result = new HashMap<>();
            result.put("base64", Base64.getEncoder().encodeToString(sealed));
            result.put("hexPreview", hexPreview(sealed, 64));
            result.put("size", sealed.length);
            ctx.json(result);
        }
        catch (InvalidKeySpecException | NoSuchAlgorithmException e)
        {
            ctx.status(400).json(Map.of("error", "Invalid RSA key: " + e.getMessage()));
        }
        catch (BadPaddingException e)
        {
            ctx.status(400).json(Map.of("error", "Sealing failed"));
        }
        catch (Exception e)
        {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static String hexPreview(byte[] data, int maxBytes)
    {
        int len = Math.min(data.length, Math.max(0, maxBytes));
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = 0; i < len; i++)
        {
            sb.append(String.format("%02x", data[i]));
        }
        if (data.length > len)
        {
            sb.append("...");
        }
        return sb.toString();
    }

    private static boolean parseBoolean(String val, boolean defaultVal)
    {
        if (val == null) return defaultVal;
        String v = val.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("on") || v.equals("yes");
    }

    private static Integer parseIntOrDefault(String s, int def)
    {
        try { return s == null ? def : Integer.parseInt(s); } catch (Exception ignored) { return def; }
    }

    private static Long parseLongOrNull(String s)
    {
        try { return s == null || s.isEmpty() ? null : Long.parseLong(s); } catch (Exception ignored) { return null; }
    }
}


