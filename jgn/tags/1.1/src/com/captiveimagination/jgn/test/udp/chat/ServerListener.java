/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn.test.udp.chat;

import java.io.*;
import java.util.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

/**
 * Used by the server to send the message to
 * all connected clients.
 * 
 * @author Matthew D. Hicks
 */
public class ServerListener implements MessageListener {
    private MessageServer server;
    private HashMap clients;
    
    public ServerListener(MessageServer server) {
        this.server = server;
        clients = new HashMap();
    }
    
    public void messageReceived(Message message) {
        if (message instanceof ConnectMessage) {
            System.out.println("SERVER> Received Connect Message...");
            ConnectMessage m = (ConnectMessage)message;
            Client c = new Client();
            c.setNickname(m.getNickname());
            c.setAddress(m.getRemoteAddress());
            c.setPort(m.getRemotePort());
            broadcast(c, "Connected.");
            clients.put(m.getRemoteAddress().toString() + ":" + m.getRemotePort(), c);
        } else if (message instanceof BroadcastMessage) {
            System.out.println("SERVER> Received Broadcast Message...");
            BroadcastMessage m = (BroadcastMessage)message;
            Client c = (Client)clients.get(m.getRemoteAddress().toString() + ":" + m.getRemotePort());
            broadcast(c, m.getMessage());
        }
    }
    
    public void broadcast(Client c, String message) {
        Collection coll = clients.values();
        Iterator iterator = coll.iterator();
        BroadcastMessage m = new BroadcastMessage();
        m.setMessage(c.getNickname() + ": " + message);
        while (iterator.hasNext()) {
            Client client = (Client)iterator.next();
            try {
                System.out.println("Sending message to: " + client.getPort());
                try {
                    server.sendMessage(m, client.getAddress(), client.getPort());
                } catch(IOException exc) {
                    throw new RuntimeException(exc);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    public int getListenerMode() {
        return MessageListener.BASIC;
    }
}
