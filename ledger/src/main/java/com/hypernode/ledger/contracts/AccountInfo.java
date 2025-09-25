package com.hypernode.ledger.contracts;


public class AccountInfo {
    DistributedLedgerAccount account;
    ValidatorNode node;

    public DistributedLedgerAccount getAccount() {return account;}
    public void setAccount(DistributedLedgerAccount account) {this.account = account;}
    public ValidatorNode getNode() {return node;}
    public void setNode(ValidatorNode node) {this.node = node;}
}
