/*
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
