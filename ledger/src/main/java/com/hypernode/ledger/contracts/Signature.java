package com.hypernode.ledger.contracts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypernode.ledger.encryptionInterfaces.Encryption;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_BaseInterface;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Signature
{
    private String publicKey;
    private String signatureValue;
    private String messageValue;
    @JsonCreator
    public static Signature create
            (
                    @JsonProperty("publicKey") String publicKey,         // Map to "publicKey" in JSON
                    @JsonProperty("messageValue") String messageValue,   // Map to "messageValue" in JSON
                    @JsonProperty("signatureValue") String signatureValue // Map to "signatureValue" in JSON)
            )
    {
        Signature ret = new Signature();
        ret.signatureValue = signatureValue;
        ret.publicKey = publicKey;
        ret.messageValue = messageValue;
        return ret;
    }

    public static Signature create(EncryptionEntity_BaseInterface encryptionEntity, String messageValue, boolean saveMessage)
    {
        Signature ret = new Signature();
        ret.signatureValue = encryptionEntity.signMessage(messageValue);
        ret.publicKey = encryptionEntity.getPublicKey();
        if(saveMessage)
        {
            ret.messageValue = messageValue;
        }
        return ret;
    }

    public boolean validate(String message)
    {
        return Encryption.verifySignedMessage(message,this.getPublicKey(),this.getSignatureValue());
    }
    public boolean validate()
    {
        return Encryption.verifySignedMessage(this.getMessageValue(),this.getPublicKey(),this.getSignatureValue());
    }

    public static String listToString(Set<Signature> messages)
    {
        String ret = "";
        if(messages == null)
        {
            return "";
        }
        return  messages.stream()//.filter(Signature::validate)
                        .sorted(Comparator.comparing(Signature::uniqueString))
                        .map(Signature::uniqueString)
                        .collect(Collectors.joining("|")
        );
    }
    public String uniqueString() {
        return Encryption.hash(this.getPublicKey()) + ";" + this.getMessageValue() +";" + Encryption.hash(this.getSignatureValue()) + ";";
    }

    public static Set<Signature> publicKeyToName(Set<Signature> signatures, Set<DistributedLedgerAccount> accounts) {
        Map<String, String> map = accounts.stream().filter(a -> (a.getName() != null && !a.getName().isEmpty()))
                .collect(Collectors.toMap(DistributedLedgerAccount::getPublicKey, DistributedLedgerAccount::getName));
        return signatures.stream().map(p -> Signature.create(map.getOrDefault(p.getPublicKey(),p.getPublicKey()),p.messageValue,p.signatureValue)).collect(Collectors.toSet());
    }

    public static Signature publicKeyToName(Signature s, Set<DistributedLedgerAccount> accounts)
    {
        String name = accounts.stream()
                .filter(a -> s.getPublicKey().equals(a.getPublicKey()) && a.getName() != null && !a.getName().isEmpty())
                .map(DistributedLedgerAccount::getName).findFirst().orElse(s.getPublicKey());

        return Signature.create(name,s.messageValue,s.signatureValue);
    }

    public static Set<Signature> nameToPublicKey(Set<Signature> signatures, Set<DistributedLedgerAccount> accounts)
    {
        Map<String, String> map = accounts.stream().filter(a -> (a.getName() != null && !a.getName().isEmpty()))
                .collect(Collectors.toMap(DistributedLedgerAccount::getName, DistributedLedgerAccount::getPublicKey));
        return signatures.stream().filter(
                        p -> map.containsKey(p.getPublicKey())
                ).map(p -> Signature.create(map.getOrDefault(p.getPublicKey(),p.getPublicKey()),p.messageValue,p.signatureValue)).collect(Collectors.toSet());
    }

    public static Signature nameToPublicKey(Signature s, Set<DistributedLedgerAccount> accounts)
    {
        String name = accounts.stream()
                .filter(a -> s.getPublicKey().equals(a.getName())).map(DistributedLedgerAccount::getPublicKey)
                .findFirst().orElse(s.getPublicKey());

        return Signature.create(name,s.messageValue,s.signatureValue);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Signature d = (Signature) o;
        return this.uniqueString().equals(d.uniqueString());
    }

    @Override
    public int hashCode() {
        return this.uniqueString().hashCode();
    }
//getters and setters
    public String getPublicKey() { return this.publicKey; }
    public String getSignatureValue() { return this.signatureValue; }
    public String getMessageValue() { return this.messageValue; }
}
