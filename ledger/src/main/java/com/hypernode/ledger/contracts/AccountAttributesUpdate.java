package com.hypernode.ledger.contracts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypernode.ledger.encryptionInterfaces.Encryption;
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountAttributesUpdate
{
    private String from;
    private String name;
    private String description;
    private String delegated;
    private int blockId;
    private String previousBlockHash;
    private String signatureValue;

    public boolean validate(int blockId, String blockHash)
    {
        if (this.blockId == blockId && this.previousBlockHash.equals(blockHash))
        {
            return Encryption.verifySignedMessage(this.getStringToSign(),this.getFrom(),this.getSignatureValue());
        }
        return false;
    }

    public String getFrom() { return this.from;}
    public void setFrom(String from) {this.from = from;}
    public String getDelegated() { return this.delegated;}
    public void setDelegated(String delegated) {this.delegated = delegated;}
    public int getBlockId() {return blockId;}
    public void setBlockId(int blockId) {this.blockId = blockId;}
    public String getSignatureValue() {return signatureValue;}
    public void setSignatureValue(String signatureValue) {this.signatureValue = signatureValue;}
    @JsonIgnore
    public String getStringToSign(){return this.getFrom() + this.getDelegated() + this.getName() + this.getBlockId() + this.getPreviousBlockHash();}
    public String getPreviousBlockHash() {return previousBlockHash;}
    public void setPreviousBlockHash(String previousBlockHash) {this.previousBlockHash = previousBlockHash;}
    public String getName() {return name;}
    public void setName(String name)
    {
        int maxLength = 200;
        if (name == null || name.length() <= maxLength) {
            this.name = name;
        } else {
            this.name = name.substring(0, maxLength);
        }
    }
    public String getDescription() {return description;}
    public void setDescription(String description)
    {
        int maxLength = 2000;
        if (description == null || description.length() <= maxLength) {
            this.description = description;
        } else {
            this.description = description.substring(0, maxLength);
        }
    }

    @JsonCreator
    public static AccountAttributesUpdate create
            (
                    @JsonProperty("from") String publicKeyFrom,               // Maps to "publicKeyFrom" in JSON
                    @JsonProperty("name") String name,               // Maps to "publicKeyFrom" in JSO
                    @JsonProperty("description") String description,               // Maps to "publicKeyFrom" in JSO
                    @JsonProperty("delegated") String publicKeyDelegated,     // Maps to "publicKeyDelegated" in JSON
                    @JsonProperty("blockId") int blockId,                             // Maps to "blockId" in JSON
                    @JsonProperty("previousRevisionHash") String previousRevisionHash, // Maps to "previousRevisionHash" in JSON
                    @JsonProperty("signatureValue") String signatureValue
            )
    {
        AccountAttributesUpdate _this = new AccountAttributesUpdate();
        _this.from = publicKeyFrom;
        _this.setName(name);
        _this.setDescription(description);
        _this.delegated = publicKeyDelegated;
        _this.blockId = blockId;
        _this.previousBlockHash = previousRevisionHash;
        _this.signatureValue = signatureValue;
        return _this;
    }
    @Override
    public String toString()
    {
        return  "publicKey:"+this.getFrom() +
                "name:"+this.getName()+
                "description:"+this.getDescription()+
                "delegated:"+this.getDelegated() +
                "blockId:"+this.getBlockId() +
                "previousBlockHash:"+this.getPreviousBlockHash() +
                "signatureValue:"+this.getSignatureValue()
                ;
    }
    @JsonIgnore
    public String uniqueString()
    {
        return  this.getFrom() +
                ":"+this.getName()+
                ":"+this.getDescription()+
                ":"+this.getDelegated() +
                ":"+this.getBlockId() +
                ":"+this.getPreviousBlockHash() +
                ":"+this.getSignatureValue()
                ;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountAttributesUpdate d = (AccountAttributesUpdate) o;
        return this.uniqueString().equals(d.uniqueString());
    }

    @JsonIgnore
    @Override
    public int hashCode() {
        return this.uniqueString().hashCode();
    }
}
