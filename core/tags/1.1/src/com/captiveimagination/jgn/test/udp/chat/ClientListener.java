/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn.test.udp.chat;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class ClientListener implements MessageListener {
    private ChatClient client;
    
    public ClientListener(ChatClient client) {
        this.client = client;
    }
    
    public void messageReceived(Message message) {
        if (message instanceof BroadcastMessage) {
            BroadcastMessage m = (BroadcastMessage)message;
            client.addMessage(m.getMessage());
        }
    }

    public int getListenerMode() {
        return MessageListener.BASIC;
    }
}
