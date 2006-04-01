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
