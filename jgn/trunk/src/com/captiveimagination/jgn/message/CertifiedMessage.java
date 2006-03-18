/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn.message;

/**
 * CertifiedMessage should be extended by messages that
 * need guaranteed delivery to their destination. These
 * messages are sent exactly the same way as other
 * messages, but internally the sender waits for a
 * received receipt certifying the message was received
 * properly. If the <code>resendTimeout</code> lapses
 * before a receipt is received the system will resend
 * the same message without assigning a new unique id.
 * 
 * @author Matthew D. Hicks
 */
public abstract class CertifiedMessage extends Message {
    private int tried;
    private boolean certified;
    
    public CertifiedMessage() {
        tried = 0;
    }
    
    /**
     * @return
     *      The amount of time to wait in milliseconds
     *      for a certification response before resending
     *      this message.
     */
    public abstract long getResendTimeout();
    
    /**
     * @return
     *      The number of attempts to make to send this
     *      message before discarding it. A value of 0
     *      will keep attempting until successful.
     */
    public abstract int getMaxTries();
    
    public boolean getCertified() {
    	return isCertified();
    }
    
    /**
     * @return
     * 		The certification state of this message.
     */
    public boolean isCertified() {
    	return certified;
    }
    
    /**
     * Set whether this message has been certified yet.
     * 
     * @param certified
     */
    public void setCertified(boolean certified) {
    	this.certified = certified;
    }
    
    public void failed() {
        tried++;
    }
    
    public int getTried() {
        return tried;
    }
}
