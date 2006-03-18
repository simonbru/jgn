/*
 * Created on Feb 1, 2006
 */
package com.captiveimagination.jgn.test.udp.clientServer;

import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.player.*;
import com.captiveimagination.jgn.server.*;

/**
 * @author Matthew D. Hicks
 */
public class TestBasicServer {
    public static void main(String[] args) throws Exception {
        UDPMessageServer messageServer = new UDPMessageServer(IP.getLocalHost(), 1000);
        NetworkingServer server = new NetworkingServer(messageServer);
        server.addPlayerListener(new PlayerListener() {
            public void createLocalPlayer(PlayerJoinResponseMessage message) {
                System.out.println("S> CreateLocalPlayer " + message.getPlayerId());
            }

            public void createLocalPlayerDenied() {
                System.out.println("S> CreateLocalPlayerDenied");
            }

            public void removeLocalPlayer(short playerId) {
                System.out.println("S> RemoveLocalPlayer " + playerId);
            }

            public void createRemotePlayer(PlayerJoinRequestMessage message) {
                System.out.println("S> CreateRemotePlayer " + message.getPlayerId());
            }

            public void removeRemotePlayer(PlayerDisconnectMessage message) {
                System.out.println("S> RemoveRemotePlayer " + message.getPlayerId());
            }            
        });
        Thread t = new Thread(server);
        t.start();
    }
}
