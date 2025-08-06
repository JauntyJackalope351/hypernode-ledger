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

import java.util.ArrayList;
import java.util.List;

/**
 * Controller responsible for handling all client-facing operations related to encryption setup,
 * external payment generation, vote delegation, and ledger endpoint configuration.
 *
 * <p>Each method serves either an API endpoint (returning JSON) or a form-based interface (returning a view name).
 */
@Controller("/client")
public class ClientEndpoints {

    @Autowired
    private ClientEngine clientEngine;

    /**
     * Displays client metadata (e.g., public key, last known hash).
     *
     * @param model Spring Model to populate with attributes
     * @return view name for client page
     */
    @GetMapping("/client")
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
     * Initializes an integrated encryption entity, verifying its signature before setting.
     *
     * @param jsonInput payload containing key information
     * @return status string
     */
    @ResponseBody
    @PostMapping("/client/setEncryptionEntityIntegrated")
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
    @GetMapping("/client/setEncryptionEntityIntegrated")
    public String clientSetEncryptionEntityIntegrated(Model model) {
        model.addAttribute("apiEndpoint","/client/setEncryptionEntityIntegrated");
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
    @PostMapping("/client/setEncryptionEntityExternalServer")
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
    @GetMapping("/client/setEncryptionEntityExternalServer")
    public String setEncryptionEntityExternalServer(Model model) {
        model.addAttribute("apiEndpoint","/client/setEncryptionEntityExternalServer");
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
    @PostMapping("/client/setEndpoint")
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
    @GetMapping("/client/setEndpoint")
    public String clientVoteDelegation(Model model) {
        model.addAttribute("apiEndpoint","/client/setEndpoint");
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
    @PostMapping("/client/clientNotifySpendOffline")
    public ExternalPayment clientNotifySpendOffline(@RequestBody ExternalPayment json) {
        json.signPayment(clientEngine.getEncryptionEntity());
        return json;
    }

    /**
     * Displays form for generating and signing offline payments.
     */
    @GetMapping("/client/clientNotifySpendOffline")
    public String clientAuthorizePaymentGet(Model model) {
        model.addAttribute("apiEndpoint","/client/clientNotifySpend");
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
    @PostMapping("/client/clientPay")
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
    @GetMapping("/client/clientPay")
    public String clientPayGet(Model model) {
        model.addAttribute("apiEndpoint","/client/clientPay");
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
    @PostMapping("/client/clientUpdateAccountAttributesOffline")
    public AccountAttributesUpdate clientUpdateAccountAttributesOfflineGet(@RequestBody AccountAttributesUpdate json) {
        json.setSignatureValue(clientEngine.getEncryptionEntity().signMessage(json.getStringToSign()));
        return json;
    }

    /**
     * Displays form for offline vote delegation signing.
     */
    @GetMapping("/client/clientUpdateAccountAttributesOffline")
    public String clientVoteDelegationOfflinePost(Model model) {
        model.addAttribute("apiEndpoint","/client/clientVoteDelegationOffline");
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
    @PostMapping("/client/clientUpdateAccountAttributes")
    public String clientUpdateAccountAttributes(@RequestBody AccountAttributesUpdate json) {

        json.setBlockId( this.clientEngine.getNextAvailablePaymentBlock()-1);
        json.setPreviousBlockHash( this.clientEngine.getStatus().getHash());
        try
        {
            clientEngine.changeVoteDelegation(json);
        }
        catch (Exception e)
        {
            ErrorHandling.logEvent("error",false,e);
            return e.getMessage();
        }
        return "OK";
    }

    /**
     * Displays form for server-side vote delegation.
     */
    @GetMapping("/client/clientUpdateAccountAttributes")
    public String clientUpdateAccountAttributesPost(Model model) {
        model.addAttribute("apiEndpoint","/client/clientUpdateAccountAttributes");
        model.addAttribute("title","Generate Vote Delegation");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste JSON here:");
        model.addAttribute("jsonDefault","{\n" +
                "\"from\": \"ACCOUNT_PUBLIC_KEY\",\n" +
                "\"name\": \"ACCOUNT_NEW_NAME\",\n" +
                "\"description\": \"ACCOUNT_DESCRIPTION\",\n" +
                "\"delegated\": \"PUBLIC_KEY_SERVER\",\n" +
                "}");
        return "JSONInput";
    }

    /**
     * Retrieves the total balance for the given account public key.
     */
    @ResponseBody
    @PostMapping("/client/AccountTotals")
    public String AccountTotal(@RequestBody String publicKey) {
        return clientEngine.getAccountTotal(publicKey);
    }

    /**
     * Displays form to fetch account totals.
     */
    @GetMapping("/client/getAccountTotals")
    public String AccountTotalGet(Model model) {
        model.addAttribute("apiEndpoint","/client/AccountTotals");
        model.addAttribute("title","Account Totals");
        model.addAttribute("info","");
        model.addAttribute("jsonLabel","Paste public key here:");
        model.addAttribute("jsonDefault","");
        return "JSONInput";
    }
}
