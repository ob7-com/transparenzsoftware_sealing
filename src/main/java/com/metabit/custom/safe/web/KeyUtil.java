package com.metabit.custom.safe.web;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class KeyUtil
{
    private KeyUtil() { }

    static RSAPublicKey parsePublicKeyPem(final String pem)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        String cleaned = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(cleaned);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        return (RSAPublicKey) kf.generatePublic(spec);
    }

    static RSAPrivateKey parsePrivateKeyPem(final String pem)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        String cleaned = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(cleaned);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    static KeyPair generateRsaKeyPair(final int keySize)
            throws NoSuchAlgorithmException
    {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(keySize);
        return kpg.generateKeyPair();
    }

    static String toPemPublic(final RSAPublicKey key)
    {
        String b64 = Base64.getEncoder().encodeToString(key.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----\n";
    }

    static String toPemPrivate(final RSAPrivateKey key)
    {
        String b64 = Base64.getEncoder().encodeToString(key.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + b64 + "\n-----END PRIVATE KEY-----\n";
    }

    // --- EC key helpers for Transparenzsoftware inputs (e.g., OCMF ECDSA P-256)

    static PublicKey parseEcPublicKeyPem(final String pem)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        String cleaned = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(cleaned);
        KeyFactory kf = KeyFactory.getInstance("EC");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        return kf.generatePublic(spec);
    }

    static PublicKey parseEcPublicKeyBase64Der(final String base64Der)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        byte[] der = Base64.getDecoder().decode(base64Der.trim());
        KeyFactory kf = KeyFactory.getInstance("EC");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        return kf.generatePublic(spec);
    }
}


