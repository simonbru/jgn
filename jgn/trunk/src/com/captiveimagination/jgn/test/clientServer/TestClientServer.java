/*
 * Created on Feb 11, 2006
 */
package com.captiveimagination.jgn.test.clientServer;

import java.io.*;
import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.client.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.player.*;
import com.captiveimagination.jgn.server.*;
import com.captiveimagination.jgn.test.*;

/**
 * Example using a TCPMessageServer and UDPMessageServer together
 * in a NetworkingClient/NetworkingServer environment
 * 
 * @author Matthew D. Hicks
 */
public class TestClientServer {
    public static void main(String[] args) throws Exception {
        JGN.registerMessage(BasicMessage.class, (short)1);
        
        UDPMessageServer server1a = new UDPMessageServer(null, 9901);
        TCPMessageServer server1b = new TCPMessageServer(null, 9902);
        final NetworkingServer server = new NetworkingServer(server1a, server1b);
        new Thread(server).start();
        
        server.getTCPMessageServer().addMessageListener(new MessageListener() {
            public void messageReceived(Message message) {
            }
            
            public void messageReceived(BasicMessage message) {
                System.out.println("S> Message received TCP: " + message.getText());
            }

            public void messageReceived(PlayerNoopMessage message) {
            	//System.out.println("Received TCP Noop!");
            }
            
            public int getListenerMode() {
                return MessageListener.CLOSEST;
            }
        });
        
        server.getUDPMessageServer().addMessageListener(new MessageListener() {
            public void messageReceived(Message message) {
            }
            
            public void messageReceived(BasicMessage message) {
                System.out.println("S> Message received UDP: " + message.getText());
            }
            
            public void messageReceived(PlayerNoopMessage message) {
            	//System.out.println("Received UDP Noop!");
            }

            public int getListenerMode() {
                return MessageListener.CLOSEST;
            }
        });
        
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
				System.out.println("S> RemoteRemotePlayer " + message.getPlayerId());
			}
        });
        
        UDPMessageServer server2a = new UDPMessageServer(null, 4005);
        TCPMessageServer server2b = new TCPMessageServer(null, 4006);
        final NetworkingClient client = new NetworkingClient(server2a, server2b);;
        new Thread(client).start();
        
        client.getTCPMessageServer().addMessageListener(new MessageListener() {
            public void messageReceived(Message message) {
            }
            
            public void messageReceived(BasicMessage message) {
                System.out.println("C> Message received TCP: " + message.getText());
                if (message.getText().equals("PlayerMessageTestServer")) {
                    try {
                        client.disconnect();
                        client.shutdown();
                    } catch(IOException exc) {
                        exc.printStackTrace();
                    }
                	server.shutdown();
                }
            }

            public int getListenerMode() {
                return MessageListener.CLOSEST;
            }
        });
        
        client.getUDPMessageServer().addMessageListener(new MessageListener() {
            public void messageReceived(Message message) {
            }
            
            public void messageReceived(BasicMessage message) {
                System.out.println("C> Message received UDP: " + message.getText());
            }

            public int getListenerMode() {
                return MessageListener.CLOSEST;
            }
        });
        
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
				System.out.println("C> RemoteRemotePlayer " + message.getPlayerId());
			}
        });
        
        System.out.println("Connected: " + client.connectAndWait(IP.fromName("captiveimagination.com"), 9901, 9902, 10 * 1000));
        
        BasicMessage message = new BasicMessage();
        message.setText("TCPMessageTestClient");
        client.sendToServerTCP(message);
        
        message.setText("UDPMessageTestClient");
        client.sendToServerUDP(message);
        
        message.setText("PlayerMessageTestClient");
        client.sendToServer(message);
        
        /*message.setText("TCPMessageTestServer");
        server.sendToAllClientsTCP(message);
        
        message.setText("UDPMessageTestServer");
        server.sendToAllClientsUDP(message);
        
        message.setText("PlayerMessageTestServer");
        server.sendToAllClients(message);*/
    }
}
