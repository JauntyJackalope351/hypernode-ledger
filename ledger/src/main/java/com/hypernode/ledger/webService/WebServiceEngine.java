package com.hypernode.ledger.webService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hypernode.ledger.ErrorHandling;
import com.hypernode.ledger.client.ExternalPayment;
import com.hypernode.ledger.contracts.*;
import com.hypernode.ledger.encryptionInterfaces.Encryption;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_BaseInterface;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.time.Instant.now;


//@Slf4j
@Service
public class WebServiceEngine {
    StatusDataContract statusDataContract;
    TransportMessageDataContract transportMessageDataContract;
    TransportMessageDataContract currentlyTransmittedTransportMessage;
    ValidatorMessageDataContract pendingNextMessage;
    ValidatorNode thisValidatorNode;
    List<ValidatorNode> peers;
    EncryptionEntity_BaseInterface encryptionEntity;
    LedgerHistory ledgerHistory;
    BlockRevisionResult blockRevisionResult;

    public void TimedProcessingEvent() {
        // changed from POST to GET
        //
        // the comment below explains why we moved from a POST design (other nodes send you their data)
        // to GET (everyone serves their data as a webpage and you go and pick it up on your own terms).
        // It also becomes a lot less problematic to deal with thread safety if you work with GETs
        //
        // get a generous timer going, and call those in the peerlist.
        // you will build a map <validatornode, list<transportmessagedatacontract>>
        // after the propagation you will have it populated with all of the values(ideally).
        // (if someone did not write you can filter the map to find those who have listsize of 0
        // and remove them from the validatornode list at the end of this process)
        // you will then need another round to sign the validated final block revision
        // this will go around and you'll have a map <transportmessagedatacontract,list<signatures>>
        // and when the number of passages reaches the agreed amount if there is someone with more than 50%
        // of the validators the list<signatures> will become the new validatornodes,
        // and the agreed message will be used as the base for the next block
        // .
        // or simply call receiveData on the peerList...
        this.updateCurrentMessage();

    }

    public void TimedReadingEvent()
    {
        for (ValidatorNode peer : this.peers) {
            try {
                String connectionString = peer.getConnectionString();
                TransportMessageDataContract peerMessage = WebServiceCaller.callServerMethod(
                        connectionString,
                        "getCurrentlyTransmittedTransportMessage",
                        null,
                        new TypeReference<>() {}
                );
                this.receiveData(peerMessage);
            }
            catch (IOException | InterruptedException e)
            {
                ErrorHandling.logEvent("error",false,e);
                // log or handle errors
            }
        }
    }

    /**
     * ReceiveData is an exposed web service method that allows other servers (your peers)
     * to contact you and share their side of the data contract.
     * Once the data is processed (and if there is no more waiting) then it is possible
     * to send to your peers the updated data contract with sendData()
     *
     * @param _dataContract the message from the other validator node
     */

    public void receiveData(TransportMessageDataContract _dataContract) {
        ValidatorNode validatorNode;
        try
        {
            _dataContract.nameToPublicKey(this.statusDataContract.getDistributedLedgerAccounts());
            //if you want to be strict and only receive from your peers
            validatorNode = ValidatorNode.findByPublicKey(this.peers, _dataContract.getSignature().getPublicKey());
            //TODO use this instead if you want to manually enter the messages
            // validatorNode = ValidatorNode.findByPublicKey(this.statusDataContract.getValidatorNodeList(), _dataContract.getSignature().getPublicKey());
        }
        catch (Exception e)
        {// TODO that server is not within my peer list, you might want to remove this check if you manually enter elements
            ErrorHandling.logEvent("error",false,e);
            return;
        }

        if (validatorNode.getPublicKey().equals( _dataContract.getSignature().getPublicKey())
                && Encryption.verifySignedMessage(_dataContract.getStringToSign(), _dataContract.getSignature())
        )
        {
            //someone gave you data, you have to process it and see if it is still good
            this.transportMessageDataContract.storeDataContract(_dataContract, this.getEncryptionEntity());
        }
        //else ... ignore the message
    }

    public boolean updateCurrentMessage() {
        //implements point 3b
        TransportMessageDataContract messageToSend;
        boolean newRevision;
        //check if you need to send a new version of this revision or if it is complete and ready to be processed
        newRevision = this.transportMessageDataContract.getBlockTempVersion() >= this.statusDataContract.getLedgerParameters().getMessageTransmissionsPerRevision();
        if (!newRevision)
        {
            messageToSend = this.newDataContractTempVersion();
        }
        else
        {
            messageToSend = this.processDataContractRevision();
        }
        this.currentlyTransmittedTransportMessage = messageToSend.hardCopy();
        this.currentlyTransmittedTransportMessage.publicKeyToName(this.statusDataContract.getDistributedLedgerAccounts());
        return true;
    }

    /**
     * Update the dataContract that you are going to share
     * keeping in mind that this is just an updated temp version and it is not a complete revision
     * @return TransportMessageDataContract, updated and signed contract
     */
    private TransportMessageDataContract newDataContractTempVersion() {
        this.transportMessageDataContract.setBlockTempVersion(this.transportMessageDataContract.getBlockTempVersion() +1);
        this.transportMessageDataContract.setSent( now());
        this.transportMessageDataContract.signContract(this.getEncryptionEntity());

        return this.transportMessageDataContract;
    }

    private TransportMessageDataContract processDataContractRevision()
    {
        this.blockRevisionResult = BlockRevisionResult.processRevision(transportMessageDataContract, statusDataContract);
        // checking if block revision result has a majority of the votes with the weights
        if (this.blockRevisionResult.isValid())
        {

             return this.newBlockId();
        }
        //this.transportMessageDataContract.setPreviousRevision(this.transportMessageDataContract);
        return this.newDataContractRevision();
    }

    /**
     * Construct the new message you will pass along,
     * by gathering the data that sits in the queue
     * @return TransportMessageDataContract the new data contract message
     */
    TransportMessageDataContract newDataContractRevision() {

        TransportMessageDataContract newMessage =  TransportMessageDataContract.create(
                this.transportMessageDataContract.getBlockRevision() +1,
                this.statusDataContract,
                SignedValidatorMessage.findFirstByPublicKey(
                        this.transportMessageDataContract.getSignedValidatorMessages(),
                        this.encryptionEntity.getPublicKey())
                        .getContract(),
                this.getEncryptionEntity());

        Set<Signature> previousBlockRevisionResultSignatures = new HashSet<>();

        previousBlockRevisionResultSignatures.add(Signature.create(this.encryptionEntity,this.blockRevisionResult.getStringToSign(),true));
        newMessage.setSignedValidatorMessages(this.transportMessageDataContract.getSignedValidatorMessages());
        this.transportMessageDataContract = newMessage;
        this.transportMessageDataContract.setPreviousBlockRevisionResultSignatures(previousBlockRevisionResultSignatures);

        return newMessage;
    }

    /**
     * processDataContractBlock is called once enough contract versions have been distributed
     * and it is possible to reconstruct the unique message and determine the new statusDataContract
     *
     * This new status will then be distributed to the servers who are waiting to be let into the network
     */
    private TransportMessageDataContract newBlockId() {
        ledgerHistory.addNewBlockRevisionResult(this.blockRevisionResult);

        this.statusDataContract = this.blockRevisionResult.getResultingStatusDataContract();
        this.statusDataContract.signContract(this.getEncryptionEntity());


        //let the other servers waiting to be authenticated back in
        SignedValidatorMessage thisLastMessage = SignedValidatorMessage.findFirstByPublicKey(this.transportMessageDataContract.getSignedValidatorMessages(), thisValidatorNode.getPublicKey());
        Set<ValidatorNode> validatorNodes = thisLastMessage.getContract().getValidatorNodes();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            List<Callable<Boolean>> tasks = validatorNodes.stream()
                    .map(node -> (Callable<Boolean>) () -> WebServiceCaller.callServerMethodThrows(
                            node.getConnectionString(), "initialize", statusDataContract, new TypeReference<Boolean>() {
                            }))
                    .toList();
            executor.invokeAll(tasks); // wait for completion, handle exceptions if needed
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        this.transportMessageDataContract = TransportMessageDataContract.create(1,this.statusDataContract, pendingNextMessage, this.getEncryptionEntity());
        this.thisValidatorNode = ValidatorNode.findByPublicKey(this.statusDataContract.getValidatorNodeList(), thisValidatorNode.getPublicKey());
        this.statusDataContract.getLedgerParameters().setGroupParameters(this.statusDataContract.getValidatorNodeList().size());
        this.peers = this.statusDataContract.getLedgerParameters().calculatePeers(this.statusDataContract.getValidatorNodeList(), this.thisValidatorNode.getAddress());
        this.pendingNextMessage.setPaymentSet(new HashSet<>());
                //this.pendingNextMessage.getPaymentSet().stream().filter(p -> p.getBlockId() > this.statusDataContract.getId() +1)
                //.map(p -> p.validatorSignPayment(this.getEncryptionEntity(),this.statusDataContract.getHashPreviousBlock())).collect(Collectors.toSet()));
        LedgerParameters ledgerParameters = this.pendingNextMessage.getVotedParameterChanges();
        if(ledgerParameters.getStringToSign().equals(this.blockRevisionResult.getResultingLedgerParameters().getStringToSign()))
        {
            ledgerParameters.setDistributedLedgerAccountReassignProposals(new HashSet<>());
        }
        this.pendingNextMessage = ValidatorMessageDataContract.createEmpty(this.pendingNextMessage.getId()+1);
        this.pendingNextMessage.setVotedParameterChanges(ledgerParameters);



        return this.transportMessageDataContract;
    }
    //exposed web service methods

    public String requestAuthenticationStringToSign() {
        //the caller asks for a message to sign and then gives it back signed.
        //He will need to use a public key of an account he controls,
        // which will then be used for staking
        return Encryption.hash(this.statusDataContract.getStringToSign());
        //this will be updated at the meshExchangeData when it's time to validate the block revision and switch to the new one
    }

    public boolean authenticateServer(String _publicKey, String _signedMessage, String _connectionString, String _signedConnectionString) {
        if (
                !Encryption.verifySignedMessage(this.requestAuthenticationStringToSign(), _publicKey, _signedMessage)
                || !Encryption.verifySignedMessage(_connectionString, _publicKey, _signedConnectionString)
        ) {
            //this authentication failed
            return false;
        }

        if (this.pendingNextMessage.getValidatorNodes().stream().anyMatch(v -> v.getPublicKey().equals(_publicKey))) {
            //this public key has already been validated
            return false;
        }

        if (this.statusDataContract.getAccountsList().stream().noneMatch(c -> c.getPublicKey().equals(_publicKey)
                && c.getAmount().compareTo(this.statusDataContract.getLedgerParameters().getAmountRequestedToBeValidator()) > 0)) {
            //not enough credit to become a validator
            return false;
        }

        //valid authentication, put this into the queue
        //it will be sent out to be discussed in the next block.
        //Once the discussion is done they will be pinged by the connected hypernodes
        //and from that they will be able to function

        ValidatorNode validatorNode = new ValidatorNode();
        validatorNode.setAddress( new HashMap<Integer, Integer>());
        validatorNode.setPublicKey(_publicKey);
        validatorNode.setConnectionString( _connectionString);
        validatorNode.setSignature(_signedConnectionString);
        Set<ValidatorNode> newAuthenticating = this.pendingNextMessage.getValidatorNodes();
        newAuthenticating.add(validatorNode);
        this.pendingNextMessage.setValidatorNodes(newAuthenticating);
        return true;
    }

    public List<ExternalPayment> notifySpend(List<ExternalPayment> _requestedPayments) {

        List<ExternalPayment> validPayments = new ArrayList<>(_requestedPayments.stream().filter(
                payment -> payment.verifyClientSignature()
                        && payment.getAmount().compareTo(this.statusDataContract.getNextLedgerParameters().getTransactionCost()) >= 0
                        && (this.statusDataContract.getNextLedgerParameters().getMaxMessageLength() <=0
                             ||   this.statusDataContract.getNextLedgerParameters().getMaxMessageLength() > payment.getComment().length())
                        && this.statusDataContract.getAccountsList().stream().anyMatch(c -> c.getValidatorNode().equals(this.getEncryptionEntity().getPublicKey()) && c.getPublicKey().equals(payment.getFrom()))
                        && payment.getBlockId() == this.statusDataContract.getId() + 2
                        && DistributedLedgerAccount.exists(this.statusDataContract.getAccountsList(), payment.getFrom())).toList());

        if(this.pendingNextMessage.getPaymentSet().size() + validPayments.size() > this.statusDataContract.getNextLedgerParameters().getMaxTransactionsPerBlock())
        {
            ErrorHandling.logEvent("too many payments, limit reached for this frame",false,null);
            return new ArrayList<ExternalPayment>(0);//TODO return string saying list is full?
        }

        this.pendingNextMessage.getPaymentSet().addAll(validPayments.stream().map(Payment::createFromExternalPayment).toList());

        return validPayments;
    }

    public String notifyupdateAccountAttributes(AccountAttributesUpdate _accountAttributesUpdate)
    {
        if(_accountAttributesUpdate.validate(this.statusDataContract.getId()+1, Encryption.hash(this.statusDataContract.getStringToSign())))
        {
            this.pendingNextMessage.getVotingDelegationSet().add(_accountAttributesUpdate);
            return "OK";
        }
        return "Invalid update";
    }

    public BigDecimal getAmountAvailable(String publicKey)
    {
        try
        {
            return this.statusDataContract.getAccountsList().stream().filter(a -> a.getPublicKey().equals(publicKey)).findFirst().get().getAmount();
        }
        catch (Exception e)
        {
            return BigDecimal.ZERO;

        }
    }

    public EncryptionEntity_BaseInterface getEncryptionEntity(){ return encryptionEntity;}
    public void setEncryptionEntity(EncryptionEntity_BaseInterface credentials){ encryptionEntity = credentials;}
    public StatusDataContract getStatusDataContract() {return statusDataContract;}
    public TransportMessageDataContract getCurrentlyTransmittedTransportMessage() {return currentlyTransmittedTransportMessage;}
    public ValidatorMessageDataContract getPendingNextMessage() {return pendingNextMessage;}
    public void setThisValidatorNode(ValidatorNode thisValidatorNode) {this.thisValidatorNode = thisValidatorNode;}
    public LedgerHistory getLedgerHistory() {return ledgerHistory;}
}