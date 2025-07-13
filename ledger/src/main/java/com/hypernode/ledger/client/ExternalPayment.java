package com.hypernode.ledger.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypernode.ledger.encryptionInterfaces.Encryption;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_BaseInterface;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalPayment {
    private String from;//public key or name
    private String to;//public key or name
    private String comment;//comment of the transaction
    private BigDecimal amount;//value of the exchanged currency
    private String signature;//digital signature to validate the emission from the PublicKeyFrom
    private int blockId;
    @JsonCreator
    public static ExternalPayment createJson(
            @JsonProperty("from") String publicKeyFrom,
            @JsonProperty("to") String publicKeyTo,
            @JsonProperty("comment") String paymentComment,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("blockId") int blockId,
            @JsonProperty("signature") String signature)
    {
        ExternalPayment externalPayment = new ExternalPayment();
        externalPayment.from = publicKeyFrom;
        externalPayment.to = publicKeyTo;
        externalPayment.comment = paymentComment;
        externalPayment.amount = amount;
        externalPayment.blockId = blockId;
        externalPayment.signature = signature;

        return externalPayment;
    }

    public static ExternalPayment create(String publicKeyFrom,String publicKeyTo, String paymentComment, BigDecimal amount, int blockId, EncryptionEntity_BaseInterface encryptionEntity) {
        ExternalPayment externalPayment = new ExternalPayment();
        externalPayment.from = publicKeyFrom;
        externalPayment.to = publicKeyTo;
        externalPayment.comment = paymentComment;
        externalPayment.amount = amount;
        externalPayment.blockId = blockId;
        externalPayment.signature = encryptionEntity.signMessage(externalPayment.getStringToSign());
        return externalPayment;
    }

    public String getStringToSign()
    {
        return this.getFrom()
                +":"+ this.amount
                +":"+ this.getTo()
                +":"+ this.comment
                +":"+ this.blockId;
    }

    public void signPayment(EncryptionEntity_BaseInterface encryptionEntity)
    {
        this.signature =
                encryptionEntity.signMessage(this.getStringToSign());
    }

    public boolean verifyClientSignature()
    {
        return Encryption.verifySignedMessage(this.getStringToSign(), from, signature) ;
    }

    public String getFrom() {return from;}
    public String getTo() {return to;}
    public String getComment() {return comment;}
    public BigDecimal getAmount() {return amount;}
    public String getSignature() {return signature;}
    public int getBlockId() {return blockId;}

}
