/*
 * Created on Feb 11, 2006
 */
package com.captiveimagination.jgn.test.tcp.certified;

import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

/**
 * This is a test to verify that CertifiedMessage will still send properly over
 * TCP even though it serves no purpose beyond that of a Message on TCP.
 * 
 * @author Matthew D. Hicks
 */
public class TestCertified {
    public static void main(String[] args) throws Exception {
        JGN.registerMessage(BasicCertifiedMessage.class, (short)1);
        
        TCPMessageServer server1 = new TCPMessageServer(null, 1000);
        server1.startUpdateThread();
        server1.addMessageListener(new MessageListener() {
            public void messageReceived(Message message) {
                System.out.println("Server1> Received message " + message.getClass().getName());
            }

            public int getListenerMode() {
                return MessageListener.BASIC;
            }
        });
        
        TCPMessageServer server2 = new TCPMessageServer(null, 1005);
        server2.startUpdateThread();
        server2.addMessageListener(new MessageListener() {
            public void messageReceived(Message message) {
                System.out.println("Server2> Received message " + message.getClass().getName());
            }

            public int getListenerMode() {
                return MessageListener.BASIC;
            }
        });
        
        BasicCertifiedMessage message = new BasicCertifiedMessage();
        server2.sendMessage(message, IP.getLocalHost(), 1000);
    }
}
