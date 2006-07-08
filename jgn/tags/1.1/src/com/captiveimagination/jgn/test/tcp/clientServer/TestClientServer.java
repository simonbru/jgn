/*
 * Created on Feb 11, 2006
 */
package com.captiveimagination.jgn.test.tcp.clientServer;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.client.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.player.*;
import com.captiveimagination.jgn.server.*;

/**
 * @author Matthew D. Hicks
 */
public class TestClientServer {
    public static void main(String[] args) throws Exception {
        TCPMessageServer server1 = new TCPMessageServer(null, 1000);
        NetworkingServer server = new NetworkingServer(server1);
        server.addPlayerListener(new PlayerListener() {
            public void createLocalPlayer(PlayerJoinResponseMessage message) {
                System.out.println("S> createLocalPlayer " + message.getPlayerId());
            }

            public void createLocalPlayerDenied() {
                System.out.println("S> createLocalPlayerDenied");
            }

            public void removeLocalPlayer(short playerId) {
                System.out.println("S> removeLocalPlayer " + playerId);
            }

            public void createRemotePlayer(PlayerJoinRequestMessage message) {
                System.out.println("S> createRemotePlayer " + message.getPlayerId());
            }

            public void removeRemotePlayer(PlayerDisconnectMessage message) {
                System.out.println("S> removeRemotePlayer " + message.getPlayerId());
            }
        });
        new Thread(server).start();
        
        TCPMessageServer server2 = new TCPMessageServer(null, 1005);
        NetworkingClient client = new NetworkingClient(server2);
        client.addPlayerListener(new PlayerListener() {
            public void createLocalPlayer(PlayerJoinResponseMessage message) {
                System.out.println("C> createLocalPlayer " + message.getPlayerId());
            }

            public void createLocalPlayerDenied() {
                System.out.println("C> createLocalPlayerDenied");
            }

            public void removeLocalPlayer(short playerId) {
                System.out.println("C> removeLocalPlayer " + playerId);
            }

            public void createRemotePlayer(PlayerJoinRequestMessage message) {
                System.out.println("C> createRemotePlayer " + message.getPlayerId());
            }

            public void removeRemotePlayer(PlayerDisconnectMessage message) {
                System.out.println("C> removeRemotePlayer " + message.getPlayerId());
            }
        });
        client.getPlayerMessageServer().addMessageSentListener(new MessageSentListener() {
            public void messageSent(Message message) {
                System.out.println("C> Message sent: " + message.getClass().getName());
            }

            public int getListenerMode() {
                return MessageSentListener.BASIC;
            }
        });
        new Thread(client).start();
        
        System.out.println("Connected: " + client.connectAndWait(IP.getLocalHost(), -1, 1000, 10000));
        System.out.println("Player ID: " + client.getPlayerId());
    }
}
