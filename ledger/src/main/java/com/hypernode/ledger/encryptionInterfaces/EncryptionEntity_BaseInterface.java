package com.hypernode.ledger.encryptionInterfaces;

public interface EncryptionEntity_BaseInterface {

    String getPublicKey();

    String signMessage(String _message);

}
