package com.hypernode.ledger.contracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hypernode.ledger.encryptionInterfaces.Encryption;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidatorMessageDataContract {

    int id;
    private Set<ValidatorNode> validatorNodes = new HashSet<>();
    private Set<Payment> paymentSet = ConcurrentHashMap.newKeySet();
    private LedgerParameters votedParameterChanges = new LedgerParameters();
    private Set<AccountAttributesUpdate> accountAttributesUpdateSet = new HashSet<>();

    public void mergeMessage(ValidatorMessageDataContract _message) {
        this.paymentSet.addAll(_message.paymentSet);
        this.validatorNodes.addAll(_message.validatorNodes);
        this.accountAttributesUpdateSet.addAll(_message.accountAttributesUpdateSet);
    }
    @JsonIgnore
    public String getStringToSign()
    {
        return this.validatorNodes.stream().map(ValidatorNode::uniqueString).sorted().collect(Collectors.joining("|"))
                + ":" + this.paymentSet.stream().map(Payment::uniqueString).sorted().collect(Collectors.joining("|"))
                + ":" + this.accountAttributesUpdateSet.stream().map(AccountAttributesUpdate::uniqueString).sorted().collect(Collectors.joining("|"))
                + ":" + this.votedParameterChanges.getStringToSign()
                ;
    }

    public boolean isValid(String validator, StatusDataContract status) {
        if(this.paymentSet.size() > status.getLedgerParameters().getMaxTransactionsPerBlock()
        || this.validatorNodes.stream().anyMatch(v -> !v.validate())
        || this.getPaymentSet().stream().anyMatch(p -> !p.validateSignature()
            || (p.getComment().length() > status.getLedgerParameters().getMaxMessageLength()
                    && status.getLedgerParameters().getMaxMessageLength() > 0)
            || !p.validateValidatorSignature(validator,status.getHashPreviousBlock()))
        || this.accountAttributesUpdateSet.stream().anyMatch(v -> v.validate(status.getId(), status.getHashPreviousBlock()))
        )
        {
            return false;
        }
        return true;
    }

    public static ValidatorMessageDataContract createEmpty(int _id)
    {
        ValidatorMessageDataContract ret = new ValidatorMessageDataContract();
        ret.setValidatorNodes(new HashSet<>());
        ret.setPaymentSet(new HashSet<>());
        ret.setVotedParameterChanges(new LedgerParameters());
        ret.setId(_id);
        return ret;
    }
//getters and setters
    public int getId() {return id;}
    public void setId(int id) {this.id = id;}
    public Set<ValidatorNode> getValidatorNodes() {return validatorNodes;}
    public void setValidatorNodes(Set<ValidatorNode> validatorNodes) {this.validatorNodes = validatorNodes;}
    public Set<Payment> getPaymentSet() {return paymentSet;}
    public void setPaymentSet(Set<Payment> paymentSet) {this.paymentSet = paymentSet;}
    public LedgerParameters getVotedParameterChanges() {return votedParameterChanges;}
    public void setVotedParameterChanges(LedgerParameters votedParameterChanges) {this.votedParameterChanges = votedParameterChanges;}
    public Set<AccountAttributesUpdate> getVotingDelegationSet() {return accountAttributesUpdateSet;}
    public void setVotingDelegationSet(Set<AccountAttributesUpdate> accountAttributesUpdateSet) {this.accountAttributesUpdateSet = accountAttributesUpdateSet;}

}
