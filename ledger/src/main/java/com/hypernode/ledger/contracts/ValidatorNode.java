package com.hypernode.ledger.contracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hypernode.ledger.encryptionInterfaces.Encryption;

import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidatorNode
{
    private Map<Integer,Integer> address;//hypernode address
    private String connectionString;
    private String publicKey;
    private String signature;

    public static ValidatorNode findByPublicKey(List<ValidatorNode> _list, String _publicKey)
    {
        return _list.stream().filter(validatorNode -> validatorNode.getPublicKey().equals( _publicKey)).findFirst().get();
    }
    public static List<ValidatorNode> filterByAddress(List<ValidatorNode> _list, Set<String> _address)
    {
        return _list.stream().filter(validatorNode -> _address.contains(LedgerParameters.calculateAddressString(validatorNode.address))).toList();
    }

    public static List<ValidatorNode> merge(List<ValidatorNode> _list, Set<ValidatorNode> _validatorNodesUpdate, LedgerParameters _ledgerParameters)
    {
        //Iterator<ValidatorNode> validatorNodesUpdateIterator = _validatorNodesUpdate.iterator();
        ListIterator<ValidatorNode> validatorNodeListIterator;
        ValidatorNode validatorNode;
        ValidatorNode existingValidatorNode;
        int listSize;
        List<ValidatorNode> processingList = new ArrayList<>();
        List<ValidatorNode> returnList = new ArrayList<>();
        //processingList.addAll(_list);
        /*
        //scroll through the validatornodes to add
        while (validatorNodesUpdateIterator.hasNext())
        {
            validatorNode = validatorNodesUpdateIterator.next();
            ValidatorNode finalValidatorNode = validatorNode;
            try
            {
                //if it already exists remove the old validatornode
                existingValidatorNode = processingList.stream().filter(ValidatorNode -> ValidatorNode.getPublicKeyBase64().equals( finalValidatorNode.getPublicKeyBase64())).findFirst().get();
                processingList.remove(existingValidatorNode);

            }
            catch (Exception e)
            {
                //item does not exist so no need to remove it
            }
            if(!finalValidatorNode.connectionString.isEmpty())
            {
                processingList.add(finalValidatorNode);
            }
        }
        Collections.sort(processingList, (a, b) -> b.getPublicKeyBase64().compareTo(a.getPublicKeyBase64()));

        */
        processingList.addAll(
                _validatorNodesUpdate.stream()
                        .filter(v -> !v.connectionString.isEmpty() && !v.connectionString.isBlank())
                        .toList()
        );

        processingList.addAll(
                _list.stream().filter(v -> processingList.stream()
                        .noneMatch(c -> c.getPublicKey().equals(v.getPublicKey())))
                        .toList()
        );
        processingList.sort(Comparator.comparing(ValidatorNode::getPublicKey));

        listSize = processingList.size();
        _ledgerParameters.setGroupParameters(listSize);
        validatorNodeListIterator = processingList.listIterator();
        while (validatorNodeListIterator.hasNext())
        {
            validatorNode = validatorNodeListIterator.next();
            validatorNode.address = _ledgerParameters.calculateAddress(validatorNodeListIterator.nextIndex());
            returnList.add(validatorNode);
        }
        return returnList;
    }
    @JsonIgnore
    public boolean validate()
    {
        return Encryption.verifySignedMessage(this.getStringToSign(), this.getPublicKey(), this.getSignature());
    }
    @JsonIgnore
    public String getStringToSign()
    {
        return this.publicKey +
                this.connectionString;

    }
    @JsonIgnore
    @Override
    public String toString() {
        return "ValidatorNode{" +
                ", address=" + address +
                ", connectionString='" + connectionString + '\'' +
                ", publicKey=" + getPublicKey() +
                ", signature=" + getSignature() +
                '}';
    }
    @JsonIgnore
    public String uniqueString() {
        return  address +
                ":" + connectionString + '\'' +
                ":" + getPublicKey() +
                ":" + getSignature()
                ;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidatorNode d = (ValidatorNode) o;
        return this.uniqueString().equals(d.uniqueString());
    }
    @JsonIgnore
    @Override
    public int hashCode() {
        return this.uniqueString().hashCode();
    }

//getters and setters
    public Map<Integer, Integer> getAddress() {return address;}
    public void setAddress(Map<Integer, Integer> address) {this.address = address;}
    public String getConnectionString() {return connectionString;}
    public void setConnectionString(String connectionString) {this.connectionString = connectionString;}
    public String getPublicKey() { return this.publicKey;}
    public void setPublicKey(String publicKey) {this.publicKey = publicKey;}
    public String getSignature() { return this.signature;}
    public void setSignature(String _signature) {this.signature = _signature;}

}
