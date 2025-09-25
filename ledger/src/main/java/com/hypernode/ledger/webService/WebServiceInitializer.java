package com.hypernode.ledger.webService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hypernode.ledger.ErrorHandling;
import com.hypernode.ledger.contracts.*;
import com.hypernode.ledger.controller.WebServiceEndpoints;

import java.util.HashSet;

public class WebServiceInitializer {

    /**
     * Method used to start a new ledger from scratch
     *
     * @param _startingData the data at the beginning of the ledger, allowing others to start joining
     */
    public static void initAndCreateNewLedger(WebServiceEngine _this, StatusDataContract _startingData)
    {
        _this.transportMessageDataContract = new TransportMessageDataContract();
        _this.transportMessageDataContract.setSignedValidatorMessages(new HashSet<>());
        initialize(_this, _startingData);
        _this.newDataContractRevision();
        //start everything up
        while(_this.currentlyTransmittedTransportMessage.getBlockId()< 3)
        {
            _this.prepareNextMessage();
            _this.updateCurrentMessage();
        }

    }

    /**
     * Connect to an existing ledger
     *
     * @param _connectionStringServer the server you want to connect to
     * @param _thisConnectionString   your own connection string
     */
    public static String joinExistingLedger(WebServiceEngine _this, String _connectionStringServer, String _thisConnectionString) {

        String authenticationString;

        if(_this.getEncryptionEntity() == null)
        {
            return "EncryptionEntity not configured";
        }

        authenticationString = WebServiceCaller.callServerMethodThrows(_connectionStringServer, "hdls/requestAuthenticationStringToSign",
                null, new TypeReference<>() {
                });
        WebServiceEndpoints.AuthenticateServerRequest request = new WebServiceEndpoints.AuthenticateServerRequest(_this.getEncryptionEntity().getPublicKey(), _this.getEncryptionEntity().signMessage(authenticationString), _thisConnectionString, _this.getEncryptionEntity().signMessage(_thisConnectionString));
        if(WebServiceCaller.callServerMethodThrows(_connectionStringServer, "hdls/authenticateServer", request, new TypeReference<Boolean>() {
        }))
        {
            return "OK";
        }
        return "Not Authenticated";
    }

    /**
     * This method is called after the authentication, and allows this new node to receive the processed data.
     * It is the async response to authenticateServer().
     *
     * @param _lastDataContract the last validated data contract
     */
    public static void initialize(WebServiceEngine _this, StatusDataContract _lastDataContract) {
        _this.statusDataContract = _lastDataContract;
        _this.statusDataContract.setNextLedgerParameters(_this.statusDataContract.getLedgerParameters());
        _this.ledgerHistory = LedgerHistory.create(_lastDataContract);
        ValidatorMessageDataContract message = ValidatorMessageDataContract.createEmpty(_lastDataContract.getId()+1);
        message.setValidatorNodes( new HashSet<>());
        message.setPaymentSet( new HashSet<>());
        message.setVotedParameterChanges(_lastDataContract.getLedgerParameters());
        _this.thisValidatorNode = ValidatorNode.findByPublicKey(_this.statusDataContract.getValidatorNodeList(), _this.getEncryptionEntity().getPublicKey());
        _this.statusDataContract.getLedgerParameters().setGroupParameters(_this.statusDataContract.getValidatorNodeList().size());
        _this.peers = _this.statusDataContract.getLedgerParameters().calculatePeers(_this.statusDataContract.getValidatorNodeList(), _this.thisValidatorNode.getAddress());
        _this.transportMessageDataContract = TransportMessageDataContract.create(1,_lastDataContract,message, _this.getEncryptionEntity());
        //_this.transportMessageDataContract.setPeerContracts(new HashSet<>());

        _this.nextTransmittedTransportMessage = _this.transportMessageDataContract.hardCopy();
        _this.nextTransmittedTransportMessage.publicKeyToName(_this.statusDataContract.getDistributedLedgerAccounts());
        _this.currentlyTransmittedTransportMessage = _this.nextTransmittedTransportMessage;
        _this.pendingNextMessage = ValidatorMessageDataContract.createEmpty(message.getId()+1);
        _this.pendingNextMessage.setPaymentSet( new HashSet<>());
        _this.pendingNextMessage.setValidatorNodes(new HashSet<>());
        _this.pendingNextMessage.setVotedParameterChanges(_lastDataContract.getLedgerParameters());
        _this.blockRevisionResult = BlockRevisionResult.processRevision(_this.transportMessageDataContract,_this.statusDataContract);
        //you are now authenticated, at the next block revision the Validator list will be updated
        //and at the next one you will be able to exchange messages
        //with the other servers.
        //the first block revision you will listen and populate your startingLedger
        //and from the subsequent you will be able to send your own transactions
        ErrorHandling.logEvent("initialized",false,null);
    }
}
