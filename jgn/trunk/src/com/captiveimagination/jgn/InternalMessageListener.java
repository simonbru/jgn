package com.captiveimagination.jgn;

import java.io.*;
import java.util.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class InternalMessageListener implements MessageListener {
	private MessageServer messageServer;
	
	private long pingTime;
	private long remoteTime;
	private HashSet certifyQueue;
	
	public InternalMessageListener(MessageServer messageServer) {
		this.messageServer = messageServer;
		pingTime = -1;
		remoteTime = -1;
		certifyQueue = new HashSet();
	}
	
	public void messageReceived(Message message) {
        if ((message instanceof Receipt) && (messageServer instanceof UDPMessageServer)) {
            Receipt m = (Receipt)message;
            ((UDPMessageServer)messageServer).getMessageCertifier().certify(m.getCertifiedId());
            certifyQueue.remove(new Long(m.getCertifiedId()));
        } else if (message instanceof PingMessage) {
            try {
                messageServer.sendMessage(new PongMessage(), message.getRemoteAddress(), message.getRemotePort());
            } catch(IOException exc) {
                throw new RuntimeException(exc);
            }
        } else if (message instanceof PongMessage) {
        	pingTime = System.nanoTime();
        } else if (message instanceof TimeSyncMessage) {
        	TimeSyncMessage m = (TimeSyncMessage)message;
        	long conversion = m.getLocalTime() - System.currentTimeMillis();
    		messageServer.setTimeConversion(m.getRemoteAddress(), m.getRemotePort(), conversion);
        	if (m.getRemoteTime() > 0) {
        		// TODO do I need to do anything here?
        	} else {
        		// Return sync
        		m.setRemoteTime(m.getLocalTime());
        		m.setLocalTime(System.currentTimeMillis());
        		try {
                    messageServer.sendMessage(m, message.getRemoteAddress(), message.getRemotePort());
                } catch(IOException exc) {
                    throw new RuntimeException(exc);
                }
        	}
        }
    }

    public int getListenerMode() {
        return MessageListener.BASIC;
    }
    
    public synchronized long pingAndWait(IP remoteAddress, int remotePort, long timeout) {
    	pingTime = -1;
    	long maxWait = System.currentTimeMillis() + timeout;
    	PingMessage message = new PingMessage();
    	long time = System.nanoTime();
        try {
            messageServer.sendMessage(message, remoteAddress, remotePort);
        } catch(IOException exc) {
            throw new RuntimeException(exc);
        }
    	while (pingTime == -1) {
    		try {
    			Thread.sleep(10);
    		} catch(InterruptedException exc) {
    			exc.printStackTrace();
    		}
    		if (System.currentTimeMillis() > maxWait) return -1;
    	}
    	return pingTime - time;
    }
    
    public synchronized void timeSync(IP remoteAddress, int remotePort) {
    	TimeSyncMessage message = new TimeSyncMessage();
    	message.setLocalTime(System.currentTimeMillis());
    	try {
    		messageServer.sendMessage(message, remoteAddress, remotePort);
    	} catch(IOException exc) {
    		throw new RuntimeException(exc);
    	}
    }
    
    public boolean sendCertified(CertifiedMessage message, IP remoteAddress, int remotePort, long timeout) {
    	if (messageServer instanceof UDPMessageServer) {
    		try {
                messageServer.sendMessage(message, remoteAddress, remotePort);
            } catch(IOException exc) {
                throw new RuntimeException(exc);
            }
        	Long l = new Long(message.getId());
        	certifyQueue.add(l);
        	long time = System.currentTimeMillis();
        	while (certifyQueue.contains(l)) {
        		try {
        			Thread.sleep(10);
        		} catch(InterruptedException exc) {
        			exc.printStackTrace();
        		}
        		if (System.currentTimeMillis() - time > timeout) {
        			return false;
        		}
        	}
        	return true;
    	}
		return true;
    }
}
