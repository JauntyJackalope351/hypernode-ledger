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
import java.time.Instant;
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
    TransportMessageDataContract nextTransmittedTransportMessage;
    ValidatorMessageDataContract pendingNextMessage;
    ValidatorNode thisValidatorNode;
    List<ValidatorNode> peers;
    EncryptionEntity_BaseInterface encryptionEntity;
    LedgerHistory ledgerHistory;
    BlockRevisionResult blockRevisionResult;

    public void TimedUpdatingEvent() {
        ErrorHandling.logEvent("TimedProcessingEvent",false,null);
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
        //TODO use executors to read in parallel?
        // no, i would have to rewrite everything to be thread safe.
        List<ValidatorNode> retryPeers = new ArrayList<>();
        for (ValidatorNode peer : this.peers)
        {
            try
            {
                String connectionString = peer.getConnectionString();
                TransportMessageDataContract peerMessage = WebServiceCaller.callServerMethodThrows(
                        connectionString,
                        "hdls/getCurrentlyTransmittedTransportMessage",
                        null,
                        new TypeReference<>() {
                        }
                );
                this.receiveData(peerMessage);
            }
            catch (Exception e)
            {
                ErrorHandling.logEvent("failed reading data from" + peer.getConnectionString(), false, e);
                // log or handle errors
                // have a retry
                retryPeers.add(peer);
            }
        }
        try
        {
            wait(5000); //wait a second before retrying
        }
        catch (Exception e)
        {

        }
        for (ValidatorNode peer : retryPeers) {
            try
            {
                String connectionString = peer.getConnectionString();
                TransportMessageDataContract peerMessage = WebServiceCaller.callServerMethodThrows(
                        connectionString,
                        "hdls/getCurrentlyTransmittedTransportMessage",
                        null,
                        new TypeReference<>() {
                        }
                );
                this.receiveData(peerMessage);
            }
            catch (Exception e)
            {
                ErrorHandling.logEvent("failed re-reading data from" + peer.getConnectionString(), false, e);
            }
        }
        this.prepareNextMessage();
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
            //TODO now the system is strict and only receives from your peers
            // to avoid ddos or flooding.
            // if you want to manually enter the messages use this line instead
            // validatorNode = ValidatorNode.findByPublicKey(this.statusDataContract.getValidatorNodeList(), _dataContract.getSignature().getPublicKey());
            validatorNode = ValidatorNode.findByPublicKey(this.peers, _dataContract.getSignature().getPublicKey());

            if ( _dataContract.getSignature().getPublicKey().equals(validatorNode.getPublicKey())
                    && Encryption.verifySignedMessage(_dataContract.getStringToSign(), _dataContract.getSignature())
            )
            {
                //someone gave you data, you have to process it and see if it is still good
                this.transportMessageDataContract.storeDataContract(_dataContract, this.getEncryptionEntity());
            }
            else
            {
                ErrorHandling.logEvent("error before receiveData for " + _dataContract.getSignature().getPublicKey(),false,null);
            }
            //else ... ignore the message

        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("error during receiveData for " + _dataContract.getSignature().getPublicKey(),false,e);
            return;
        }


    }

    public boolean updateCurrentMessage()
    {
        this.currentlyTransmittedTransportMessage = this.nextTransmittedTransportMessage.hardCopy();
        this.currentlyTransmittedTransportMessage.setSent(Instant.now());
        ErrorHandling.logEvent("updateCurrentMessage block " + this.currentlyTransmittedTransportMessage.getBlockId()
                + "rev " + this.currentlyTransmittedTransportMessage.getBlockRevision()
                + "version" + this.currentlyTransmittedTransportMessage.getBlockTempVersion()
                ,false,null
        );

        return true;

    }

    public void prepareNextMessage()
    {
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
        this.nextTransmittedTransportMessage = messageToSend.hardCopy();
        this.nextTransmittedTransportMessage.signContract(this.encryptionEntity);
        this.nextTransmittedTransportMessage.publicKeyToName(this.statusDataContract.getDistributedLedgerAccounts());
        ErrorHandling.logEvent("updated nextTransmittedTransportMessage id " + nextTransmittedTransportMessage.getBlockId()
                +" rev " + nextTransmittedTransportMessage.getBlockRevision()
                + "ver " + nextTransmittedTransportMessage.getBlockTempVersion()
                ,false,null);
    }

    /**
     * Update the dataContract that you are going to share
     * keeping in mind that this is just an updated temp version and it is not a complete revision
     * @return TransportMessageDataContract, updated and signed contract
     */
    private TransportMessageDataContract newDataContractTempVersion() {
        this.transportMessageDataContract.setBlockTempVersion(this.transportMessageDataContract.getBlockTempVersion() +1);
        this.transportMessageDataContract.setSent( now());

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
                            node.getConnectionString(), "hdls/initialize", statusDataContract, new TypeReference<Boolean>() {
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

        if(!ValidatorNode.validateConnectionString(_connectionString))
        {
            return false;
        }

        //test the connection string to verify that it is the correct server?
        String publicKeyConnectionString = WebServiceCaller.callServerMethodThrows(
                _connectionString,
                "hdls/getPublicKey",
                null,
                new TypeReference<String>() {}
        );
        if(!_publicKey.equals(publicKeyConnectionString))
        {
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

    public AccountInfo getAccountInfo(String id)
    {
        AccountInfo ret = new AccountInfo();
        DistributedLedgerAccount account;
        ValidatorNode node;
        if(id.length()<200)
        {
            account = this.statusDataContract.getAccountsList().stream().filter(d -> d.getName().equals(id)).findFirst().orElse(new DistributedLedgerAccount());
        }
        else
        {//TODO postman has a bug where the id gets truncated when it has a "+" in it
            account = this.statusDataContract.getAccountsList().stream().filter(d -> d.getPublicKey().equals(id)).findFirst().orElse(new DistributedLedgerAccount());
        }
        if(account.getPublicKey().isEmpty())
        {
            return ret;
        }
        ret.setAccount(account);
        node = this.statusDataContract.getValidatorNodeList().stream().filter(v -> v.getPublicKey().equals(account.getPublicKey())).findFirst().orElse(new ValidatorNode());
        ret.setNode(node);
        return ret;
    }

    public int getBlockId()
    {
        return this.nextTransmittedTransportMessage.getBlockId();
    }

    public EncryptionEntity_BaseInterface getEncryptionEntity(){ return encryptionEntity;}
    public void setEncryptionEntity(EncryptionEntity_BaseInterface credentials){ encryptionEntity = credentials;}
    public StatusDataContract getStatusDataContract() {return statusDataContract;}
    public TransportMessageDataContract getCurrentlyTransmittedTransportMessage() {return currentlyTransmittedTransportMessage;}
    public ValidatorMessageDataContract getPendingNextMessage() {return pendingNextMessage;}
    public void setThisValidatorNode(ValidatorNode thisValidatorNode) {this.thisValidatorNode = thisValidatorNode;}
    public LedgerHistory getLedgerHistory() {return ledgerHistory;}
}