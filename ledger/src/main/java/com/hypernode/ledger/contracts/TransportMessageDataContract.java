package com.hypernode.ledger.contracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hypernode.ledger.ErrorHandling;
import com.hypernode.ledger.encryptionInterfaces.Encryption;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_BaseInterface;

import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransportMessageDataContract
{
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private int blockId;
    private int blockRevision;
    private int blockTempVersion;
    private String hash = "";
    private Signature signature;
    private Set<SignedValidatorMessage> signedValidatorMessages;
    private Set<Signature> previousBlockRevisionResultSignatures = new HashSet<>();
    //TransportMessageDataContract previousRevision; // debug only
    //private Set<TransportMessageDataContract> peerContracts;
    private Instant received;
    private Instant sent;


    public TransportMessageDataContract hardCopy() {
        try
        {
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.readValue(objectMapper.writeValueAsString(this), this.getClass());
        }
        catch (JsonProcessingException e)
        {
            //should never happen
            ErrorHandling.logEvent("error TransportMessageDataContract.hardCopy",true,e);
            //throw new RuntimeException(e);
            return null;
        }
    }

    //regrouop data while receiving element within a blockTempVersion
    public void storeDataContract(TransportMessageDataContract _newContract, EncryptionEntity_BaseInterface _encryptionEntity)
    {
        SignedValidatorMessage currentSignedValidatorMessage;
        Iterator<SignedValidatorMessage> iterator;

        if(     this.blockId == _newContract.getBlockId()
                && this.blockRevision == _newContract.getBlockRevision()
                && this.blockTempVersion == _newContract.getBlockTempVersion()
        )
        {
            _newContract.received = Instant.now();
            //TransportMessageDataContract.updateList(this.peerContracts, _newContract);// FOR DEBUG ONLY, TO BE REMOVED
            //merge the lists, sign the signaturechain if you need to

            iterator = _newContract.signedValidatorMessages.iterator();
            while (iterator.hasNext()) {
                currentSignedValidatorMessage = iterator.next();
                if (currentSignedValidatorMessage.validateSignature())
                {
                    SignedValidatorMessage.mergeValue(this.signedValidatorMessages, currentSignedValidatorMessage,_encryptionEntity);
                    previousBlockRevisionResultSignatures.addAll(_newContract.getPreviousBlockRevisionResultSignatures().stream()
                            .filter(s -> TransportMessageDataContract.validateBlockRevisionSignature(s, this)).toList());
                }
            }
        }
        else
        {
            throw new RuntimeException();
        }
    }

    public static boolean validateBlockRevisionSignature(Signature s, TransportMessageDataContract _this)
    {
        return s.getMessageValue().startsWith("ID"+_this.getBlockId() + "R" + (_this.getBlockRevision()-1)) && s.validate();
    }

    public static TransportMessageDataContract create(int blockRevision,
                                                      StatusDataContract status,
                                                      ValidatorMessageDataContract message,
                                                      EncryptionEntity_BaseInterface encryptionEntity
    )
    {
        TransportMessageDataContract transportMessageDataContract = new TransportMessageDataContract();
        transportMessageDataContract.blockId = status.getId() + 1;
        transportMessageDataContract.blockTempVersion = 1;
        transportMessageDataContract.blockRevision = blockRevision;
        transportMessageDataContract.signedValidatorMessages = new HashSet<>();
        //transportMessageDataContract.peerContracts = new HashSet<>();
        //transportMessageDataContract.previousRevision = new TransportMessageDataContract();
        transportMessageDataContract.signedValidatorMessages.add(SignedValidatorMessage.create(message,encryptionEntity,status));
        transportMessageDataContract.sent = Instant.now();

        transportMessageDataContract.signContract(encryptionEntity);
        transportMessageDataContract.computeHash();
        return transportMessageDataContract;
    }

    public static Set<TransportMessageDataContract> updateList(Set<TransportMessageDataContract> _list, TransportMessageDataContract _element)
    {
        TransportMessageDataContract existingDataContract;
        //cleanup and make sure it is unique or just remove
        //cleaned the list at every pass
        //existingDataContract = _list.stream().filter(DataContract -> DataContract.publicKeySender == _element.publicKeySender).findFirst().get();
        //_list.remove(existingDataContract);
        _list.add(_element);

        return _list;
    }

    public void signContract(EncryptionEntity_BaseInterface _encryptionEntity)
    {
        this.signature = Signature.create(_encryptionEntity, this.getStringToSign(),false);
        // useful in debug, return to false after testing
    }
    @JsonIgnore
    public String getStringToSign()
    {

        return "ID" + this.blockId
                + "R" + this.blockTempVersion
                + ":" + Encryption.hash(SignedValidatorMessage.SetToString(this.signedValidatorMessages))
                + ":" + Encryption.hash(Signature.listToString(this.previousBlockRevisionResultSignatures));
                //+ this.getPreviousRevision().getHash();
    }

    public void publicKeyToName(Set<DistributedLedgerAccount> accounts)
    {
        this.setPreviousBlockRevisionResultSignatures(Signature.publicKeyToName(this.getPreviousBlockRevisionResultSignatures(),accounts));
        this.setSignature(Signature.publicKeyToName(this.getSignature(),accounts));
        this.setSignedValidatorMessages(this.getSignedValidatorMessages().stream().map(s -> s.publicKeyToName(accounts)).collect(Collectors.toSet()));
    }

    public void nameToPublicKey(Set<DistributedLedgerAccount> accounts)
    {
        this.setPreviousBlockRevisionResultSignatures(Signature.nameToPublicKey(this.getPreviousBlockRevisionResultSignatures(),accounts));
        this.setSignature(Signature.nameToPublicKey(this.getSignature(),accounts));
        this.setSignedValidatorMessages(this.getSignedValidatorMessages().stream().map(s -> s.nameToPublicKey(accounts)).collect(Collectors.toSet()));
    }

//getters and setters
    public int getBlockId() { return blockId;}
    public void setBlockId(int blockId) {this.blockId = blockId;}
    public int getBlockTempVersion() {return blockTempVersion;}
    public void setBlockTempVersion(int blockTempVersion) {this.blockTempVersion = blockTempVersion;}
    public Signature getSignature() {return signature;}
    public void setSignature(Signature signature) {this.signature = signature;}
    public Set<SignedValidatorMessage> getSignedValidatorMessages() {return signedValidatorMessages;}
    public void setSignedValidatorMessages(Set<SignedValidatorMessage> signedValidatorMessages) {this.signedValidatorMessages = signedValidatorMessages;}
    //public Set<TransportMessageDataContract> getPeerContracts() {return peerContracts;}
    //public void setPeerContracts(Set<TransportMessageDataContract> peerContracts) {this.peerContracts = peerContracts;}
    public Instant getReceived() {return received;}
    public void setReceived(Instant received) {this.received = received;}
    public Instant getSent() {return sent;}
    public void setSent(Instant sent) {this.sent = sent;}
    public int getBlockRevision() {return blockRevision;}
    public void setBlockRevision(int blockRevision) {this.blockRevision = blockRevision;}
    public String getHash() {return hash;}
    public void computeHash() {this.hash = Encryption.hash(this.getStringToSign());}
    public void setHash(String _hash) {this.hash = _hash;}
    //public TransportMessageDataContract getPreviousRevision() {return previousRevision;}
    //public void setPreviousRevision(TransportMessageDataContract previousRevision) {this.previousRevision = previousRevision;}
    public Set<Signature> getPreviousBlockRevisionResultSignatures() {return previousBlockRevisionResultSignatures;}
    public void setPreviousBlockRevisionResultSignatures(Set<Signature> previousBlockRevisionResultSignatures) {this.previousBlockRevisionResultSignatures = previousBlockRevisionResultSignatures;}
}
