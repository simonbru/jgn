/*
 * Created on Jan 23, 2006
 */
package com.captiveimagination.jgn.register;

import java.io.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.register.message.*;

/**
 * Sends messages to the registry to update the status of this server.
 * 
 * @author Matthew D. Hicks
 */
public class RegisterServerMessageSender implements MessageSender {
    private MessageServer messageServer;
    private Server server;
    private IP address;
    private int port;
    private boolean enabled;
    
    public RegisterServerMessageSender(MessageServer messageServer, Server server, IP address, int port) {
        this.messageServer = messageServer;
        this.server = server;
        this.address = address;
        this.port = port;
        enabled = true;
        
        ServerRegister.registerMessages();
    }
    
    public int getUpdatesPerCycle() {
        return 1;
    }

    public void sendMessage() {
        RegisterServerMessage message = new RegisterServerMessage();
        message.setAddress(server.getAddress());
        message.setGame(server.getGame());
        message.setHost(server.getHost());
        message.setInfo(server.getInfo());
        message.setMap(server.getMap());
        message.setPortUDP(server.getPortUDP());
        message.setPortTCP(server.getPortTCP());
        message.setServerName(server.getServerName());
        try {
            messageServer.sendMessage(message, address, port);
        } catch(IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public void enable() {
        enabled = true;
    }
    
    public void disable() {
        enabled = false;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public static final Updater createUpdaterAndServerRegistrationSender(MessageServer messageServer, Server server, IP registryAddress, int registryPort) {
        Updater updater = new Updater(15 * 1000);
        RegisterServerMessageSender sender = new RegisterServerMessageSender(messageServer, server, registryAddress, registryPort);
        updater.add(sender);
        updater.start();
        return updater;
    }
}
