/*
 * Created on Feb 10, 2006
 */
package com.captiveimagination.jgn.test.tcp.stress;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.test.*;

/**
 * @author Matthew D. Hicks
 */
public class StressTCPTest {
    private static final int MAX = 10000;
    
    public static void main(String[] args) throws Exception {
        // We first have to register our message
        JGN.registerMessage(BasicMessage.class, (short)1);
        
        // Create our first server to receive a message
        final MessageServer server1 = new TCPMessageServer(IP.getLocalHost(), 1000);
        server1.startUpdateThread();
        
        // Create our second server to send a message
        final MessageServer server2 = new TCPMessageServer(IP.getLocalHost(), 1001);
        server2.startUpdateThread();
        
        // Lets add a listener to the first server so we know when a message is received
        server1.addMessageListener(new MessageListener() {
            private int count = 0;
            private long time;
            
            public void messageReceived(Message message) {
            }
            
            public void messageReceived(BasicMessage message) {
                if (count == 0) {
                    time = System.nanoTime();
                }
                count++;
                if (count >= MAX) {
                    System.out.println("Completed receive of " + MAX + " messages in " + ((System.nanoTime() - time) / 1000000));
                    server1.shutdown();
                    server2.shutdown();
                }
            }

            public int getListenerMode() {
                return MessageListener.CLOSEST;
            }
        });
        
        // Now lets send a message from server2 to server1
        long time = System.nanoTime();
        BasicMessage message = new BasicMessage();
        for (int i = 0; i < MAX; i++) {
            message.setText("Hello Server1: " + i);
            server2.sendMessage(message, IP.getLocalHost(), 1000);
        }
        System.out.println("Took: " + ((System.nanoTime() - time) / 1000000) + "ms to send " + MAX + " messages.");
    }
}
