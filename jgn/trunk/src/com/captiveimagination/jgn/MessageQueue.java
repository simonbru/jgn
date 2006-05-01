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

import java.lang.reflect.*;
import java.util.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

/**
 * MessageQueue receives all incoming messages
 * from the server and processes them and calls
 * the MessageListeners accordingly. This queue
 * is instantiated directly in MessageServer and
 * need not be instantiated by implementers of the
 * API.
 * 
 * @author Matthew D. Hicks
 */
public class MessageQueue {
    private MessageServer server;
    private ArrayList queue;
    private ArrayList listeners;
    private ArrayList queueSent;
    private ArrayList listenersSent;
    private MessageIdentificationCache cache;
    
    private HashMap ordered;
    
    public MessageQueue(MessageServer server) {
        this.server = server;
        
        queue = new ArrayList();
        listeners = new ArrayList();
        queueSent = new ArrayList();
        listenersSent = new ArrayList();
        ordered = new HashMap();
        cache = new MessageIdentificationCache(500);
    }
    
    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }
    
    public void addMessageSentListener(MessageSentListener listener) {
        listenersSent.add(listener);
    }
    
    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }
    
    public void removeMessageSentListener(MessageSentListener listener) {
        listenersSent.remove(listener);
    }
    
    public synchronized void enqueue(Message message) {
        queue.add(message);
    }
    
    public synchronized void enqueueSent(Message message) {
        queueSent.add(message);
    }
    
    public void update() {
        Message m;
        
        // Message Received
        List temp = new ArrayList();
        temp.addAll(queue);
        Iterator iterator = temp.iterator();
        while (iterator.hasNext()) {
        	m = (Message)iterator.next();
        	queue.remove(m);
            if (cache.contains(m.getId())) {
            	if (!(m instanceof CertifiedMessage)) {
            		System.err.println("Received duplicate id, ignoring: " + m.getId() + ", " + m.getClass().getName());
            	}
                continue;
            }
            cache.add(m.getId());
            if ((m instanceof CertifiedMessage) && (!((CertifiedMessage)m).isCertified())) {
                sendCertification((CertifiedMessage)m);
                ((CertifiedMessage)m).setCertified(true);
            }
            if (m instanceof PerishableMessage) {
                if (((PerishableMessage)m).isExpired()) {
                    continue;
                }
            }
            if ((m instanceof OrderedMessage) && (!isCorrectOrder((OrderedMessage)m)) && (server instanceof UDPMessageServer)) {
                enqueue(m);
                cache.remove(m.getId());
                continue;
            }
            for (int i = 0; i < listeners.size(); i++) {
                try {
                    sendMessage((MessageListener)listeners.get(i), m);
                } catch(Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }
        
        // Message Sent
        temp.clear();
        temp.addAll(queueSent);
        iterator = temp.iterator();
        while (iterator.hasNext()) {
        	m = (Message)iterator.next();
        	queueSent.remove(m);
            for (int i = 0; i < listenersSent.size(); i++) {
                try {
                    sendSentMessage((MessageSentListener)listenersSent.get(i), m);
                } catch(Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }
    }
    
    private synchronized boolean isCorrectOrder(OrderedMessage message) {
    	String key = message.getOrderOriginator() + ":" + message.getOrderGroup();
    	Long lastPosition = (Long)ordered.get(key);
    	if (lastPosition != null) {
    		if (message.getOrderId() == lastPosition.longValue() + 1) {
    			ordered.put(key, new Long(message.getOrderId()));
    			return true;
    		}
    	} else if (message.getOrderId() == 0) {
    		ordered.put(key, new Long(message.getOrderId()));
    		return true;
    	}
    	return false;
    }
    
    public int size() {
        return queue.size();
    }
    
    private void sendMessage(MessageListener listener, Message message) {
        if (listener.getListenerMode() == MessageListener.BASIC) {
            listener.messageReceived(message);
        } else if (listener.getListenerMode() == MessageListener.CLOSEST) {
            callMethod(listener, "messageReceived", message, false);
        } else if (listener.getListenerMode() == MessageListener.ALL) {
            callMethod(listener, "messageReceived", message, true);
        } else {
            throw new RuntimeException("Invalid listener mode: " + listener.getListenerMode());
        }
    }
    
    private void sendSentMessage(MessageSentListener listener, Message message) {
        if (listener.getListenerMode() == MessageSentListener.BASIC) {
            listener.messageSent(message);
        } else if (listener.getListenerMode() == MessageSentListener.CLOSEST) {
            callMethod(listener, "messageSent", message, false);
        } else if (listener.getListenerMode() == MessageSentListener.ALL) {
            callMethod(listener, "messageSent", message, true);
        } else {
            throw new RuntimeException("Invalid listener mode: " + listener.getListenerMode());
        }
    }
    
    private static void callMethod(Object o, String methodName, Object var, boolean callAll) {
        try {
            Method[] allMethods = o.getClass().getMethods();
            ArrayList m = new ArrayList();
            for (int i = 0; i < allMethods.length; i++) {
                if ((allMethods[i].getName().equals(methodName)) && (allMethods[i].getParameterTypes().length == 1)) {
                    m.add(allMethods[i]);
                }
            }
            
            // Check to see if an interface is found first
            Class[] interfaces = var.getClass().getInterfaces();
            for (int j = 0; j < interfaces.length; j++) {
            	for (int i = 0; i < m.size(); i++) {
                    if (((Method)m.get(i)).getParameterTypes()[0] == interfaces[i]) {
                        ((Method)m.get(i)).setAccessible(true);
                        ((Method)m.get(i)).invoke(o, new Object[] {var});
                        if (!callAll) return;
                    }
                }
            }
            
            // Check to find the closest extending class
            Class c = var.getClass();
            do {
                for (int i = 0; i < m.size(); i++) {
                    if (((Method)m.get(i)).getParameterTypes()[0] == c) {
                        ((Method)m.get(i)).setAccessible(true);
                        ((Method)m.get(i)).invoke(o, new Object[] {var});
                        if (!callAll) return;
                    }
                }
            } while ((c = c.getSuperclass()) != null);
        } catch(Throwable t) {
            System.err.println("Object: " + o + ", MethodName: " + methodName + ", Var: " + var + ", callAll: " + callAll);
            t.printStackTrace();
        }
    }
    
    private void sendCertification(Message message) {
        if (server instanceof UDPMessageServer) {
            Receipt receipt = new Receipt();
            receipt.setCertifiedId(message.getId());
            try {
            	server.sendMessage(receipt, message.getRemoteAddress(), message.getRemotePort());
            } catch(Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
