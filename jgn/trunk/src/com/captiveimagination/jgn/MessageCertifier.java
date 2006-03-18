/*
 * Created on Nov 29, 2005
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
        while ((c = getNext()) != null) {
            if ((c.getMessage().getTried() % 10 == 0) && (c.getMessage().getTried() > 0)) {
                System.err.println("Message unable to send after: " + c.getMessage().getTried() + ", " + c.getMessage().getId());
            }
            
            if (isCertified(c.getMessage().getId())) {
                certified.remove(new Long(c.getMessage().getId()));
                //System.out.println("message certified: " + c.getMessage().getId() + " - " + c.getMessage().getClass().getName() + " after " + c.getMessage().getTried() + " tries.");
            } else if ((c.getMessage().getTried() >= c.getMessage().getMaxTries()) && (c.getMessage().getMaxTries() != 0)) {
                System.err.println("Unable to send message: " + c.getMessage().getId() + " - " + c.getMessage().getClass().getName() + " after " + c.getMessage().getTried() + " tries.");
            } else {
                if (System.currentTimeMillis() > c.getLastTry() + c.getMessage().getResendTimeout()) {
                    try {
                        server.resendMessage(c.getMessage(), c.getRecipientAddress(), c.getRecipientPort());
                    } catch(IOException exc) {
                        throw new RuntimeException(exc);
                    }
                    c.tried();
                } else {
                    queue.add(c);
                }
            }
        }
    }
    
    private Certification getNext() {
    	if (queue.size() > 0) {
    		Certification c = (Certification)queue.get(0);
    		queue.remove(0);
    		return c;
    	}
    	return null;
    }
    
    public int size() {
        return queue.size();
    }
    
    private boolean isCertified(long id) {
    	return certified.contains(new Long(id));
    }
}
