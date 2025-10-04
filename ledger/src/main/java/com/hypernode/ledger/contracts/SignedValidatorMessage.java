package com.hypernode.ledger.contracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hypernode.ledger.encryptionInterfaces.Encryption;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_BaseInterface;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignedValidatorMessage
{
    private ValidatorMessageDataContract contract;
    private Signature originalSignature;
    private Set<Signature> validatorsSignatures;//publicKey,signature
    //Map<Integer,List<String>> signatureChainList; old idea of keeping all the paths. new idea only signs the message
    //this contains the instances of the signature and who signed them last
    @JsonIgnore
    public String getStringToSign()
    {
        return contract.getStringToSign();
        //this needs to be just the contract because the signatures in the validationsignatures list
        // need to have the same message to sign
    }
    public static SignedValidatorMessage create(ValidatorMessageDataContract _contract, EncryptionEntity_BaseInterface _encryptionEntity,StatusDataContract status)
    {
        SignedValidatorMessage signedValidatorMessage = new SignedValidatorMessage();
        ValidatorMessageDataContract contract = ValidatorMessageDataContract.createEmpty(_contract.getId());
        contract.setPaymentSet(Payment.populatePublicKeys(
            _contract.getPaymentSet().stream()
                    .map(p -> p.validatorSignPayment(_encryptionEntity,status.getHashPreviousBlock()))
                    .collect(Collectors.toSet()),status.getDistributedLedgerAccounts()));
        contract.setValidatorNodes(new HashSet<>(_contract.getValidatorNodes()));
        contract.setVotingDelegationSet(new HashSet<>(_contract.getVotingDelegationSet()));
        contract.setVotedParameterChanges(_contract.getVotedParameterChanges());

        signedValidatorMessage.contract = contract;
        Signature localSignature = Signature.create(_encryptionEntity, signedValidatorMessage.getStringToSign(),false);
        signedValidatorMessage.originalSignature = localSignature;
        Set<Signature> signatureList = new HashSet<>();
        signatureList.add(localSignature);
        signedValidatorMessage.validatorsSignatures = signatureList;
        return signedValidatorMessage;
    }
    public static SignedValidatorMessage findFirstByPublicKey(Set<SignedValidatorMessage> set, String publicKey)
    {
        return set.stream().filter(signedValidatorMessage -> signedValidatorMessage.originalSignature.getPublicKey().equals(publicKey)).findFirst().get();
    }

    public static void mergeValue(Set<SignedValidatorMessage> _set, SignedValidatorMessage _message,EncryptionEntity_BaseInterface _encryptionEntity)
    {
        SignedValidatorMessage previousMessage;
        SignedValidatorMessage modifiedMessage;
        Signature messageSignature;
        if(!_message.validateSignature())
        {
            return;
        }
        try
        {//modify old element
            previousMessage = _set.stream()
                    .filter(signedValidatorMessage -> signedValidatorMessage.originalSignature.equals(_message.originalSignature))
                    .findFirst().get();
            previousMessage.validatorsSignatures.addAll(_message.validatorsSignatures.stream().filter(s -> s.validate(_message.getStringToSign())).toList());

        }
        catch(Exception e)
        {//old element not found, new element
            modifiedMessage = _message;
            //modifiedMessage.validatorsSignatures = new HashSet<>();
            messageSignature = Signature.create(_encryptionEntity,_message.getStringToSign(),false);
            modifiedMessage.validatorsSignatures.add(messageSignature);
            _set.add(modifiedMessage);
        }
    }

    public static String SetToString(Set<SignedValidatorMessage> _messages)
    {
        if(_messages == null)
        {
            return "";
        }
        return _messages.stream().map(SignedValidatorMessage::uniqueString).sorted().collect(Collectors.joining("|"));
        //return Signature.listToString(_messages.stream().map(SignedValidatorMessage::getOriginalSignature).collect(Collectors.toSet()));
    }
    @JsonIgnore
    public boolean validateSignature()
    {
        return Encryption.verifySignedMessage(this.getStringToSign(),this.originalSignature);
    }

    public SignedValidatorMessage publicKeyToName(Set<DistributedLedgerAccount> accounts)
    {
        SignedValidatorMessage ret = new SignedValidatorMessage();

        ret.contract = this.getContract();
        ret.contract.setPaymentSet(Payment.removePublicKeys(ret.contract.getPaymentSet(),accounts));
        ret.validatorsSignatures = Signature.publicKeyToName(this.getValidatorsSignatures(),accounts);
        ret.originalSignature = Signature.publicKeyToName(this.getOriginalSignature(),accounts);

        return ret;
    }

    public SignedValidatorMessage nameToPublicKey(Set<DistributedLedgerAccount> accounts)
    {
        SignedValidatorMessage ret = new SignedValidatorMessage();

        ret.contract = this.getContract();
        ret.contract.setPaymentSet(Payment.populatePublicKeys(this.contract.getPaymentSet(),accounts));
        ret.validatorsSignatures = Signature.nameToPublicKey(this.getValidatorsSignatures(),accounts);
        ret.originalSignature = Signature.nameToPublicKey(this.getOriginalSignature(),accounts);

        return ret;
    }
    @JsonIgnore
    public String uniqueString()
    {
        return  this.originalSignature.uniqueString() +
                this.validatorsSignatures.stream().map(Signature::uniqueString).sorted().collect(Collectors.joining("|")) +
                this.contract.getStringToSign()
                ;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignedValidatorMessage d = (SignedValidatorMessage) o;
        return this.uniqueString().equals(d.uniqueString());
    }
    @JsonIgnore
    @Override
    public int hashCode() {
        return this.uniqueString().hashCode();
    }

    //getters and setters
    public ValidatorMessageDataContract getContract() {return contract;}
    public Set<Signature> getValidatorsSignatures() {return validatorsSignatures;}
    public Signature getOriginalSignature(){return this.originalSignature;}
}
