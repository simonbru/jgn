/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn.message;

/**
 * This is an internal component and need not
 * be handled directly by implementers of the
 * API. Internally this is the message that
 * is sent when a CertifiedMessage is received
 * to let the sender know that the message was
 * received successfully.
 * 
 * @author Matthew D. Hicks
 */
public class Receipt extends Message {
    private long certifiedId;
    
    public void setCertifiedId(long certifiedId) {
        this.certifiedId = certifiedId;
    }
    
    public long getCertifiedId() {
        return certifiedId;
    }
}
