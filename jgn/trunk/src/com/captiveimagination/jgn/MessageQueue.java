/*
 * Created on Nov 29, 2005
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
        //while ((m = getNext()) != null) {
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
                // TODO figure out why it stops working when this system out is taken out
                System.out.println("Not in correct order: " + ((OrderedMessage)m).getOrderId() + ", expecting: " + getNextInOrder((OrderedMessage)m));
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
        //while ((m = getNextSent()) != null) {
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
    
    private synchronized long getNextInOrder(OrderedMessage message) {
    	String key = message.getOrderOriginator() + ":" + message.getOrderGroup();
    	Long lastPosition = (Long)ordered.get(key);
    	if (lastPosition != null) {
    		return lastPosition.longValue() + 1;
    	}
    	return -1;
    }
    
    /*private synchronized Message getNext() {
        if (queue.size() > 0) {
            Message m = (Message)queue.get(0);
            queue.remove(0);
            return m;
        }
        return null;
    }
    
    private synchronized Message getNextSent() {
        if (queueSent.size() > 0) {
            Message m = (Message)queueSent.get(0);
            queueSent.remove(0);
            return m;
        }
        return null;
    }*/
    
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
