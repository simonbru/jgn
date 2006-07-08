/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn.test.udp.chat;

import com.captiveimagination.jgn.*;

/**
 * @author Matthew D. Hicks
 */
public class TestChat {
    public static void main(String[] args) throws Exception {
        JGN.registerMessage(ConnectMessage.class, (short)1);
        JGN.registerMessage(BroadcastMessage.class, (short)2);
        
        ChatServer server = new ChatServer(9000);
        server.start();
        
        ChatClient client1 = new ChatClient(9000, 9001, "John");
        client1.start();
        
        ChatClient client2 = new ChatClient(9000, 9002, "Jane");
        client2.start();
    }
}
