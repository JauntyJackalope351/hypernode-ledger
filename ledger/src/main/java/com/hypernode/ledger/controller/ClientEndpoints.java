package com.hypernode.ledger.controller;

import com.hypernode.ledger.client.ClientEngine;
import com.hypernode.ledger.client.ExternalPayment;
import com.hypernode.ledger.ErrorHandling;
import com.hypernode.ledger.contracts.AccountAttributesUpdate;
import com.hypernode.ledger.encryptionInterfaces.Encryption;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_ExternalServer;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_Integrated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller responsible for handling all client-facing operations related to encryption setup,
 * external payment generation, vote delegation, and ledger endpoint configuration.
 *
 * <p>Each method serves either an API endpoint (returning JSON) or a form-based interface (returning a view name).
 */
@Controller("/hdls-client")
public class ClientEndpoints {

    @Autowired
    private ClientEngine clientEngine;


    /**
     * Displays client metadata (e.g., public key, last known hash).
     *
     * @param model Spring Model to populate with attributes
     * @return view name for client page
     */
    @GetMapping("/hdls-client")
    public String client(Model model) {
        String publicKey = "PUBLIC_KEY";
        String endpoint = "LAST_HASH";
        if(this.clientEngine.getEncryptionEntity() != null)
        {
            publicKey = this.clientEngine.getEncryptionEntity().getPublicKey();
        }
        if(this.clientEngine.getEndpoint() != null)
        {
            endpoint = this.clientEngine.getEndpoint();
        }
        model.addAttribute("publicKey", publicKey);
        model.addAttribute("lastHash", endpoint);
        return "client"; // Corresponds to home.html
    }


    /**
     * Generates a new public/private key pair for creating additional accounts.
     * The keys are returned in Base64 encoded format as JSON.
     *
     * @return String JSON response containing both privateKeyBase64 and publicKeyBase64
     */
    @GetMapping("/hdls-client/newKeyPair")
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
     * Initializes an integrated encryption entity, verifying its signature before setting.
     *
     * @param jsonInput payload containing key information
     * @return status string
     */
    @ResponseBody
    @PostMapping("/hdls-client/setEncryptionEntityIntegrated")
    public String setEncryptionEntityIntegrated(@RequestBody EncryptionEntity_Integrated jsonInput) {
        String testMessage = "the quick brown fox jumps over the lazy dog";
        if(clientEngine.getEncryptionEntity() != null) {
            return "Already Started";
        }
        if(!Encryption.verifySignedMessage(testMessage,jsonInput.getPublicKey(),jsonInput.signMessage(testMessage)))
        {
            return "Invalid Keys";
        }

        clientEngine.setEncryptionEntity(jsonInput);
        return "OK";
    }

    /**
     * Displays form for setting an integrated encryption entity.
     */
    @GetMapping("/hdls-client/setEncryptionEntityIntegrated")
    public String clientSetEncryptionEntityIntegrated(Model model) {
        model.addAttribute("apiEndpoint","/hdls-client/setEncryptionEntityIntegrated");
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
     * Initializes an external encryption entity, verifying its signature before setting.
     */
    @ResponseBody
    @PostMapping("/hdls-client/setEncryptionEntityExternalServer")
    public String setEncryptionEntityExternalServer(@RequestBody EncryptionEntity_ExternalServer jsonInput) {
        String testMessage = "the quick brown fox jumps over the lazy dog";
        if(clientEngine.getEncryptionEntity() != null) {
            return "Already Started";
        }
        if(!Encryption.verifySignedMessage(testMessage,jsonInput.getPublicKey(),jsonInput.signMessage(testMessage)))
        {
            return "Invalid Keys";
        }

        clientEngine.setEncryptionEntity(jsonInput);
        return "OK";
    }

    /**
     * Displays form for setting an external encryption entity.
     */
    @GetMapping("/hdls-client/setEncryptionEntityExternalServer")
    public String setEncryptionEntityExternalServer(Model model) {
        model.addAttribute("apiEndpoint","/hdls-client/setEncryptionEntityExternalServer");
        model.addAttribute("title","Set Encryption Entity (Integrated)");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste connection string here:");
        model.addAttribute("jsonDefault", "");
        return "JSONInput";
    }

    /**
     * Sets or updates the validator endpoint.
     */
    @ResponseBody
    @PostMapping("/hdls-client/setEndpoint")
    public String setEndpointPost(@RequestBody String endpoint) {
        if (endpoint.equals(this.clientEngine.getEndpoint()))
        {
            return "Already configured";
        }
        this.clientEngine.setEndpoint(endpoint);
        return "OK";
    }

    /**
     * Displays form for setting the server endpoint.
     */
    @GetMapping("/hdls-client/setEndpoint")
    public String clientVoteDelegation(Model model) {
        model.addAttribute("apiEndpoint","/hdls-client/setEndpoint");
        model.addAttribute("title","Set Server endpoint");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste validator connection string here:");
        model.addAttribute("jsonDefault","");
        return "JSONInput";
    }

    /**
     * Signs an offline payment object using the local encryption key.
     */
    @ResponseBody
    @PostMapping("/hdls-client/clientNotifySpendOffline")
    public ExternalPayment clientNotifySpendOffline(@RequestBody ExternalPayment json) {
        json.signPayment(clientEngine.getEncryptionEntity());
        return json;
    }

    /**
     * Displays form for generating and signing offline payments.
     */
    @GetMapping("/hdls-client/clientNotifySpendOffline")
    public String clientAuthorizePaymentGet(Model model) {
        model.addAttribute("apiEndpoint","/hdls-client/clientNotifySpend");
        model.addAttribute("title","Generate spend messages");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste JSON here:");
        model.addAttribute("jsonDefault",
                "{\n" +
                "\"publicKeyFrom\":\""+clientEngine.getEncryptionEntity().getPublicKey()+"\",\n" +
                "\"publicKeyTo\":\"PUBLIC_KEY_TO\",\n" +
                "\"paymentComment\":\"COMMENT\",\n" +
                "\"amount\": \"1.00\",\n" +
                "\"blockId\": 2\n" +
                "}");
        return "JSONInput";
    }

    /**
     * Creates and sends a payment transaction using the current encryption entity.
     */
    @ResponseBody
    @PostMapping("/hdls-client/clientPay")
    public List<ExternalPayment> clientPay(@RequestBody ExternalPayment json)
    {
        List<ExternalPayment> list = new ArrayList<>();
        ExternalPayment payment = ExternalPayment.create(
                clientEngine.getEncryptionEntity().getPublicKey(),
                json.getTo(),
                json.getComment(),
                json.getAmount(),
                clientEngine.getNextAvailablePaymentBlock(),
                clientEngine.getEncryptionEntity()
        );
        list.add(payment);
        return clientEngine.sendPayments(list);
    }

    /**
     * Displays form for submitting payment requests.
     */
    @GetMapping("/hdls-client/clientPay")
    public String clientPayGet(Model model) {
        model.addAttribute("apiEndpoint","/hdls-client/clientPay");
        model.addAttribute("title","Make a payment");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste JSON here:");
        model.addAttribute("jsonDefault",
                "{\n" +
                        "\"to\":\"PUBLIC_KEY_TO\",\n" +
                        "\"comment\":\"COMMENT\",\n" +
                        "\"amount\": \"1.00\"" +
                        "}");
        return "JSONInput";
    }

    /**
     * Signs a voting delegation message offline.
     */
    @ResponseBody
    @PostMapping("/hdls-client/clientUpdateAccountAttributesOffline")
    public AccountAttributesUpdate clientUpdateAccountAttributesOfflineGet(@RequestBody AccountAttributesUpdate json) {
        json.setSignatureValue(clientEngine.getEncryptionEntity().signMessage(json.getStringToSign()));
        return json;
    }

    /**
     * Displays form for offline vote delegation signing.
     */
    @GetMapping("/hdls-client/clientUpdateAccountAttributesOffline")
    public String clientVoteDelegationOfflinePost(Model model) {
        model.addAttribute("apiEndpoint","/hdls-client/clientVoteDelegationOffline");
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
     * Delegates voting to another public key server-side.
     */
    @ResponseBody
    @PostMapping("/hdls-client/clientUpdateAccountAttributes")
    public String clientUpdateAccountAttributes(@RequestBody AccountAttributesUpdate json)
    {
        String ret;
        json.setFrom(this.clientEngine.getEncryptionEntity().getPublicKey());
        json.setBlockId( this.clientEngine.getNextAvailablePaymentBlock()-1);
        json.setPreviousBlockHash( this.clientEngine.getStatus().getHash());
        try
        {
            ret = clientEngine.changeVoteDelegation(json);
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("error",false,e);
            return e.getMessage();
        }
        return ret;
    }

    /**
     * Displays form for server-side vote delegation.
     */
    @GetMapping("/hdls-client/clientUpdateAccountAttributes")
    public String clientUpdateAccountAttributesPost(Model model) {
        model.addAttribute("apiEndpoint","/hdls-client/clientUpdateAccountAttributes");
        model.addAttribute("title","Update account attributes");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste JSON here:");
        model.addAttribute("jsonDefault","{\n" +
                "\"name\": \"ACCOUNT_NEW_NAME\",\n" +
                "\"description\": \"ACCOUNT_DESCRIPTION\",\n" +
                "\"delegated\": \"PUBLIC_KEY_SERVER\"\n" +
                "}");
        return "JSONInput";
    }

    /**
     * Retrieves the total balance for the given account public key.
     */
    @ResponseBody
    @PostMapping("/hdls-client/AccountTotals")
    public String AccountTotal(@RequestBody String publicKey) {
        return clientEngine.getAccountTotal(publicKey);
    }

    /**
     * Displays form to fetch account totals.
     */
    @GetMapping("/hdls-client/getAccountTotals")
    public String AccountTotalGet(Model model) {
        model.addAttribute("apiEndpoint","/hdls-client/AccountTotals");
        model.addAttribute("title","Account Totals");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste name or public key here:");
        model.addAttribute("jsonDefault","");
        return "JSONInput";
    }

    /**
     * Retrieves the total balance for the given account public key.
     */
    @ResponseBody
    @PostMapping("/hdls-client/getIPAddress")
    public String getIPAddress(@RequestBody String publicKey) { return clientEngine.getIPAddress(publicKey); }

    /**
     * Displays form to fetch account totals.
     */
    @GetMapping("/hdls-client/getIPAddress")
    public String getIPAddress(Model model) {
        model.addAttribute("apiEndpoint","/hdls-client/getIPAddress");
        model.addAttribute("title","get IP Address");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste name or public key here:");
        model.addAttribute("jsonDefault","");
        return "JSONInput";
    }

    /**
     * Retrieves the total balance for the given account public key.
     */
    @ResponseBody
    @PostMapping("/hdls-client/getAccountInfo")
    public String getAccountInfo(@RequestBody String publicKey) {
        return clientEngine.getAccountInfo(publicKey);
    }

    /**
     * Displays form to fetch account totals.
     */
    @GetMapping("/hdls-client/getAccountInfo")
    public String getAccountInfo(Model model) {
        model.addAttribute("apiEndpoint","/hdls-client/getAccountInfo");
        model.addAttribute("title","get Account info");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste name or public key here:");
        model.addAttribute("jsonDefault","");
        return "JSONInput";
    }
}
