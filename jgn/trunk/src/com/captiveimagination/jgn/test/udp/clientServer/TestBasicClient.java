/*
 * Created on Feb 1, 2006
 */
package com.captiveimagination.jgn.test.udp.clientServer;

import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.client.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.player.*;

/**
 * @author Matthew D. Hicks
 */
public class TestBasicClient {
    public static void main(String[] args) throws Exception {
        UDPMessageServer messageServer = new UDPMessageServer(IP.getLocalHost(), 1005);
        NetworkingClient client = new NetworkingClient(messageServer);
        client.addPlayerListener(new PlayerListener() {
            public void createLocalPlayer(PlayerJoinResponseMessage message) {
                System.out.println("C> CreateLocalPlayer " + message.getPlayerId());
            }

            public void createLocalPlayerDenied() {
                System.out.println("C> CreateLocalPlayerDenied");
            }

            public void removeLocalPlayer(short playerId) {
                System.out.println("C> RemoveLocalPlayer " + playerId);
            }

            public void createRemotePlayer(PlayerJoinRequestMessage message) {
                System.out.println("C> CreateRemotePlayer " + message.getPlayerId());
            }

            public void removeRemotePlayer(PlayerDisconnectMessage message) {
                System.out.println("C> RemoveRemotePlayer " + message.getPlayerId());
            }
        });
        Thread t = new Thread(client);
        t.start();
        System.out.println("Connected: " + client.connectAndWait(IP.getLocalHost(), 1000, -1, 10000));
        System.out.println("Player ID: " + client.getPlayerId());
    }
}
