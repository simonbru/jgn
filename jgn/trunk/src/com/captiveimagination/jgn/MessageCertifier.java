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
package com.captiveimagination.jgn;

import java.io.*;
import java.util.*;

import com.captiveimagination.jgn.message.*;

/**
 * MessageCertifier is the internal mechanism instantiated
 * and managed by MessageServer to certify messages that
 * extend CertifiedMessage and send back confirmation to
 * the sender. This need not be directly instantiated.
 * 
 * @author Matthew D. Hicks
 */
public class MessageCertifier {
    private UDPMessageServer server;
    
    private List queue;
    private List certified;
    
    public MessageCertifier(UDPMessageServer server) {
        this.server = server;
        
        queue = new ArrayList();
        certified = new ArrayList();
    }
    
    public void enqueue(CertifiedMessage message, IP address, int port) {
        queue.add(new Certification(message, address, port));
    }
    
    public synchronized void certify(long id) {
    	certified.add(new Long(id));
    }
        
    public synchronized void update() {
        Certification c;
        List temp = new ArrayList();
        temp.addAll(queue);
        Iterator iterator = temp.iterator();
        while (iterator.hasNext()) {
        	c = (Certification)iterator.next();
            if ((c.getRetryCount() % 10 == 0) && (c.getRetryCount() > 0)) {
                System.err.println("Message unable to send after: " + c.getMessage().getTried() + ", " + c.getCertificationId()+ " "+queue.size());
            }
            if (isCertified(c.getCertificationId())) {
                certified.remove(new Long(c.getCertificationId()));
                queue.remove(c);
                //System.out.println("message certified: " + c.getMessage().getId() + " - " + c.getMessage().getClass().getName() + " after " + c.getMessage().getTried() + " tries.");
            } else if ((c.getRetryCount() >= c.getMessage().getMaxTries()) && (c.getMessage().getMaxTries() != 0)) {
                System.err.println("Unable to send message: " + c.getMessage().getId() + " - " + c.getMessage().getClass().getName() + " after " + c.getMessage().getTried() + " tries.");
                queue.remove(c);
            } else {
                if (System.currentTimeMillis() > c.getLastTry() + c.getMessage().getResendTimeout()) {
                    try {
                    	c.getMessage().setId(c.getCertificationId());
                        server.resendMessage(c.getMessage(), c.getRecipientAddress(), c.getRecipientPort());
                        System.err.println("RESENGING "+c.getMessage().getId());
                    } catch(IOException exc) {
                        throw new RuntimeException(exc);
                    }
                    c.tried();
                }
            }
        }
    }
    
    public int size() {
        return queue.size();
    }
    
    private boolean isCertified(long id) {
    	return certified.contains(new Long(id));
    }
}
