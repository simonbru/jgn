/*
 * Created on Jan 23, 2006
 */
package com.captiveimagination.jgn.register.listener;

import java.io.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.register.*;
import com.captiveimagination.jgn.register.message.*;

/**
 * @author Matthew D. Hicks
 */
public class ClientListener implements MessageListener {
    private ServerRegister register;
    
    public ClientListener(ServerRegister register) {
        this.register = register;
    }
    
    public void messageReceived(Message message) {
    }
    
    public void messageReceived(RequestServersMessage message) {
        Server[] servers = register.getServers();
        ServerStatusMessage ssm;
        for (int i = 0; i < servers.length; i++) {
            ssm = new ServerStatusMessage();
            ssm.setAddress(servers[i].getAddress());
            ssm.setGame(servers[i].getGame());
            ssm.setHost(servers[i].getHost());
            ssm.setInfo(servers[i].getInfo());
            ssm.setMap(servers[i].getMap());
            ssm.setPortUDP(servers[i].getPortUDP());
            ssm.setPortTCP(servers[i].getPortTCP());
            ssm.setServerName(servers[i].getServerName());
            ssm.setPlayers(servers[i].getPlayers());
            ssm.setMaxPlayers(servers[i].getMaxPlayers());
            try {
                register.getMessageServer().sendMessage(ssm, message.getRemoteAddress(), message.getRemotePort());
            } catch(IOException exc) {
                throw new RuntimeException(exc);
            }
        }
        System.out.println("Server list sent to: " + message.getRemoteAddress().toString() + ":" + message.getRemotePort() + " - " + servers.length + " servers sent.");
    }

    public int getListenerMode() {
        return MessageListener.CLOSEST;
    }
}
