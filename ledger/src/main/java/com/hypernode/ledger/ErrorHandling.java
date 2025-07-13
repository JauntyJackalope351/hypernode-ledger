package com.hypernode.ledger;

public class ErrorHandling {

    /** Cuatom event logging method
     * Since this app needs to keep running most of the errors need to fail silently.
     * I've therefore decided instead of throwing errors i would be calling this method instead, whenever possible,
     * so that the errors are still debuggable but it is harder for a malicious attacker to make the app crash.
     * @param eventDesc human placed string to describe the circumstances of the error
     * @param throwError should this error be thrown or be silenced
     * @param e original exception, if available
     */
    public static void logEvent( String eventDesc, boolean throwError, Exception e)
    {
        System.out.print(eventDesc);
        if (throwError)
        {
            throw new RuntimeException(e);
        }
    }
}
