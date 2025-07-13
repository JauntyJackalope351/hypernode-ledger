package com.hypernode.ledger.controller;

import com.hypernode.ledger.encryptionInterfaces.Encryption;
import com.hypernode.ledger.encryptionInterfaces.EncryptionEntity_Integrated;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller("/encryptionEntity")
public class EncryptionEndpointController {
    EncryptionEntity_Integrated encryptionEntity;

    @ResponseBody
    @PostMapping("/encryptionEntity/setEncryptionEntityIntegrated")
    public String setEncryptionEntityIntegrated(@RequestBody EncryptionEntity_Integrated jsonInput) {
        String testMessage = "the quick brown fox jumps over the lazy dog";
        if(encryptionEntity != null) {
            return "Already Started";
        }
        if(!Encryption.verifySignedMessage(testMessage,jsonInput.getPublicKey(),jsonInput.signMessage(testMessage)))
        {
            return "Invalid Keys";
        }
        encryptionEntity = jsonInput;
        return "OK";
    }

    @ResponseBody
    @GetMapping("/encryptionEntity/getPublicKey")
    public String getPublicKey() {
        return encryptionEntity.getPublicKey();
    }
    @ResponseBody
    @PostMapping("/encryptionEntity/signMessage")
    public String signMessage(@RequestBody String message) {
        return encryptionEntity.signMessage(message);
    }

    @GetMapping("/encryptionEntity/setEncryptionEntityIntegrated")
    public String clientSetEncryptionEntityIntegrated(Model model) {
        model.addAttribute("apiEndpoint","/encryptionEntity/setEncryptionEntityIntegrated");
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

}
