package com.hypernode.ledger.encryptionInterfaces;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.hypernode.ledger.ErrorHandling;
import com.hypernode.ledger.webService.WebServiceCaller;

public class EncryptionEntity_ExternalServer implements EncryptionEntity_BaseInterface {

    String connectionString;
    @Override
    public String getPublicKey()
    {
        try
        {
            return WebServiceCaller.callServerMethodThrows(connectionString, "getPublicKey", null, new TypeReference<String>() {
            });
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("error",false,e);
            return "";
        }
    }

    @Override
    public String signMessage(String _message)
    {
        try
        {
            return WebServiceCaller.callServerMethodThrows(connectionString, "signMessage", _message, new TypeReference<String>() {
            });
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("Signature did not work",false,e);
            return "";
        }

    }
@JsonCreator
    public static EncryptionEntity_ExternalServer create(String connectionString)
    {
        EncryptionEntity_ExternalServer webServiceCredentials = new EncryptionEntity_ExternalServer();
        webServiceCredentials.connectionString = connectionString;
        return webServiceCredentials;
    }

}
