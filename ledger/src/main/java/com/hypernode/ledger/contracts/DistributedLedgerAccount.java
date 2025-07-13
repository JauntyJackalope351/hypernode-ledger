package com.hypernode.ledger.contracts;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypernode.ledger.ErrorHandling;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DistributedLedgerAccount
{
    private String name = "";
    private String publicKey = "";
    private BigDecimal amount;
    private String validatorNode;

    public static DistributedLedgerAccount find(Set<DistributedLedgerAccount> _set, String _publicKeyFrom)
    {
        try
        {
            return _set.stream().filter(account -> account.getPublicKey().equals(_publicKeyFrom)).findFirst().get();
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("empty find",false,e);
            return new DistributedLedgerAccount();
        }
    }
    public static boolean exists(Set<DistributedLedgerAccount> _set, String _publicKeyFrom)
    {
        return _set.stream().anyMatch(account -> account.getPublicKey().equals(_publicKeyFrom));
    }
    public static Set<DistributedLedgerAccount> updateAccountAttributes(final Set<DistributedLedgerAccount> _currencies, final Set<AccountAttributesUpdate> _delegations)
    {
        Set<DistributedLedgerAccount> ret = _currencies;
        Map<String,String> delegationsString = _delegations.stream().collect(Collectors.toMap(AccountAttributesUpdate::getFrom, AccountAttributesUpdate::getDelegated));
        Map<String,String> newName = _delegations.stream().collect(Collectors.toMap(AccountAttributesUpdate::getFrom, AccountAttributesUpdate::getName));
        Set<String> existingNames = _currencies.stream().map(DistributedLedgerAccount::getName).collect(Collectors.toSet());

        final Map<String,String> finalNewName = newName.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.toList()))
                .entrySet().stream()
                .filter(e -> e.getValue().size() == 1) // Keep groups with only one entry (unique values)
                .flatMap(e -> e.getValue().stream()) // Flatten the lists back into a stream of original entries
                .filter(entry -> !existingNames.contains(entry.getValue())) // Filter out banned values
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        Set<DistributedLedgerAccount> update = _currencies.stream().filter(c -> delegationsString.containsKey(c.getPublicKey())).collect(Collectors.toSet());
        ret.removeAll(update);
        ret.addAll(update.stream().map(c -> DistributedLedgerAccount
                .create(c.getPublicKey(),finalNewName.getOrDefault(c.getPublicKey(),c.getName()),c.getAmount(),delegationsString.get(c.getPublicKey()))).collect(Collectors.toSet()));
        return ret;
    }

    public static Map<String, BigDecimal> calculateAmountDelegated(Set<DistributedLedgerAccount> _set)
    {
        return _set.stream().collect(Collectors.toMap(
                DistributedLedgerAccount::getValidatorNode, DistributedLedgerAccount::getAmount, BigDecimal::add));
    }
    @JsonCreator
    public static DistributedLedgerAccount create
            (
                    @JsonProperty("publicKey") String publicKey,          // Maps to "publicKey" in JSON
                    @JsonProperty("name") String name,          // Maps to "publicKey" in JSON
                    @JsonProperty("amount") BigDecimal amount,             // Maps to "amount" in JSON
                    @JsonProperty("validatorNode") String validatorNode
            )
    {
        DistributedLedgerAccount distributedLedgerAccount = new DistributedLedgerAccount();
        distributedLedgerAccount.publicKey = publicKey;
        distributedLedgerAccount.name = name;
        distributedLedgerAccount.amount = amount;
        distributedLedgerAccount.validatorNode = validatorNode;
        return distributedLedgerAccount;
    }

    @Override
    public String toString()
    {
        return "DistributedLedgerAccount{" +
                "publicKeyFrom=" + this.getPublicKey() +
                ",name=" + name +
                ",amount=" + amount +
                ",validatorNode=" + getValidatorNode() +
                '}';
    }
    public String uniqueString()
    {
        return this.getPublicKey() +
                ":" + name +
                ":" + amount +
                ":" + getValidatorNode();
    }
    public String toJson() {
        return "{" +
                "\"publicKeyFrom\"=\"" + this.getPublicKey() + "\",\n" +
                "\"name\"=\"" + name + "\",\n" +
                "\"amount\"=\"" + amount + "\",\n" +
                "\"validatorNode\"=\"" + getValidatorNode() + "\",\n" +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistributedLedgerAccount d = (DistributedLedgerAccount) o;
        return this.uniqueString().equals(d.uniqueString());
    }

    @Override
    public int hashCode() {
        return this.uniqueString().hashCode();
    }

//getters and setters
    public BigDecimal getAmount() { return this.amount; }
    public String getPublicKey() { return this.publicKey;}
    public String getValidatorNode() { return this.validatorNode;}
    public void setValidatorNode(String validatorNode){this.validatorNode = validatorNode;}
    public void setPublicKey(String publicKey) {this.publicKey = publicKey;}
    public void setAmount(BigDecimal amount) {this.amount = amount;}
    public String getName() {return name;}
    public void setName(String name) {
        this.name = name;
        if(name.length()>200)
        {
            this.name = name.substring(0,200);
        }
    }//TODO accountNameLength
}
