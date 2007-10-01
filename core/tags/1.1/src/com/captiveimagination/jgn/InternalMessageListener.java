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

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class InternalMessageListener implements MessageListener {
	private MessageServer messageServer;
	
	private long pingTime;
	//private long remoteTime;
	private HashSet certifyQueue;
	
	public InternalMessageListener(MessageServer messageServer) {
		this.messageServer = messageServer;
		pingTime = -1;
		//remoteTime = -1;
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
        	pingTime = JGN.getNanoTime();
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
    	long time = JGN.getNanoTime();
        try {
            messageServer.sendMessage(message, remoteAddress, remotePort);
        } catch(IOException exc) {
            throw new RuntimeException(exc);
        }
    	while (pingTime == -1) {
    		try {
    			Thread.sleep(1);
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
