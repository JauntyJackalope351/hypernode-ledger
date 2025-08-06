package com.hypernode.ledger.client;
import com.fasterxml.jackson.core.type.TypeReference;
import com.hypernode.ledger.contracts.StatusDataContract;
import com.hypernode.ledger.contracts.AccountAttributesUpdate;
import com.hypernode.ledger.encryptionInterfaces.Encryption;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_BaseInterface;
import com.hypernode.ledger.webService.WebServiceCaller;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClientEngine {
    private EncryptionEntity_BaseInterface encryptionEntity;
    String endpoint;

    public static ClientEngine create(EncryptionEntity_BaseInterface entity, String endpoint)
    {
        ClientEngine clientMethods = new ClientEngine();
        clientMethods.encryptionEntity = entity;
        clientMethods.endpoint = endpoint;
        return clientMethods;
    }

    public BigDecimal getAmountAvailable(String publicKey)
    {
        return WebServiceCaller.callServerMethodThrows(endpoint, "getAmount", publicKey, new TypeReference<BigDecimal>() {});
    }

    public List<ExternalPayment> sendPayments(List<ExternalPayment> paymentList)
    {
        return WebServiceCaller.callServerMethodThrows(endpoint, "spend", paymentList, new TypeReference<List<ExternalPayment>>() {});
    }

    public int getNextAvailablePaymentBlock()
    {
        return 1+ WebServiceCaller.callServerMethodThrows(endpoint, "getBlockId", null, new TypeReference<Integer>() {});
    }
    public String getAccountTotal(String publicKey)
    {
        return WebServiceCaller.callServerMethodThrows(endpoint, "AccountTotals", publicKey, new TypeReference<String>() {});
    }
    public StatusDataContract getStatus()
    {
        StatusDataContract ret =  WebServiceCaller.callServerMethodThrows(endpoint, "getStatus", null, new TypeReference<StatusDataContract>() {});
        if(Encryption.hash(ret.getStringToSign()).equals(ret.getHash()))
        {
            return ret;
        }
        return null;
    }

    public void changeVoteDelegation(AccountAttributesUpdate json)
    {
        AccountAttributesUpdate accountAttributesUpdate = new AccountAttributesUpdate();
        accountAttributesUpdate.setFrom(this.encryptionEntity.getPublicKey());
        accountAttributesUpdate.setName(json.getName());
        accountAttributesUpdate.setDescription(json.getDescription());
        accountAttributesUpdate.setDelegated(json.getDelegated());
        accountAttributesUpdate.setBlockId(json.getBlockId());
        accountAttributesUpdate.setPreviousBlockHash(json.getPreviousBlockHash());
        accountAttributesUpdate.setSignatureValue(this.encryptionEntity.signMessage(accountAttributesUpdate.getStringToSign()));
        WebServiceCaller.callServerMethodThrows(endpoint, "updateAccountAttributes", accountAttributesUpdate, new TypeReference<AccountAttributesUpdate>() {});
    }

    public void setEndpoint(String endpoint) {this.endpoint = endpoint;}
    public String getEndpoint() {return endpoint;}
    public EncryptionEntity_BaseInterface getEncryptionEntity(){ return encryptionEntity;}
    public void setEncryptionEntity(EncryptionEntity_BaseInterface credentials){ encryptionEntity = credentials;}
}
