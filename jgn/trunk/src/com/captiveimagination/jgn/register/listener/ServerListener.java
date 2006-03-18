/*
 * Created on Jan 23, 2006
 */
package com.captiveimagination.jgn.register.listener;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.register.*;
import com.captiveimagination.jgn.register.message.*;

/**
 * @author Matthew D. Hicks
 */
public class ServerListener implements MessageListener {
    private ServerRegister register;
    
    public ServerListener(ServerRegister register) {
        this.register = register;
    }
    
    public void messageReceived(Message message) {
    }
    
    public void messageReceived(RegisterServerMessage message) {
        Server server = new Server();
        server.setAddress(message.getAddress());
        server.setGame(message.getGame());
        server.setHost(message.getHost());
        server.setInfo(message.getInfo());
        server.setMap(message.getMap());
        server.setPortUDP(message.getPortUDP());
        server.setPortTCP(message.getPortTCP());
        server.setServerName(message.getServerName());
        server.setPlayers(message.getMaxPlayers());
        server.setMaxPlayers(message.getMaxPlayers());
        if (!register.addServer(server)) {
            System.out.println("Server registered: " + server.getServerName() + " (" + register.getServers().length + " total servers registered).");
        }
    }
    
    public void messageReceived(UnregisterServerMessage message) {
        register.removeServer(message.getAddress(), message.getPortUDP(), message.getPortTCP());
        System.out.println("Server unregistered: " + message.getAddress() + ":" + message.getPortUDP() + ":" + message.getPortTCP() + " (" + register.getServers().length + " total servers registered).");
    }

    public int getListenerMode() {
        return MessageListener.CLOSEST;
    }
}
