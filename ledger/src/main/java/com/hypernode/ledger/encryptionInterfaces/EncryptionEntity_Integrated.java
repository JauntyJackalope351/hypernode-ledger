package com.hypernode.ledger.encryptionInterfaces;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.hypernode.ledger.ErrorHandling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
//@JsonTypeName("Integrated")
@Component
public class EncryptionEntity_Integrated implements EncryptionEntity_BaseInterface {

    private static String KEY_ALGORITHM  ;//Key algorithm
    private static String SIGN_ALGORITHM ;//Signature algorithm

    @Value("${encryption.keyAlgo}")
    public void setKeyAlgorithm(String keyAlgorithm) {
        KEY_ALGORITHM = keyAlgorithm;
    }
    @Value("${encryption.signAlgo}")
    public void setSignAlgorithm(String signAlgorithm) {
        SIGN_ALGORITHM = signAlgorithm;
    }


    //this base one will load it in memory
    private String privateKey;
    private String publicKey;

    @Override
    public String getPublicKey() {return publicKey;}
    @Override
    public String signMessage(String _message)
    {
        try
        {

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Encryption.base64ToByteArray(this.privateKey));
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PrivateKey priKey = keyFactory.generatePrivate(keySpec);
            java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(priKey);
            signature.update(_message.getBytes());//Set the data to be calculated
            return Encryption.ByteArrayToBase64(signature.sign());

        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("error",false,e);
            return "";
        }
    }

    @JsonCreator
    public static EncryptionEntity_Integrated create(
            String publicKey,
            String privateKey)
    {
        EncryptionEntity_Integrated webServiceCredentials = new EncryptionEntity_Integrated();
        webServiceCredentials.privateKey = privateKey.replaceAll("\\s+", "");
        webServiceCredentials.publicKey = publicKey.replaceAll("\\s+", "");
        return webServiceCredentials;
    }

}
