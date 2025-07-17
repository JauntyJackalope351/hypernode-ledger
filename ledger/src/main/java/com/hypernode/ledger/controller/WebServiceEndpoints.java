package com.hypernode.ledger.controller;

import com.hypernode.ledger.ErrorHandling;
import com.hypernode.ledger.client.ExternalPayment;
import com.hypernode.ledger.contracts.*;
import com.hypernode.ledger.contracts.DistributedLedgerAccount;
import com.hypernode.ledger.encryptionInterfaces.Encryption;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_ExternalServer;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_Integrated;
import com.hypernode.ledger.webService.WebServiceEngine;
import com.hypernode.ledger.webService.WebServiceInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * REST Controller for managing distributed ledger web service endpoints.
 * Provides HTTP endpoints for ledger operations, account management, authentication,
 * and validator node interactions in a distributed blockchain system.
 */
@Controller("/")
public class WebServiceEndpoints
{
    @Autowired
    public WebServiceEngine webServiceEngine;

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Displays the home page of the web service.
     * Sets up model attributes for the home template rendering.
     *
     * @param model Spring MVC model for passing data to the view
     * @return String view name "home" which corresponds to home.html template
     */
    @GetMapping("/")
    public String home(Model model) {
        return "home"; // Corresponds to home.html
    }

    @ResponseBody
    @GetMapping("/version")
    public String getVersion() {
        return "1.0.0.1";
    }

    /**
     * Displays the server status page showing current server information.
     * Includes the server's public key and the hash of the last processed block.
     *
     * @param model Spring MVC model for passing server status data to the view
     * @return String view name "server" which corresponds to server.html template
     */
    @GetMapping("/server")
    public String server(Model model) {
        String publicKey = "PUBLIC_KEY";
        String lastHash = "LAST_HASH";
        if(this.webServiceEngine.getEncryptionEntity() != null)
        {
            publicKey = this.webServiceEngine.getEncryptionEntity().getPublicKey();
        }
        if(this.webServiceEngine.getStatusDataContract() != null)
        {
            lastHash = this.webServiceEngine.getStatusDataContract().getHash();
        }
        model.addAttribute("publicKey", publicKey);
        model.addAttribute("lastHash", lastHash);
        return "server"; // Corresponds to home.html
    }

    /**
     * Generates a new public/private key pair for creating additional accounts.
     * The keys are returned in Base64 encoded format as JSON.
     *
     * @return String JSON response containing both privateKeyBase64 and publicKeyBase64
     */
    @GetMapping("/newKeyPair")
    public String generateKeyPair(Model model)
    {
        KeyPair keyPair = Encryption.createNewKey();
        model.addAttribute("apiEndpoint","");
        model.addAttribute("title","generate Key Pair");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Your key pair");
        model.addAttribute("jsonDefault",
                "{\n\"privateKey\":\n\"" + Encryption.ByteArrayToBase64(keyPair.getPrivate().getEncoded())
                        + "\",\n\"publicKey\":\n\"" + Encryption.ByteArrayToBase64(keyPair.getPublic().getEncoded() ) + "\"\n}");

        return "JSONInput";

    }

    /**
     * Sets the encryption entity for integrated key management.
     * Validates the provided keys by testing message signing before acceptance.
     * This method configures the server to use locally stored private keys.
     *
     * @param jsonInput EncryptionEntity_Integrated object containing public and private keys
     * @return String status message: "OK" if successful, "Already Started" if already configured,
     *         or "Invalid Keys" if the key validation fails
     */
    @ResponseBody
    @PostMapping("/setEncryptionEntityIntegrated")
    public String setEncryptionEntityIntegrated(@RequestBody EncryptionEntity_Integrated jsonInput) {
        String testMessage = "the quick brown fox jumps over the lazy dog";
        if(webServiceEngine.getEncryptionEntity() != null) {
            return "Already Started";
        }
        if(!Encryption.verifySignedMessage(testMessage,jsonInput.getPublicKey(),jsonInput.signMessage(testMessage)))
        {
            return "Invalid Keys";
        }
        webServiceEngine.setEncryptionEntity(jsonInput);
        return "OK";
    }

    /**
     * Displays the form page for setting up integrated encryption entity.
     * Provides a user interface for inputting public and private keys.
     *
     * @param model Spring MVC model for passing form configuration to the view
     * @return String view name "JSONInput" for the key input form
     */
    @GetMapping("/server/setEncryptionEntityIntegrated")
    public String clientSetEncryptionEntityIntegrated(Model model) {
        model.addAttribute("apiEndpoint","/setEncryptionEntityIntegrated");
        model.addAttribute("title","Set Encryption Entity (Integrated)");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste JSON here:");
        model.addAttribute("jsonDefault","{\n" +
                "\"publicKey\":\n" +
                "\"PUBLIC_KEY\",\n" +
                "\"privateKey\":\n" +
                "\"PRIVATE_KEY\"\n" +
                "}");
        return "JSONInput";
    }

    /**
     * Sets the encryption entity for external server key management.
     * Configures the server to use an external signing service for cryptographic operations.
     * Validates the connection by testing message signing before acceptance.
     *
     * @param jsonInput EncryptionEntity_ExternalServer object containing connection details
     * @return String status message: "OK" if successful, "Already Started" if already configured,
     *         or "Invalid Keys" if the external server validation fails
     */
    @ResponseBody
    @PostMapping("/setEncryptionEntityExternalServer")
    public String setEncryptionEntityExternalServer(@RequestBody EncryptionEntity_ExternalServer jsonInput) {
        String testMessage = "the quick brown fox jumps over the lazy dog";
        if(webServiceEngine.getEncryptionEntity() != null) {
            return "Already Started";
        }
        if(!Encryption.verifySignedMessage(testMessage,jsonInput.getPublicKey(),jsonInput.signMessage(testMessage)))
        {
            return "Invalid Keys";
        }
        webServiceEngine.setEncryptionEntity(jsonInput);
        return "OK";
    }

    /**
     * Displays the form page for setting up external server encryption entity.
     * Provides a user interface for inputting external server connection details.
     *
     * @param model Spring MVC model for passing form configuration to the view
     * @return String view name "JSONInput" for the external server connection form
     */
    @GetMapping("/server/setEncryptionEntityExternalServer")
    public String setEncryptionEntityExternalServer(Model model) {
        model.addAttribute("apiEndpoint","/setEncryptionEntityExternalServer");
        model.addAttribute("title","Set Encryption Entity (Integrated)");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste connection string here:");
        model.addAttribute("jsonDefault", "");
        return "JSONInput";
    }

    /**
     * Initializes and creates a new distributed ledger with the provided configuration.
     * This method sets up the genesis block and initial validator nodes.
     * Requires that an encryption entity has been previously configured.
     *
     * @param startingData StatusDataContract containing initial ledger configuration,
     *                    including validator nodes, accounts, and ledger parameters
     * @return String status message: "OK" if successful, "Already configured" if ledger exists,
     *         or "Set Encryption entity first" if no encryption entity is configured
     */
    @ResponseBody
    @PostMapping("/initCreateNewLedger")
    public String initCreateNewLedger(@RequestBody StatusDataContract startingData) {
        ValidatorNode validatorNode;
        if(webServiceEngine.getStatusDataContract() != null)
        {
            return "Already configured";
        }
        if(webServiceEngine.getEncryptionEntity() == null)
        {
            return "Set Encryption entity first";
        }
        Optional<ValidatorNode> validatorNodeOptional = startingData.getValidatorNodeList().stream().filter(v -> v.getPublicKey().equals(webServiceEngine.getEncryptionEntity().getPublicKey())).findFirst();
        if(validatorNodeOptional.isEmpty())
        {
            return "Current validator node not found in Status contract";
        }
        validatorNode = validatorNodeOptional.get();
        if(validatorNode.getAddress() == null)
        {
            Map<Integer,Integer> address = new HashMap<Integer,Integer>();
            address.put(1,0);
            validatorNode.setAddress(address);
        }
        validatorNode.setSignature(
                webServiceEngine.getEncryptionEntity().signMessage(
                        validatorNode.getStringToSign()
                ));

        webServiceEngine.setThisValidatorNode(validatorNode);
        WebServiceInitializer.initAndCreateNewLedger(webServiceEngine, startingData);
        this.startTimer();
        return "OK";
    }

    /**
     * Displays the form page for creating a new ledger.
     * Pre-populates the form with default configuration including the current server's public key.
     *
     * @param model Spring MVC model for passing form configuration to the view
     * @return String view name "JSONInput" for the new ledger creation form
     */
    @GetMapping("/server/createNewLedger")
    public String initCreateNewLedgerGet(Model model) {
        String publicKey = "PUBLIC_KEY";
        if(this.webServiceEngine.getEncryptionEntity() != null)
        {
            publicKey = this.webServiceEngine.getEncryptionEntity().getPublicKey();
        }
        model.addAttribute("apiEndpoint","/initCreateNewLedger");
        model.addAttribute("title","Create new Ledger");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste JSON here:");
        model.addAttribute("jsonDefault","{\n" +
                "\"id\": 1,\n" +
                "\"distributedLedgerAccounts\":[\n" +
                "{\n" +
                "    \"publicKey\":\"" + publicKey +"\",\n" +
                "    \"name\": \"ACCOUNT1_NAME\",\n" +
                "    \"amount\": \"1000.00\",\n" +
                "    \"validatorNode\":\"" + publicKey +"\"\n" +
                "}],\n" +
                "\"validatorNodeList\":[{\n" +
                "    \"connectionString\": \"CONNECTION_STRING\",\n" +
                "    \"publicKey\":\"" + publicKey +"\"\n" +
                "}],\n" +
                "\"ledgerParameters\":{\n" +
                "    \"messageUpdateFrequencyPerHour\": 30,\n" +
                "    \"frameProcessingTimeMilliseconds\": 5000,\n" +
                "    \"maxConnections\": 20,\n" +
                "    \"amountRequestedToBeValidator\": \"1\",\n" +
                "    \"transactionCost\": \"0.1\",\n" +
                "    \"maxTransactionsPerBlock\": 1000,\n" +
                "    \"transmitRedundancy\": 1\n" +
                "}\n" +
                "}");
        return "JSONInput";
    }

    /**
     * Record class for join existing ledger requests.
     * Contains the connection strings needed for a node to join an existing network.
     *
     * @param thisConnectionString String connection string for this node
     * @param connectionStringDestination String connection string of the target node to connect to
     */
    public record joinExistingLedgerRequest(String thisConnectionString, String connectionStringDestination){
    }

    /**
     * Joins an existing distributed ledger network by connecting to a known validator node.
     * Initiates the authentication and synchronization process with the existing network.
     *
     * @param _request joinExistingLedgerRequest containing this node's connection string
     *                and the destination node's connection string for initial contact
     * @return String status message indicating success or failure of the join operation
     */
    @ResponseBody
    @PostMapping("/joinExistingLedger")
    public String joinExistingLedger(@RequestBody joinExistingLedgerRequest _request) {
        String ret;
        if(webServiceEngine.getStatusDataContract() != null)
        {
            return "Already Started";
        }
        return WebServiceInitializer.joinExistingLedger(webServiceEngine, _request.connectionStringDestination, _request.thisConnectionString);

    }

    /**
     * Displays the form page for joining an existing ledger.
     * Provides interface for specifying connection strings for network joining.
     *
     * @param model Spring MVC model for passing form configuration to the view
     * @return String view name "JSONInput" for the join ledger form
     */
    @GetMapping("/server/joinExistingLedger")
    public String joinExistingLedgerGet(Model model) {
        model.addAttribute("apiEndpoint","/joinExistingLedger");
        model.addAttribute("title","Join existing Ledger");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste JSON here:");
        model.addAttribute("jsonDefault","{\n" +
                "\"thisConnectionString\":\"thisConnectionString\",\n" +
                "\"connectionStringDestination\":\"connectionStringDestination\"\n" +
                "}");
        return "JSONInput";
    }

    /**
     * Completes the initialization process for a new node joining the network.
     * This is called asynchronously after successful authentication to provide
     * the latest validated data contract to the newly joined node.
     *
     * @param _lastDataContract StatusDataContract containing the most recent
     *                         validated state of the distributed ledger
     */
    @ResponseBody
    @PostMapping("/initialize")
    public boolean initialize(@RequestBody StatusDataContract _lastDataContract) {
        WebServiceInitializer.initialize(webServiceEngine, _lastDataContract);
        this.startTimer();
        return true;
    }

    /**
     * Provides a challenge string that must be digitally signed for authentication.
     * This string is used in the two-phase authentication process where clients
     * must prove ownership of their private key by signing this challenge.
     *
     * @return String challenge text that must be signed with the client's private key
     */
    @ResponseBody
    @GetMapping("/requestAuthenticationStringToSign")
    public String requestAuthenticationStringToSign() {
        return webServiceEngine.requestAuthenticationStringToSign();
    }

    /**
     * Record class for server authentication requests.
     * Contains all necessary information for validating a server's identity and connection.
     *
     * @param publicKey String Base64 encoded public key of the requesting server
     * @param signedMessage String Base64 encoded signature of the challenge string
     * @param connectionString String connection details for reaching the requesting server
     * @param signedConnectionString String Base64 encoded signature of the connection string
     */
    public record AuthenticateServerRequest(String publicKey, String signedMessage, String connectionString, String signedConnectionString){
    }

    /**
     * Authenticates a server attempting to join the network.
     * Verifies that the requesting server owns the private key corresponding
     * to their claimed public key by validating their digital signature.
     *
     * @param request AuthenticateServerRequest containing the public key, signed challenge,
     *               connection string, and signed connection string for verification
     * @return boolean true if authentication succeeds and server is accepted into network,
     *         false if authentication fails
     */
    @ResponseBody
    @PostMapping("/authenticateServer")
    public boolean authenticateServerWrapper(@RequestBody AuthenticateServerRequest request) {
        return webServiceEngine.authenticateServer(request.publicKey, request.signedMessage, request.connectionString, request.signedConnectionString );
    }

    /**
     * Retrieves the current transport message being transmitted across the network.
     * This endpoint provides access to the latest block data and can be used
     * for network synchronization and as a cached response in CDN deployments.
     *
     * @return TransportMessageDataContract containing the current block data
     *         and transaction information being propagated through the network
     */
    @ResponseBody
    @GetMapping("/getCurrentlyTransmittedTransportMessage")
    public TransportMessageDataContract getCurrentlyTransmittedTransportMessage()
    {
        return webServiceEngine.getCurrentlyTransmittedTransportMessage();
    }

    /**
     * Submits a vote for changing ledger parameters.
     * Allows validator nodes to propose modifications to system parameters
     * such as transaction costs, block timing, and validation requirements.
     *
     * @param _LedgerParameters LedgerParameters object containing the proposed
     *                         parameter changes to be voted on by the network
     */
    @ResponseBody
    @PostMapping("/changeVotedParameters")
    public void changeVotedParameters(@RequestBody LedgerParameters _LedgerParameters)//, String signatureBase64)
    {
        //if(Encryption.verifySignedMessage(_LedgerParameters.getStringToSign(), webServiceEngine.getEncryptionEntity().getPublicKey(),Encryption.base64ToByteArray(signatureBase64)))
        //{
        webServiceEngine.getPendingNextMessage().setVotedParameterChanges(_LedgerParameters);
        //}
    }

    /**
     * Displays the form page for changing voted parameters.
     * Provides interface for validator nodes to submit parameter change proposals.
     *
     * @param model Spring MVC model for passing form configuration to the view
     * @return String view name "JSONInput" for the parameter change form
     */
    @GetMapping("/server/changeVotedParameters")
    public String changeVotedParameters(Model model) {
        model.addAttribute("apiEndpoint","/changeVotedParameters");
        model.addAttribute("title","Change Voted Parameters");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste JSON here:");
        model.addAttribute("jsonDefault","{\n" +
                "\"messageUpdateFrequencyPerHour\": 30,\n" +
                "\"frameProcessingTimeMilliseconds\": 5000,\n" +
                "\"maxConnections\": 20,\n" +
                "\"amountRequestedToBeValidator\": \"1\",\n" +
                "\"transactionCost\": \"0.1\",\n" +
                "\"maxTransactionsPerBlock\": 1000\n" +
                "\"distributedLedgerAccountReassignProposals\": [\n" +
                "{\n" +
                "    \"publicKey\":\"" +
                "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAunH4nCceRoBkLW1Ec0j4plIxBnUVH6LcPRk2s5wGOrW8SP7gdMHH2wywh7fVTH8GWo0VLr/9Wz1kyS8GWm3XOTmlX0YEyz1ogHBSy4Sp2cDls5XMX9ct4SssMoz6EU+tkHcNYHyM0KUd8hvynrT7YTbCxSWJKmz+Wez1K93wb+jV6pi4tH9x0+4fyxq38Z3z/MK7U/D00neOCMQGB7KH0qUrTG6ZEB57X4h1frOmmwvBqnQiGDDdsVf+yHJEvbPJjoCshg8l3O3TxqZutzEup7D2tPDKkm8xmRGl1FqoWaHoa86urW1+JOrIpaUhujLZWgGUuKv6yyF47MdKV4PbQMDmbHAXhLM81wbnnv7nYpNIOwTA93OXjHPfKH2EkCP2a7U/qz9H6Oe7lKyd9oxkbcdXxU2+bbtO94mgBPsIewhspBuUWyqgXeJo8ByI73j6ynow0vIc7v8EHRuLPgAZl2/zmXuwIChYf46w8auNeMV68aNll+Au1ILE5SGy3kHRAgMBAAE=" +
                "\",\n" +
                "    \"name\": \"SHINJI\",\n" +
                "    \"amount\": \"1000.00\",\n" +
                "    \"validatorNode\":\"" +
                "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAunH4nCceRoBkLW1Ec0j4plIxBnUVH6LcPRk2s5wGOrW8SP7gdMHH2wywh7fVTH8GWo0VLr/9Wz1kyS8GWm3XOTmlX0YEyz1ogHBSy4Sp2cDls5XMX9ct4SssMoz6EU+tkHcNYHyM0KUd8hvynrT7YTbCxSWJKmz+Wez1K93wb+jV6pi4tH9x0+4fyxq38Z3z/MK7U/D00neOCMQGB7KH0qUrTG6ZEB57X4h1frOmmwvBqnQiGDDdsVf+yHJEvbPJjoCshg8l3O3TxqZutzEup7D2tPDKkm8xmRGl1FqoWaHoa86urW1+JOrIpaUhujLZWgGUuKv6yyF47MdKV4PbQMDmbHAXhLM81wbnnv7nYpNIOwTA93OXjHPfKH2EkCP2a7U/qz9H6Oe7lKyd9oxkbcdXxU2+bbtO94mgBPsIewhspBuUWyqgXeJo8ByI73j6ynow0vIc7v8EHRuLPgAZl2/zmXuwIChYf46w8auNeMV68aNll+Au1ILE5SGy3kHRAgMBAAE=" +
                "\"\n" +
                "},\n" +
                "{\n" +
                "    \"publicKey\":\"" +
                "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAk5GJ451dsBR9rPqxDmWEOab/FVIYItP2O8JHjCe6Pfvf+73vJuXIRLs+yZxohAp++zqmguo1gXJHZsVn4CVvEWlmitOCm4U7P3HugOXtiQJjBpK0U9gz+4s+qMSBEE+zSTZq+1QCUXESzoQ/+bGfnYTsH7xYk2BqexCwi4TAxSLclqJrDRGkRX8AynR4A8KjnJZjlFmtYaHNalpVuLMIorRd5IEYhIlf/mgmFyJwSpcJq+PozR7yDZFrMg2388e1Z4N8DjcxlaDpEguzdRCpd6pRW9Akb1jvfOlZJ4g394+cvz3ybrRM4jv4BfCyWicrG6THPz/ACz8YOIqGqcM3M/JdtKUe7aoxYwY+CzHA+1lyaJxigjU16uesAmv4pqxtqlTspeIiYzj5WoW3QpCWD1sC+amlDo2bpz8l3N5gFLqOaX2LAl8NYAOOPOHcHkp7Lj9/BUHV9uCpbLYxO8NFD8ubTpFooaKR6g9MCdZ4zTF6SxkRuk0l5wfANSUIYoxvAgMBAAE=" +
                "\",\n" +
                "    \"name\": \"AURON\",\n" +
                "    \"amount\": \"1000.00\",\n" +
                "    \"validatorNode\":\"" +
                "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAk5GJ451dsBR9rPqxDmWEOab/FVIYItP2O8JHjCe6Pfvf+73vJuXIRLs+yZxohAp++zqmguo1gXJHZsVn4CVvEWlmitOCm4U7P3HugOXtiQJjBpK0U9gz+4s+qMSBEE+zSTZq+1QCUXESzoQ/+bGfnYTsH7xYk2BqexCwi4TAxSLclqJrDRGkRX8AynR4A8KjnJZjlFmtYaHNalpVuLMIorRd5IEYhIlf/mgmFyJwSpcJq+PozR7yDZFrMg2388e1Z4N8DjcxlaDpEguzdRCpd6pRW9Akb1jvfOlZJ4g394+cvz3ybrRM4jv4BfCyWicrG6THPz/ACz8YOIqGqcM3M/JdtKUe7aoxYwY+CzHA+1lyaJxigjU16uesAmv4pqxtqlTspeIiYzj5WoW3QpCWD1sC+amlDo2bpz8l3N5gFLqOaX2LAl8NYAOOPOHcHkp7Lj9/BUHV9uCpbLYxO8NFD8ubTpFooaKR6g9MCdZ4zTF6SxkRuk0l5wfANSUIYoxvAgMBAAE=" +
                "\"\n" +
                "}\n" +
                "]}");
        return "JSONInput";
    }

    /**
     * Retrieves the current status of the distributed ledger system.
     * Provides comprehensive information about accounts, balances, validator nodes,
     * and system parameters. Useful for monitoring and debugging the ledger state.
     *
     * @return StatusDataContract containing complete system status including
     *         account balances, validator information, and current parameters
     */
    @ResponseBody
    @GetMapping("/getStatus")
    public StatusDataContract getStatus()
    {
        StatusDataContract ret;
        ret = webServiceEngine.getStatusDataContract();
        return ret;
        //return webServiceEngine.getStatusDataContract();
    }

    /**
     * Retrieves account information for a specific public key.
     * Returns the account details including balance and validator assignment.
     *
     * @param _publicKey String public key of the account to query
     * @return DistributedLedgerAccount containing account details and current balance,
     *         or null if the account is not found
     */
    @ResponseBody
    @PostMapping("/AccountTotals")
    public DistributedLedgerAccount AccountTotal(@RequestBody String _publicKey) {
        return DistributedLedgerAccount.find(webServiceEngine.getStatusDataContract().getAccountsList(), _publicKey);
    }

    /**
     * Submits a list of payment transactions to be included in the next block.
     * Validates and queues the transactions for processing by the network consensus.
     * Each payment must be properly signed by the sender's private key.
     *
     * @param _requestedPayments List of ExternalPayment objects containing transaction details
     *                          including sender, recipient, amount, and digital signature
     * @return List<ExternalPayment> processed payments with validation results
     *         and transaction IDs assigned by the system
     */
    @ResponseBody
    @PostMapping("/spend")
    public List<ExternalPayment> spendList(@RequestBody List<ExternalPayment> _requestedPayments) {
        return webServiceEngine.notifySpend(_requestedPayments);
    }

    /**
     * Displays the form page for submitting payment transactions.
     * Provides interface for creating and submitting spend transactions to the network.
     *
     * @param model Spring MVC model for passing form configuration to the view
     * @return String view name "JSONInput" for the payment submission form
     */
    @GetMapping("/server/spend")
    public String spendListGet(Model model) {
        model.addAttribute("apiEndpoint","/spend");
        model.addAttribute("title","Receive spend messages");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste JSON here:");
        model.addAttribute("jsonDefault", "[\n" +
                "{\n" +
                "\"publicKeyFrom\":\"PUBLIC_KEY_FROM\",\n" +
                "\"publicKeyTo\":\"PUBLIC_KEY_TO\",\n" +
                "\"paymentComment\":\"COMMENT\",\n" +
                "\"amount\": \"1.00\",\n" +
                "\"blockId\": 2\n" +
                "},\n" +
                "{\n" +
                "\"publicKeyFrom\":\"PUBLIC_KEY_FROM\",\n" +
                "\"publicKeyTo\":\"PUBLIC_KEY_TO\",\n" +
                "\"paymentComment\":\"COMMENT\",\n" +
                "\"amount\": \"1.00\",\n" +
                "\"blockId\": 2\n" +
                "}\n" +
                "]");
        return "JSONInput";
    }

    /**
     * Submits a voting delegation change to the network.
     * Allows account holders to delegate their voting rights to validator nodes
     * for parameter changes and network governance decisions.
     *
     * @param _accountAttributesUpdate VotingDelegation object containing the delegation details
     *                         including delegator, delegate, and digital signature
     */
    @ResponseBody
    @PostMapping("/updateAccountAttributes")
    public String updateAccountAttributes(@RequestBody AccountAttributesUpdate _accountAttributesUpdate) {
       return  webServiceEngine.notifyupdateAccountAttributes(_accountAttributesUpdate);
    }

    /**
     * Displays the form page for changing vote delegation.
     * Provides interface for account holders to delegate their voting rights.
     *
     * @param model Spring MVC model for passing form configuration to the view
     * @return String view name "JSONInput" for the vote delegation form
     */
    @GetMapping("/server/updateAccountAttributes")
    public String voteDelegation(Model model) {
        model.addAttribute("apiEndpoint","/updateAccountAttributes");
        model.addAttribute("title","update Account Attributes");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste JSON here:");
        model.addAttribute("jsonDefault","{\n" +
                "\"from\": \"ACCOUNT_PUBLIC_KEY\",\n" +
                "\"name\": \"ACCOUNT_NEW_NAME\",\n" +
                "\"delegated\": \"PUBLIC_KEY_SERVER\",\n" +
                "\"blockId\": 5,\n" +
                "\"previousRevisionHash\": \"HASH\",\n" +
                "\"signatureValue\": \"SIGNATURE\"\n" +
                "}");
        return "JSONInput";
    }

    /**
     * Manually processes a transport message data contract.
     * This endpoint allows for manual injection of transport messages,
     * typically used for testing or recovery scenarios.
     *
     * @param transportMessageDataContract TransportMessageDataContract containing
     *                                   the message data to be processed by the system
     */
    @ResponseBody
    @PostMapping("/processTransportMessageDataContract")
    public void manualProcessTransportMessageDataContract(@RequestBody TransportMessageDataContract transportMessageDataContract) {
        webServiceEngine.receiveData(transportMessageDataContract);
    }

    /**
     * Retrieves the available balance for a specific account.
     * Returns the current spendable amount for the given public key.
     *
     * @param publicKey String public key of the account to query
     * @return BigDecimal representing the available balance for the specified account
     */
    @ResponseBody
    @GetMapping("/getAmount")
    public BigDecimal getAmount(String publicKey) {
        return webServiceEngine.getAmountAvailable(publicKey);
    }

    /**
     * Retrieves the current block ID being processed by the system.
     * Useful for synchronization and determining the current state of the ledger.
     *
     * @return int current block ID that is being transmitted across the network
     */
    @ResponseBody
    @GetMapping("/getBlockId")
    public int getBlockId()
    {
        return this.webServiceEngine.getCurrentlyTransmittedTransportMessage().getBlockId();
    }

    @ResponseBody
    @GetMapping("/getLedgerHistory")
    public LedgerHistory getLedgerHistory()
    {
        return this.webServiceEngine.getLedgerHistory();
    }
    @ResponseBody
    @GetMapping("/getLedgerHistoryInterval")
    public LedgerHistory getLedgerHistoryInterval(int start, int end)
    {
        return this.webServiceEngine.getLedgerHistory().getInterval(start,end);
    }

    @ResponseBody
    @GetMapping("/setNewLedgerHistoryOrigin")
    public String setNewLedgerOrigin(int id)
    {
        this.webServiceEngine.getLedgerHistory().changeOriginalContract(id);
        return "OK";
    }

    public void startTimer() {

        //this.webServiceEngine.startTimer();

        //starting from the hour to soft align the timescales
        final long epochNow = Instant.now().toEpochMilli();
        final long epochHourInMilli = epochNow - Math.floorMod(Instant.now().toEpochMilli(), 1000 * 60 * 60);
        final long schedulerMargin = webServiceEngine.getStatusDataContract().getLedgerParameters().getFrameProcessingTimeMilliseconds();
        final long timePerFrame =  Math.divideExact( 1000*60*60 ,Math.max(webServiceEngine.getStatusDataContract().getLedgerParameters().getMessageUpdateFrequencyPerHour(),1));
        long counter;
        //find the time of the start of the next blockTempVersion that happens in the future
        counter = ((epochNow - epochHourInMilli + 2 * schedulerMargin) + timePerFrame - 1) / timePerFrame;
        //and then get the time interval so that it starts on time
        long interval = (epochHourInMilli + counter *timePerFrame) - epochNow;
        long interval2 = interval + webServiceEngine.getStatusDataContract().getLedgerParameters().getFrameProcessingTimeMilliseconds();
        try
        {
            scheduler.schedule(this::TimedProcessingEvent, interval, TimeUnit.MILLISECONDS);
            scheduler.schedule(this::TimedReadingEvent, interval2, TimeUnit.MILLISECONDS);
            scheduler.schedule(this::startTimer,interval2 - schedulerMargin,TimeUnit.MILLISECONDS);
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("error",true,e);
            //throw new RuntimeException("fail");
        }

    }

    public void TimedProcessingEvent()
    {
        this.webServiceEngine.TimedProcessingEvent();
    }
    public void TimedReadingEvent()
    {
        this.webServiceEngine.TimedReadingEvent();
    }


}