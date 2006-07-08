/*
 * Created on Nov 30, 2005
 */
package com.captiveimagination.jgn.test.udp.basic;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class TestBasic {
    public static void main(String[] args) throws Exception {
        // All messages must be registered before they
        // can be sent or received
        JGN.registerMessage(BasicMessage.class, (short)1);
        
        // The MessageServer listens for incoming messages
        final MessageServer server = new UDPMessageServer(null, 9000);
        
        // The MessageServer that sends the message
        final MessageServer server2 = new UDPMessageServer(IP.getLocalHost(), 9001);
        server2.startUpdateThread();
        
        // Add a listener to let us know the message was received
        server.addMessageListener(new MessageListener() {
            public void messageReceived(Message message) {
            }
            
            public void messageReceived(BasicMessage message) {
                System.out.println("Received Message: " + message.getText());
                if (message.getNumbers() != null) {
                    int[] numbers = message.getNumbers();
                    for (int i = 0; i < numbers.length; i++) {
                        System.out.print(numbers[i] + " ");
                    }
                } else {
                    System.out.println("Numbers is null!");
                }
                // Lets shutdown the server now
                server.shutdown();
                server2.shutdown();
            }

            public int getListenerMode() {
                return MessageListener.CLOSEST;
            }
        });
        // We start the update thread - this is an alternative to calling update() in your game thread
        server.startUpdateThread();
        
        // Send a message to the server for processing
        BasicMessage message = new BasicMessage();
        message.setText("Hello Server!");
        message.setNumbers(new int[] {1, 2, 3, 4, 5});
        server2.sendMessage(message, null, 9000);
    }
}
