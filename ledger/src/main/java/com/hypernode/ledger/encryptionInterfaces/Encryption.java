package com.hypernode.ledger.encryptionInterfaces;

import com.hypernode.ledger.ErrorHandling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.NamedParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class Encryption {

    private static String KEY_ALGORITHM;//"RSA";//Key algorithms
    private static String SIGN_ALGORITHM;//"MD5withRSA";//Signature algorithms

    @Value("${encryption.keyAlgo}")
    public void setKeyAlgorithm(String keyAlgorithm) {
        KEY_ALGORITHM = keyAlgorithm;
    }
    @Value("${encryption.signAlgo}")
    public void setSignAlgorithm(String signAlgorithm) {
        SIGN_ALGORITHM = signAlgorithm;
    }

    //client functions
    public static KeyPair createNewKey() {
        KeyPair keyPair = null;
        try {
            if(KEY_ALGORITHM.equals("RSA"))
            {
                keyPair = KeyPairGenerator.getInstance(KEY_ALGORITHM).generateKeyPair();//RSA
            }
            else if(KEY_ALGORITHM.equals("ML-DSA-87"))
            {
                KeyPairGenerator g = KeyPairGenerator.getInstance(KEY_ALGORITHM);
                g.initialize(NamedParameterSpec.ML_DSA_87);
                keyPair = g.generateKeyPair();
            }
            else
            {
                ErrorHandling.logEvent("Invalid encryption algorithm",true, null);
            }
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("generic error during creation of new key",true, null);
        }
        return keyPair;
    }

    public static byte[] base64ToByteArray(String key)
    {
        try
        {
            return Base64.getDecoder().decode(key.replaceAll("\\s+", ""));
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("error",false,e);
            return "".getBytes();
        }
    }

    public static String ByteArrayToBase64(byte[] value)
    {
        try
        {
            return Base64.getEncoder().encodeToString(value);
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("error",false,e);
            return "";
        }
    }

    public static boolean verifySignedMessage(String _data, com.hypernode.ledger.contracts.Signature _signature)
    {
        return verifySignedMessage(_data,_signature.getPublicKey(), _signature.getSignatureValue());
    }

    public static boolean verifySignedMessage(String data, String publicKey, String signature)
    {
        return Encryption.verifySignedMessage(data,Encryption.base64ToByteArray(publicKey),Encryption.base64ToByteArray(signature));
    }
    public static boolean verifySignedMessage(String _data, byte[] publicKey, byte[] _signature)
    {
        try
        {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey);
            PublicKey pubKey = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(pubKey);
            signature.update(_data.getBytes());
            return signature.verify(_signature);
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("signature failed",false,e);
            return false;
        }
    }

    public static String hash(String value)
    {
        MessageDigest md5;
        try
        {
            md5 = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e)
        {
            //throw new RuntimeException(e);
            ErrorHandling.logEvent("somehow md5 is not present in this java version",true,e);
            return "";
        }
        md5.update(StandardCharsets.UTF_8.encode(value));
        return String.format("%032x", new BigInteger(1, md5.digest()));
    }
}
