package com.hypernode.ledger.contracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypernode.ledger.ErrorHandling;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class BlockRevisionResult
{
    TransportMessageDataContract transportMessageDataContractReceived;
    TransportMessageDataContract previousTransportMessageDataContractReceived;
    StatusDataContract statusDataContractStarting;
    StatusDataContract newStatusDataContract;
    Set<String> bannedValidators;
    boolean isValid;

    Set<String> validValidatorNodes;
    Map<String, BigDecimal> delegatedAmounts;
    @JsonIgnore
    public static BlockRevisionResult processRevision(TransportMessageDataContract previousRevisionTransportMessageDataContract, TransportMessageDataContract transportMessageDataContract, StatusDataContract statusDataContract)
    {
        BlockRevisionResult blockRevisionResult = new BlockRevisionResult();
        blockRevisionResult.init(previousRevisionTransportMessageDataContract, transportMessageDataContract,statusDataContract);
        return blockRevisionResult;
    }

    public void init(TransportMessageDataContract previousRevisionTransportMessageDataContract, TransportMessageDataContract transportMessageDataContract, StatusDataContract statusDataContract) {

        this.previousTransportMessageDataContractReceived = previousRevisionTransportMessageDataContract;
        this.transportMessageDataContractReceived = transportMessageDataContract;
        this.statusDataContractStarting = statusDataContract;
        this.validValidatorNodes = new HashSet<>();
        this.isValid = false;

        // add to the bannedvalidators those who have multiple message for same block
        this.bannedValidators = BlockRevisionResult.getBannedValidators(transportMessageDataContract.getSignedValidatorMessages(),statusDataContract);
        if(transportMessageDataContract.getBlockRevision() < statusDataContract.getLedgerParameters().getMessageTransmissionsPerBlock()
        || transportMessageDataContract.getBlockRevision() % 2 == 0)
        {
            return;
        }

        this.bannedValidators.addAll(this.multisignedPreviousRevisionResultsSignatures());

        // checking if validatormesssagedatacontract has a majority of the votes
        // with the weights
        // so form a map <signature, sumvalue> revisionSignatureValues
        // and then a map <publickey,signature> if that is above 50%+1 values
        BigDecimal totalMoney = statusDataContract.getAccountsList().stream().map(DistributedLedgerAccount::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.delegatedAmounts = DistributedLedgerAccount.calculateAmountDelegated(statusDataContract.getAccountsList());
        Map<String, BigDecimal> revisionSignatureValues =
                transportMessageDataContract.getPreviousBlockRevisionResultSignatures()
                        .stream()
                        .filter(s -> !this.getBannedValidators().contains(s.getPublicKey()))
                        .collect(Collectors.toMap(Signature::getMessageValue, s -> this.delegatedAmounts.get(s.getPublicKey()), BigDecimal::add));

        Optional<String> keyWithMaxValue = revisionSignatureValues.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        if(keyWithMaxValue.isPresent())
        {
            if(keyWithMaxValue.get().length()>1)
            {
                this.isValid = totalMoney.divide(BigDecimal.TWO).compareTo(revisionSignatureValues.get(keyWithMaxValue.get())) < 0;

                this.validValidatorNodes = new HashSet<>();
                this.validValidatorNodes.addAll(
                        transportMessageDataContract.getPreviousBlockRevisionResultSignatures().stream()
                                .filter(s -> s.getMessageValue().equals(keyWithMaxValue.get()) && !this.bannedValidators.contains(s.getPublicKey()))
                                .map(Signature::getPublicKey).toList());

            }
        }
    }

    public static Set<String> getBannedValidators(Set<SignedValidatorMessage> _messages,StatusDataContract status)
    {
        Set<String> ret = new HashSet<>();
        //filter by making sure we're only getting the messages from this block id and with valid signatures
        Set<SignedValidatorMessage> messages = _messages.stream().filter(m -> m.getContract().getId() == status.getId()+1 && m.validateSignature()).collect(Collectors.toSet());
        //group into a map, where the key is the publickey and the value is the list of values for this
        Map<String, List<SignedValidatorMessage>> collect = messages.stream().collect(Collectors.groupingBy(m -> m.getOriginalSignature().getPublicKey()));
        //if someone sent two different messages with the same id they should be banned
        ret.addAll(collect.entrySet().stream().filter(e -> e.getValue().size()>1).map(e-> e.getKey()).toList());
        //also add to the ban list elements that send a clearly invalid message
        ret.addAll(messages.stream().filter(m-> !m.getContract().isValid(m.getOriginalSignature().getPublicKey(),status)).map(m-> m.getOriginalSignature().getPublicKey()).toList());
        return ret;
    }

    public boolean isPreviousRevisionSignature(Signature s)
    {
        String prefix = "ID" + this.previousTransportMessageDataContractReceived.getBlockId() +
                "R" + this.previousTransportMessageDataContractReceived.getBlockRevision() +
                ":" + this.statusDataContractStarting.getHash();
        return s.validate() && s.getMessageValue().startsWith(prefix);
    }

    public boolean isCurrentRevisionSignature(Signature s)
    {
        String prefix = "ID" + this.transportMessageDataContractReceived.getBlockId() +
                "R" + this.transportMessageDataContractReceived.getBlockRevision() +
                ":" + this.statusDataContractStarting.getHash();
        return s.validate() && s.getMessageValue().startsWith(prefix);
    }

        @JsonIgnore
    public Set<String> multisignedPreviousRevisionResultsSignatures()
    {

        Set<Signature> signatures =this.transportMessageDataContractReceived
                .getPreviousBlockRevisionResultSignatures().stream().filter(this::isPreviousRevisionSignature).collect(Collectors.toSet());
        Map<String, List<Signature>> collect = signatures.stream().collect(Collectors.groupingBy(Signature::getPublicKey));
        return collect.entrySet().stream().filter(e -> e.getValue().size()>1).map(Map.Entry::getKey).sorted().collect(Collectors.toSet());

    }
    @JsonIgnore
    public String getStringToSign()
    {
        //cannot include the hash of the transportmessagedatacontract because
        //to make it consistent I need to filter out the banned validators here
        //while the transportmessage needs them on to validate the transport of the message
        return "ID"+ this.transportMessageDataContractReceived.getBlockId() +
                "R" + this.transportMessageDataContractReceived.getBlockRevision() +
                ":" + this.statusDataContractStarting.getHash() +
                ":" + this.bannedValidators.stream().sorted().collect(Collectors.joining("|")) +
                ":" + this.getResultingMessage(transportMessageDataContractReceived).getStringToSign();
    }
    @JsonIgnore
    public LedgerParameters getResultingLedgerParameters()
    {
        Set<SignedValidatorMessage> messages = this.previousTransportMessageDataContractReceived.getSignedValidatorMessages().stream()
                .filter(s -> this.validValidatorNodes.contains(s.getOriginalSignature().getPublicKey())).collect(Collectors.toSet());
        BigDecimal minVal = delegatedAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.TWO);
        //messages.stream().filter(s-> !this.validValidatorNodes.contains(s.getOriginalSignature().getPublicKey())).toList().forEach(messages::remove);

        Map<String, BigDecimal> votedParameterChanges = messages.stream().collect(Collectors.toMap(
                signedValidatorMessage -> signedValidatorMessage.getContract().getVotedParameterChanges().getStringToSign(),
                signedValidatorMessage -> delegatedAmounts.get(signedValidatorMessage.getOriginalSignature().getPublicKey()),
                BigDecimal::add));

        Optional<String> keyWithMaxValue = votedParameterChanges.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        try
        {
            if (keyWithMaxValue.isPresent()) {
                if(votedParameterChanges.get(keyWithMaxValue.get()).compareTo(minVal) >0)
                {
                    return messages.stream().filter(
                            signedValidatorMessage -> signedValidatorMessage.getContract().getVotedParameterChanges().getStringToSign().equals(keyWithMaxValue.get())
                    ).findFirst().get().getContract().getVotedParameterChanges();
                }
            }
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("error BlockRevisionResults.getResultingLedgerParameters",false,e);
            //something has gone wrong
        }
        return this.statusDataContractStarting.getLedgerParameters();
    }
    @JsonIgnore
    //regroup data at the end of the message exchange
    public ValidatorMessageDataContract getResultingMessage(TransportMessageDataContract _message)
    {
        ValidatorMessageDataContract returnMessage = new ValidatorMessageDataContract();
        Iterator<SignedValidatorMessage> signedValidatorMessagesIterator;
        SignedValidatorMessage signedValidatorMessage;
        signedValidatorMessagesIterator = _message.getSignedValidatorMessages().iterator();

        while(signedValidatorMessagesIterator.hasNext())
        {
            signedValidatorMessage = signedValidatorMessagesIterator.next();
            String publicKeyToFind = signedValidatorMessage.getOriginalSignature().getPublicKey();
            if(this.validValidatorNodes.stream().anyMatch(validator -> validator.equals(publicKeyToFind)))
            {
                //merge the current message with the returnMessage
                returnMessage.mergeMessage(signedValidatorMessage.getContract());
            }
        }
        return returnMessage;
    }
    @JsonIgnore
    public StatusDataContract getResultingStatusDataContract()
    {
        newStatusDataContract = this.statusDataContractStarting;
        newStatusDataContract.setHashPreviousBlock(this.statusDataContractStarting.getHash());


        ValidatorMessageDataContract validatorMessageDataContract = this.getResultingMessage(this.previousTransportMessageDataContractReceived);
        Set<DistributedLedgerAccount> newDistributedLedgerAccountList = this.statusDataContractStarting.getAccountsList();
        this.statusDataContractStarting.getAccountsList().stream().filter(c -> this.getBannedValidators().contains(c.getPublicKey())).toList().forEach(newDistributedLedgerAccountList::remove);
        newDistributedLedgerAccountList =
                Payment.processPayments(newDistributedLedgerAccountList,
                        validatorMessageDataContract.getPaymentSet(),
                        this.statusDataContractStarting.getLedgerParameters().getTransactionCost());

        //update the new votingdelegation assignments
        newDistributedLedgerAccountList = DistributedLedgerAccount.updateAccountAttributes(newDistributedLedgerAccountList,validatorMessageDataContract.getVotingDelegationSet());
        //get the ledgerParameters
        newStatusDataContract.setLedgerParameters(newStatusDataContract.getNextLedgerParameters());
        //the next block parameters must be known to gather valid transactions,
        // therefore the new resulting ledger parameters cannot be valid immediately
        // but will have one block of wait to allow the validators to be sure of the validity
        // of the transactions that they are gathering to be processed in the subsequent block
        newStatusDataContract.setNextLedgerParameters(this.getResultingLedgerParameters());

        if(!newStatusDataContract.getNextLedgerParameters().getDistributedLedgerAccountReassignProposals().isEmpty())
        {
            // get the getDistributedLedgerAccountReassignProposals out and process it
            Set<DistributedLedgerAccount> reassignedNewDistributedLedgerAccountList = newStatusDataContract.getNextLedgerParameters().getDistributedLedgerAccountReassignProposals();
            Set<String> publicKeys = reassignedNewDistributedLedgerAccountList.stream().map(s -> s.getPublicKey()).collect(Collectors.toSet());
            reassignedNewDistributedLedgerAccountList.addAll(
            newDistributedLedgerAccountList.stream()
                    .filter(l -> !publicKeys.contains(l.getPublicKey()))
                    .toList())
            ;
            newStatusDataContract.getNextLedgerParameters().setDistributedLedgerAccountReassignProposals(new HashSet<>());
            newDistributedLedgerAccountList = reassignedNewDistributedLedgerAccountList;
        }
        newStatusDataContract.setDistributedLedgerAccounts(newDistributedLedgerAccountList);
        // just as much as we add validators there should be a way to remove them
        // if they did not validate anything on the last block
        // actually only keep the ones that are identified in getValidValidatorNodes
        newStatusDataContract.setValidatorNodeList(
                ValidatorNode.merge(statusDataContractStarting.getValidatorNodeList().stream().filter(v -> this.getValidValidatorNodes().contains(v.getPublicKey())).toList(),
                        validatorMessageDataContract.getValidatorNodes(),
                        newStatusDataContract.getLedgerParameters()));
        newStatusDataContract.setId(this.statusDataContractStarting.getId()+1);
        newStatusDataContract.computeHash();

        return newStatusDataContract;
    }

    public Set<String> getBannedValidators() {return bannedValidators;}
    public boolean isValid() {return  this.isValid;}
    public Map<String, BigDecimal> getDelegatedAmounts() {return delegatedAmounts;}
    public Set<String> getValidValidatorNodes() {return validValidatorNodes;}
    public TransportMessageDataContract getTransportMessageDataContractReceived() {return transportMessageDataContractReceived;}
    public void setTransportMessageDataContractReceived(TransportMessageDataContract transportMessageDataContractReceived) {this.transportMessageDataContractReceived = transportMessageDataContractReceived;}
    public StatusDataContract getStatusDataContractStarting() {return statusDataContractStarting;}
    public void setStatusDataContractStarting(StatusDataContract statusDataContractStarting) {this.statusDataContractStarting = statusDataContractStarting;}
    public StatusDataContract getNewStatusDataContract() {return newStatusDataContract;}
    public void setNewStatusDataContract(StatusDataContract newStatusDataContract) {this.newStatusDataContract = newStatusDataContract;}
    public void setBannedValidators(Set<String> bannedValidators) {this.bannedValidators = bannedValidators;}
    public void setValid(boolean valid) {isValid = valid;}
    public void setValidValidatorNodes(Set<String> validValidatorNodes) {this.validValidatorNodes = validValidatorNodes;}
    public void setDelegatedAmounts(Map<String, BigDecimal> delegatedAmounts) {this.delegatedAmounts = delegatedAmounts;}
}
