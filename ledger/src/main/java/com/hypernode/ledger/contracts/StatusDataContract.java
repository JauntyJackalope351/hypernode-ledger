package com.hypernode.ledger.contracts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypernode.ledger.encryptionInterfaces.Encryption;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_BaseInterface;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusDataContract
{
    private int id;
    private Signature signature;
    private Set<DistributedLedgerAccount> distributedLedgerAccounts;
    //private StatusDataContract previousVersion;
    private List<ValidatorNode> validatorNodeList;
    private LedgerParameters ledgerParameters;
    private LedgerParameters nextLedgerParameters;
    private String hash;
    private String hashPreviousBlock;

    @JsonIgnore
    public String getStringToSign()
    {
        String s = this.id
                + ":" + this.distributedLedgerAccounts.stream().map(DistributedLedgerAccount::uniqueString).sorted().collect(Collectors.joining("|"))
                + ":" + this.validatorNodeList.stream().map(ValidatorNode::uniqueString).sorted().collect(Collectors.joining("|"))
                + ":" + this.ledgerParameters.getStringToSign()
                + ":" + this.nextLedgerParameters.getStringToSign()
                + ":" + this.getHashPreviousBlock();

        return s.replaceAll("\\s+", "");
    }
    public void signContract(EncryptionEntity_BaseInterface _encryptionEntity)
    {
        this.signature = Signature.create(_encryptionEntity, this.getStringToSign(),false);
    }
    public void computeHash(){this.hash = Encryption.hash(this.getStringToSign());}
    //getters and setters
    public int getId() {return id;}
    public void setId(int id) {this.id = id;}
    public Signature getSignature() {return signature;}
    public void setSignature(Signature signature) {this.signature = signature;}
    public Set<DistributedLedgerAccount> getAccountsList() {return distributedLedgerAccounts;}
    public void setDistributedLedgerAccounts(Set<DistributedLedgerAccount> distributedLedgerAccounts) {this.distributedLedgerAccounts = distributedLedgerAccounts;}
    public List<ValidatorNode> getValidatorNodeList() {return validatorNodeList;}
    public void setValidatorNodeList(List<ValidatorNode> validatorNodeList) {this.validatorNodeList = validatorNodeList;}
    public LedgerParameters getLedgerParameters() {return ledgerParameters;}
    public void setLedgerParameters(LedgerParameters _LedgerParameters) {ledgerParameters = _LedgerParameters;}
    public String getHash() {return this.hash;}
    public String getHashPreviousBlock() {return this.hashPreviousBlock;}
    public void setHashPreviousBlock(String hashPreviousBlock) {this.hashPreviousBlock = hashPreviousBlock;}
    public Set<DistributedLedgerAccount> getDistributedLedgerAccounts() {return distributedLedgerAccounts;}
    public void setHash(String hash) {this.hash = hash;}
    public LedgerParameters getNextLedgerParameters() {return nextLedgerParameters;}
    public void setNextLedgerParameters(LedgerParameters nextLedgerParameters) {this.nextLedgerParameters = nextLedgerParameters;}

    @JsonCreator
    public static StatusDataContract create(
            @JsonProperty("id") int id, // <--- Add @JsonProperty
            @JsonProperty("signature") Signature signature, // <--- Add @JsonProperty
            @JsonProperty("distributedLedgerAccounts") Set<DistributedLedgerAccount> distributedLedgerAccounts, // <--- Add @JsonProperty
            @JsonProperty("validatorNodeList") List<ValidatorNode> validatorNodeList, // <--- Add @JsonProperty
            @JsonProperty("ledgerParameters") LedgerParameters ledgerParameters, // <--- Add @JsonProperty
            @JsonProperty("nextLedgerParameters") LedgerParameters nextLedgerParameters, // <--- Add @JsonProperty
            @JsonProperty("hash") String hash, // <--- Add @JsonProperty
            @JsonProperty("hashPreviousBlock") String hashPreviousBlock // <--- Add @JsonProperty
    )
    {
        StatusDataContract s = new StatusDataContract();
        s.id = id;
        s.signature=signature;
        s.distributedLedgerAccounts = new HashSet<>();
        if(distributedLedgerAccounts != null)
        {
            s.distributedLedgerAccounts.addAll(distributedLedgerAccounts);
        }
        s.validatorNodeList = validatorNodeList;
        s.ledgerParameters = ledgerParameters;
        s.nextLedgerParameters = nextLedgerParameters;
        s.hash = hash;
        s.hashPreviousBlock = hashPreviousBlock;
        return s;
    }


}
