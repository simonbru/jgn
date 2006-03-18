package com.captiveimagination.jgn.test.register;

import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.client.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.register.*;
import com.captiveimagination.jgn.register.message.*;
import com.captiveimagination.jgn.server.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class TestRegister {
	public static void main(String[] args) throws Exception {
		/*
// Part 1 - The Server Register
		// First we have to start the server register
		UDPMessageServer registerServer = new UDPMessageServer(InetAddress.getLocalHost(), 2626);
		ServerRegister register = new ServerRegister(registerServer);
        register.start();

// Part 2 - The Server
        // Next we create a NetworkingServer to register
        UDPMessageServer messageServerUDP = new UDPMessageServer(InetAddress.getLocalHost(), 1000);
        TCPMessageServer messageServerTCP = new TCPMessageServer(InetAddress.getLocalHost(), 1001);
        NetworkingServer server = new NetworkingServer(messageServerUDP, messageServerTCP);
        new Thread(server).start();
        
        // Now we register the server with with the register
        InetAddress host = InetAddress.getLocalHost();
        Server s = new Server("TestServer", host.getHostName(), host.getAddress(), messageServerUDP.getPort(), messageServerTCP.getPort(), "TestGame", "TestMap", "TestInfo", 0, 16);
        RegisterServerMessageSender.createUpdaterAndServerRegistrationSender(messageServerTCP, s, null, 2626);
        */
// Part 3 - The Client
        // Now we create a NetworkingClient
		ServerRegister.registerMessages();
        UDPMessageServer messageServerUDP2 = new UDPMessageServer(IP.getLocalHost(), 1500);
        TCPMessageServer messageServerTCP2 = new TCPMessageServer(IP.getLocalHost(), 1501);
        NetworkingClient client = new NetworkingClient(messageServerUDP2, messageServerTCP2);
        new Thread(client).start();
        
        // We need to add a message listener to get the response from the register after we send a message
        client.addMessageListener(new MessageListener() {
			public void messageReceived(Message message) {
			}
			
			public void messageReceived(ServerStatusMessage message) {
				System.out.println("C> " + message.getGame() + ", " + message.getHost() + ", " + message.getInfo() + ", " + message.getMap() + ", " + message.getPlayers() + ", " + message.getMaxPlayers() + ", " + message.getPortUDP() + ", " + message.getPortTCP() + ", " + message.getServerName());
			}

			public int getListenerMode() {
				return MessageListener.CLOSEST;
			}
        });
        
        // Now we send a message to the register to get a list of servers
        RequestServersMessage message = new RequestServersMessage();
        message.setFilter("");
        messageServerTCP2.sendMessage(message, IP.fromName("captiveimagination.com"), 9801);
	}
}
