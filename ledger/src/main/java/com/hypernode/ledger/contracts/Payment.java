package com.hypernode.ledger.contracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hypernode.ledger.client.ExternalPayment;
import com.hypernode.ledger.encryptionInterfaces.Encryption;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_BaseInterface;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
@JsonIgnoreProperties(ignoreUnknown = true)
public class Payment
{

    private String from;//public key or name
    private String to;//public key or name
    private String publicKeyFrom;//public key
    private String publicKeyTo;//public key
    private String comment;//comment of the transaction
    private BigDecimal amount;//value of the exchanged currency
    private String signature;//digital signature to validate the emission from the PublicKeyFrom
    private String validatorSignature;
    private int blockId;
    private String lastStatusHash;
    // use the block id to sign with the payer
    // and use the validator signature to sign the payer signature + the hash of the last revision
    @JsonIgnore
    public String getStringToSign()
    {
        return this.getFrom()
                + ":" + this.amount
                + ":" + this.getTo()
                + ":" + this.comment
                + ":" + this.blockId;
    }
    @JsonIgnore
    public String getStringToSignValidator()
    {
        return this.getStringToSign()
                + this.getSignature()
                + this.lastStatusHash;
    }

    public static Set<Payment> validatePayments(Set<DistributedLedgerAccount> _setDistributedLedgerAccount, Set<Payment> _payments)
    {
        return _payments.stream().filter( payment ->
                _setDistributedLedgerAccount.stream().anyMatch(account -> account.getPublicKey().equals(payment.getPublicKeyFrom()))
        ).collect(Collectors.toSet());
    }
    public static Set<DistributedLedgerAccount> processPayments(Set<DistributedLedgerAccount> _setDistributedLedgerAccount, Set<Payment> _payments, BigDecimal _transactionCost)
    {
        String validatorNode;
        Map<String,BigDecimal> sums = _payments.stream().collect(Collectors.toMap(
                payment -> payment.publicKeyFrom, payment -> payment.amount, BigDecimal::add));

        Set<String> banned = _setDistributedLedgerAccount.stream()
                .filter(account -> account.getAmount().compareTo(sums.getOrDefault(account.getPublicKey(), BigDecimal.ZERO)) < 0)
                .map(DistributedLedgerAccount::getPublicKey)
                .collect(Collectors.toSet());

        _payments.stream().filter(p -> banned.contains(p.getPublicKeyFrom())).toList().forEach(_payments::remove);

        Map<String,BigDecimal> from = _payments.stream().filter(p -> !banned.contains(p.getPublicKeyFrom())).collect(Collectors.toMap(
                Payment::getPublicKeyFrom, payment -> payment.amount, BigDecimal::add));
        Map<String,BigDecimal> to = _payments.stream().filter(p -> !banned.contains(p.getPublicKeyFrom()) && p.getAmount().compareTo(_transactionCost) >0).collect(Collectors.toMap(
                Payment::getPublicKeyTo, payment -> payment.amount.subtract(_transactionCost) , BigDecimal::add));

        Set<String> affectedPublicKeys = new HashSet<>();
        affectedPublicKeys.addAll(from.keySet());
        affectedPublicKeys.addAll(to.keySet());

        Set<DistributedLedgerAccount> updatedCurrencies = _setDistributedLedgerAccount.stream()
                .filter(c -> !banned.contains(c.getPublicKey())
                && !affectedPublicKeys.contains(c.getPublicKey())
                ).collect(Collectors.toSet());

        updatedCurrencies.addAll(
                affectedPublicKeys.stream()
                .map(a -> {

                    DistributedLedgerAccount old =  _setDistributedLedgerAccount.stream()
                            .filter(d -> d.getPublicKey().equals(a)).findFirst()
                            .orElse(DistributedLedgerAccount.create(a,"",BigDecimal.ZERO,a));
                    return DistributedLedgerAccount.create(old.getPublicKey(),
                            old.getName(),
                            old.getAmount()
                                .add(to.getOrDefault(a, BigDecimal.ZERO))
                                .subtract(from.getOrDefault(a, BigDecimal.ZERO)),
                        old.getValidatorNode());
                }
                ).filter(c -> c.getAmount().compareTo(BigDecimal.ZERO)>0).collect(Collectors.toSet())
        );
        return updatedCurrencies;
    }

    public static Payment createNew(
            String from,//public key
            String to,//public key
            String paymentComment,//comment of the transaction
            BigDecimal amount,//value of the exchanged currency
            int blockId,//which block will the payment be processed in
            String signature//digital signature to validate the emission from the PublicKeyFrom
    )
    {
        Payment payment = new Payment();
        payment.from = from;//public key
        payment.to = to;//public key
        payment.comment = paymentComment;//comment of the transaction
        payment.amount = amount;//value of the exchanged currency
        payment.blockId = blockId;
        payment.signature = signature;//digital signature to validate the emission from the PublicKeyFrom
        return payment;
    }

    public static Payment createFromExternalPayment(ExternalPayment externalPayment)
    {
        return Payment.createNew(
                externalPayment.getFrom(),
                externalPayment.getTo(),
                externalPayment.getComment(),
                externalPayment.getAmount(),
                externalPayment.getBlockId(),
                externalPayment.getSignature()
        );
    }

    public Payment validatorSignPayment(EncryptionEntity_BaseInterface _encryptionEntity, String hash)
    {
        this.lastStatusHash = hash;
        this.validatorSignature = _encryptionEntity.signMessage(this.getStringToSignValidator());
        return this;
    }
    @JsonIgnore
    public boolean validateSignature()
    {
        return Encryption.verifySignedMessage(this.getStringToSign(), this.getPublicKeyFrom(), this.getSignature());

    }
    public boolean validateValidatorSignature(String publicKeyValidator, String previousHash)
    {
        if(!this.lastStatusHash.equals(previousHash))
        {
            return false;
        }
        return Encryption.verifySignedMessage(this.getStringToSignValidator(), publicKeyValidator, this.getValidatorSignature());

    }

    public static Set<Payment> populatePublicKeys(Set<Payment> payments, Set<DistributedLedgerAccount> accounts)
    {
        Map<String, String> map = accounts.stream().filter(a -> a.getName() != null && !a.getName().isEmpty())
                .collect(Collectors.toMap(DistributedLedgerAccount::getName, DistributedLedgerAccount::getPublicKey));

        return
                payments.stream().map(p -> p.setPublicKey(map.getOrDefault(p.getFrom(),p.getFrom())
                        ,map.getOrDefault(p.getTo(), p.getTo()))).collect(Collectors.toSet());

    }

    public static Set<Payment> removePublicKeys(Set<Payment> payments, Set<DistributedLedgerAccount> accounts)
    {

        return payments.stream().map(p -> p.setPublicKey("","")
                        ).collect(Collectors.toSet());
    }
    @JsonIgnore
    @Override public String toString()
    {
        return  "from:"+ getFrom() +
                "to:"+ getTo()+
                "comment:"+ getComment()+
                "amount:"+getAmount()+
                "signature:"+ getSignature() +
                "validatorSignature:"+this.validatorSignature+
                "blockId:"+getBlockId()+
                "lastStatusHash:"+lastStatusHash;
    }
    @JsonIgnore
    public String uniqueString()
    {
        return  getFrom() +
                ":"+ getTo()+
                ":"+ getComment()+
                ":"+getAmount()+
                ":"+ getSignature() +
                ":"+this.validatorSignature+
                ":"+getBlockId()+
                ":"+lastStatusHash;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment d = (Payment) o;
        return this.uniqueString().equals(d.uniqueString());
    }

    @Override
    public int hashCode() {
        return this.uniqueString().hashCode();
    }
//getters and setters
    public String getPublicKeyFrom() { return this.publicKeyFrom;}
    public String getPublicKeyTo() { return this.publicKeyTo;}
    public String getComment() {return comment;}
    public void setComment(String comment) {this.comment = comment;}
    public BigDecimal getAmount() {return amount;}
    public void setAmount(BigDecimal amount) {this.amount = amount;}
    public String getSignature() {return signature;}
    public int getBlockId() {return blockId;}
    public void setBlockId(int blockId) {this.blockId = blockId;}
    public String getValidatorSignature() {return validatorSignature;}
    public void setValidatorSignature(String validatorSignature) {this.validatorSignature = validatorSignature;}
    public String getLastStatusHash() {return lastStatusHash;}
    public void setLastStatusHash(String lastStatusHash) {this.lastStatusHash = lastStatusHash;}
    public void setPublicKeyFrom(String publicKeyFrom) {this.publicKeyFrom = publicKeyFrom;}
    public void setPublicKeyTo(String publicKeyTo) {this.publicKeyTo = publicKeyTo;}
    public String getFrom() {return from;}
    public void setFrom(String from) {this.from = from;}
    public String getTo() {return to;}
    public void setTo(String to) {this.to = to;}

    public Payment setPublicKey(String publicKeyFrom, String publicKeyTo)
    {
        this.publicKeyFrom = publicKeyFrom;
        this.publicKeyTo = publicKeyTo;
        return this;
    }

}
