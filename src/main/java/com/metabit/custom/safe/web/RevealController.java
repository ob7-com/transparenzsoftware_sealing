package com.metabit.custom.safe.web;

import com.metabit.custom.safe.safeseal.SAFESealRevealer;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import javax.crypto.BadPaddingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

final class RevealController
{
    private RevealController() { }

    static void register(Javalin app)
    {
        app.post("/api/reveal", RevealController::handleReveal);
    }

    private static void handleReveal(Context ctx)
    {
        try
        {
            String publicKeyPem = ctx.formParam("publicKeyPem");
            if (publicKeyPem == null)
            {
                UploadedFile keyFile = ctx.uploadedFile("publicKeyPemFile");
                if (keyFile != null)
                {
                    publicKeyPem = new String(keyFile.content().readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            if (publicKeyPem == null || publicKeyPem.isEmpty())
            {
                ctx.status(400).json(Map.of("error", "publicKeyPem is required (text or file)"));
                return;
            }

            byte[] sealed = null;
            UploadedFile sealedFile = ctx.uploadedFile("sealed");
            if (sealedFile != null)
            {
                sealed = sealedFile.content().readAllBytes();
            }
            if (sealed == null)
            {
                String sealedBase64 = ctx.formParam("sealedBase64");
                if (sealedBase64 != null && !sealedBase64.isEmpty())
                {
                    sealed = Base64.getDecoder().decode(sealedBase64);
                }
            }
            if (sealed == null)
            {
                ctx.status(400).json(Map.of("error", "sealed is required (file or sealedBase64)"));
                return;
            }

            Integer algorithmVersion = parseIntOrDefault(ctx.formParam("algorithmVersion"), 2);

            RSAPublicKey publicKey = KeyUtil.parsePublicKeyPem(publicKeyPem);

            SAFESealRevealer revealer = new SAFESealRevealer(algorithmVersion);
            byte[] payload = revealer.reveal(publicKey, null, sealed);

            Map<String, Object> result = new HashMap<>();
            result.put("payloadBase64", Base64.getEncoder().encodeToString(payload));
            result.put("utf8Preview", utf8Preview(payload, 200));
            result.put("size", payload.length);
            ctx.json(result);
        }
        catch (InvalidKeySpecException | NoSuchAlgorithmException e)
        {
            ctx.status(400).json(Map.of("error", "Invalid RSA key: " + e.getMessage()));
        }
        catch (BadPaddingException e)
        {
            ctx.status(400).json(Map.of("error", "Reveal failed (invalid seal or parameters)"));
        }
        catch (Exception e)
        {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static String utf8Preview(byte[] data, int maxChars)
    {
        try
        {
            String s = new String(data, StandardCharsets.UTF_8);
            if (s.length() > maxChars)
            {
                return s.substring(0, maxChars) + "...";
            }
            return s;
        }
        catch (Exception ignored)
        {
            return new String(data, Charset.defaultCharset());
        }
    }

    private static Integer parseIntOrDefault(String s, int def)
    {
        try { return s == null ? def : Integer.parseInt(s); } catch (Exception ignored) { return def; }
    }
}


