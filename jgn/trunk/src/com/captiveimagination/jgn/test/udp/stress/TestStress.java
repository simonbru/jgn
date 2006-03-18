/*
 * Created on Nov 30, 2005
 */
package com.captiveimagination.jgn.test.udp.stress;

import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.util.*;

/**
 * @author Matthew D. Hicks
 */
public class TestStress {
    private static final int MAX = 10000;
    
    public static void main(String[] args) throws Exception {
        // All messages must be registered before they
        // can be sent or received
        JGN.registerMessage(StressMessage.class, (short)1);
        
        // The MessageServer listens for incoming messages
        final MessageServer server = new UDPMessageServer((IP)null, 9000);
        // Add a listener to let us know the message was received
        server.addMessageListener(new MessageListener() {
            private int count = 0;
            private long start;
            
            public void messageReceived(Message message) {
                if (message instanceof StressMessage) {
                    if (count == 0) {
                        start = System.currentTimeMillis();
                    }
                    count++;
                    if (count >= MAX) {
                        System.out.println("Last message received after " + (System.currentTimeMillis() - start));
                        server.shutdown();
                        return;
                    }
                }
            }

            public int getListenerMode() {
                return MessageListener.BASIC;
            }
        });
        // We need to start the server to listen
        //server.start();
        // We start the update thread - this is an alternative to calling update() in your game thread
        server.startUpdateThread();
        
        // Start the server's monitor
        MessageServerMonitor monitor1 = new MessageServerMonitor(server, 100);
        monitor1.start();
        monitor1.createGUI("Stress Server Monitor");
        
        // Send a message to the server for processing
        StressMessage message = new StressMessage();
        long time = System.currentTimeMillis();
        MessageServer server2 = new UDPMessageServer(null, 9001);
        // We start the update thread - this is an alternative to calling update() in your game thread
        server2.startUpdateThread();
        MessageServerMonitor monitor2 = new MessageServerMonitor(server2, 100);
        monitor2.start();
        monitor2.createGUI("Stress Client Monitor");
        for (int i = 0; i < MAX; i++) {
            message.setTextOne("Hello Server1: " +  i);
            message.setTextTwo("Hello Server2: " +  i);
            message.setTextThree("Hello Server3: " +  i);
            message.setTextFour("Hello Server4: " +  i);
            message.setTextFive("Hello Server5: " +  i);
            message.setTextSix("Hello Server6: " +  i);
            message.setTextSeven("Hello Server7: " +  i);
            message.setTextEight("Hello Server8: " +  i);
            message.setTextNine("Hello Server9: " +  i);
            message.setTextTen("Hello Server10: " +  i);
            server2.sendMessage(message, IP.getLocalHost(), 9000);
        }
        server2.shutdown();
        System.out.println("Sent all my messages after: " + (System.currentTimeMillis() - time));
    }
}
