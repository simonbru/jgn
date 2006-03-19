package com.captiveimagination.jgn.client;

import java.io.*;
import java.util.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.player.*;

/**
 * NetworkingClient is a convenience class for clients connecting into a
 * client/server architecture.
 * 
 * @author Matthew D. Hicks
 */
public class NetworkingClient implements Runnable {
	/**
	 * The time in milliseconds to wait before sending a noop
	 * if no other messages are transmitted to the server.
	 * This defaults to 9000 (9 seconds).
	 */
	public static long NOOP_DELAY = 9 * 1000;
	
	private UDPMessageServer messageServerUDP;
    private TCPMessageServer messageServerTCP;
    
	private ClientSession clientSession;
	private ClientPlayerMessageListener playerMessageListener;
	
	private IP serverAddress;
	private int serverPortUDP;
    private int serverPortTCP;
	
	private short playerId;
    private ArrayList playerListeners;
	
	private long lastNoopUDP;
	private long lastNoopTCP;
	private boolean keepAlive;
	
	public NetworkingClient(int udpPort, int tcpPort) throws IOException {
		UDPMessageServer serverUDP = null;
		TCPMessageServer serverTCP = null;
		if (udpPort > -1) {
			serverUDP = new UDPMessageServer(null, udpPort);
		}
		if (tcpPort > -1) {
			serverTCP = new TCPMessageServer(null, tcpPort);
		}
		init(serverUDP, serverTCP, null);
	}
	
    public NetworkingClient(UDPMessageServer messageServer) {
        this(messageServer, null);
    }
    
    public NetworkingClient(TCPMessageServer messageServer) {
        this(null, messageServer);
    }
    
    public NetworkingClient(UDPMessageServer messageServerUDP, TCPMessageServer messageServerTCP) {
		this(messageServerUDP, messageServerTCP, null);
	}
	
	public NetworkingClient(UDPMessageServer messageServerUDP, TCPMessageServer messageServerTCP, ClientSession clientSession) {
		init(messageServerUDP, messageServerTCP, clientSession);
	}
	
	private void init(UDPMessageServer messageServerUDP, TCPMessageServer messageServerTCP, ClientSession clientSession) {
		if (clientSession == null) clientSession = new DefaultClientSession();
		
		this.messageServerUDP = messageServerUDP;
        this.messageServerTCP = messageServerTCP;
		this.clientSession = clientSession;
		playerId = -1;
		
		// Register Necessary Player Messages
		JGN.registerMessage(PlayerJoinRequestMessage.class, (short)(Short.MIN_VALUE + 1));
		JGN.registerMessage(PlayerJoinResponseMessage.class, (short)(Short.MIN_VALUE + 2));
		JGN.registerMessage(PlayerDisconnectMessage.class, (short)(Short.MIN_VALUE + 3));
		JGN.registerMessage(ServerDisconnectMessage.class, (short)(Short.MIN_VALUE + 4));
		JGN.registerMessage(PlayerNoopMessage.class, (short)(Short.MIN_VALUE + 5));
		
		// Add Message Listeners
		playerMessageListener = new ClientPlayerMessageListener(this);
        if (messageServerTCP != null) {
            messageServerTCP.addMessageListener(playerMessageListener);
        }
        if (messageServerUDP != null) {
            messageServerUDP.addMessageListener(playerMessageListener);
        }
        
        playerListeners = new ArrayList();
	}
	
	public void update() {
        updateIncoming();
        
        updateEvents();
		
		updateNoop();
	}
	
	public void updateIncoming() {
		if (messageServerTCP != null) {
			messageServerTCP.updateIncoming();
		}
        if (messageServerUDP != null) {
        	messageServerUDP.updateIncoming();
        }
	}
	
	public void updateEvents() {
		if (messageServerTCP != null) {
			messageServerTCP.updateEvents();
		}
        if (messageServerUDP != null) {
        	messageServerUDP.updateEvents();
        }
	}
	
	public void updateNoop() {
        //System.out.println("Port: " + serverPortUDP + ", " + getUDPMessageServer() + ", " + isConnected() + ", " + lastNoopUDP);
		if ((serverPortUDP > -1) && (getUDPMessageServer() != null) && (isConnected()) && (lastNoopUDP + NOOP_DELAY < System.currentTimeMillis())) {
			PlayerNoopMessage message = new PlayerNoopMessage();
			message.setPlayerId(playerId);
            try {
                if (getUDPMessageServer() != null) getUDPMessageServer().sendMessage(message, serverAddress, serverPortUDP);
            } catch(IOException exc) {
                try {
                    disconnect();
                } catch(IOException exc2) {
                }
            }
			lastNoopUDP = System.currentTimeMillis();
		}
		if ((serverPortTCP > -1) && (getTCPMessageServer() != null) && (isConnected()) && (lastNoopTCP + NOOP_DELAY < System.currentTimeMillis())) {
			PlayerNoopMessage message = new PlayerNoopMessage();
			message.setPlayerId(playerId);
            try {
                if (getTCPMessageServer() != null) getTCPMessageServer().sendMessage(message, serverAddress, serverPortTCP);
            } catch(IOException exc) {
                try {
                    disconnect();
                } catch(IOException exc2) {
                }
            }
			lastNoopTCP = System.currentTimeMillis();
		}
	}
	
	public void run() {
		keepAlive = true;
		while (keepAlive) {
			try {
				Thread.sleep(1);
			} catch(InterruptedException exc) {
			}
			update();
		}
	}
	
	/**
	 * Attempts to establish a connection to the server.
	 * This will send and immediately return, if you want
	 * to wait for a response use connectAndWait(). The
     * serverPortUDP or serverPortTCP may be -1 if that
     * protocol is not available on the server or you wish
     * only to use one type.
	 * @throws IOException 
	 */
	public void connect(IP serverAddress, int serverPortUDP, int serverPortTCP) throws IOException {
		this.serverAddress = serverAddress;
		this.serverPortUDP = serverPortUDP;
        this.serverPortTCP = serverPortTCP;
		
		PlayerJoinRequestMessage request = clientSession.createJoinRequest();
        request.setPortTCP(-1);
        request.setPortUDP(-1);
        if (messageServerTCP != null) {
            request.setPortTCP(messageServerTCP.getPort());
        }
        if (messageServerUDP != null) {
            request.setPortUDP(messageServerUDP.getPort());
        }
		sendToServer(request);
	}
	
	/**
	 * Calls connect() and then waits for a response
	 * from the server for <code>maxWait</code> amount
	 * of time.
	 * 
	 * @return
	 * 		boolean if connection was successful
	 * @throws IOException 
	 */
	public boolean connectAndWait(IP serverAddress, int serverPortUDP, int serverPortTCP, long maxWait) throws InterruptedException, IOException {
		connect(serverAddress, serverPortUDP, serverPortTCP);
		
		maxWait = System.nanoTime() + (maxWait * 1000000);
		while (playerId == -1) {
			update();    // just in case the update thread is being blocked
			try {
				Thread.sleep(10);
			} catch(InterruptedException exc) {
				exc.printStackTrace();
			}
			if (System.nanoTime() > maxWait) break;
		}
		if (playerId == -1) {
			return false;
		}
		return true;
	}
	
	/**
	 * This method is called when this client receives a PlayerJoinResponseMessage.
	 * This method assigns the playerId then calls all PlayerListeners.
	 * 
	 * @param message
	 */
	public void connectResponse(PlayerJoinResponseMessage message) {
		if (message.isAccepted()) {
			playerId = message.getPlayerId();
		}
        
        // Send initial noops to verify connectivity on both
        PlayerNoopMessage nm = new PlayerNoopMessage();
        if ((serverPortTCP > -1) && (messageServerTCP != null)) {
            try {
                sendToServerTCP(nm);
            } catch (IOException exc) {
                System.err.println("Error sending response message to server via TCP.");
            }
        }
        if ((serverPortUDP > -1) && (messageServerUDP != null)) {
            try {
                sendToServerUDP(nm);
            } catch(IOException exc) {
                System.err.println("Error sending response message to server via UDP.");
            }
        }
        
        
        PlayerListener listener;
        for (int i = 0; i < playerListeners.size(); i++) {
            listener = (PlayerListener)playerListeners.get(i);
            if (message.isAccepted()) {
                listener.createLocalPlayer(message);
            } else {
                listener.createLocalPlayerDenied();
            }
        }
	}
	
    /**
     * This method is called when this client receives a PlayerJoinRequestMessage
     * from the server. This calls all listeners' createRemotePlayer method
     * 
     * @param message
     */
    public void connectRequest(PlayerJoinRequestMessage message) {
        PlayerListener listener;
        for (int i = 0; i < playerListeners.size(); i++) {
            listener = (PlayerListener)playerListeners.get(i);
            listener.createRemotePlayer(message);
        }
    }
    
	public void playerDisconnect(PlayerDisconnectMessage message) {
        PlayerListener listener;
        for (int i = 0; i < playerListeners.size(); i++) {
            listener = (PlayerListener)playerListeners.get(i);
            listener.removeRemotePlayer(message);
        }
    }
	
	/**
	 * Sends a Message to the server if currently connected to a server.
	 * 
	 * @param message
	 * @throws IOException
	 */
	public void sendToServer(Message message) throws IOException {
        if (getPlayerMessageServer() instanceof TCPMessageServer) {
        	sendToServerTCP(message);
        } else {
        	sendToServerUDP(message);
        }
	}
    
    /**
     * Sends a Message explicitly to the TCPMessageServer associated with
     * this client.
     * 
     * @param message
     * @throws IOException 
     */
    public void sendToServerTCP(Message message) throws IOException {
        if (serverPortTCP == -1) {
            if (serverPortUDP != -1) {
                sendToServerUDP(message);
                return;
            } else {
                System.err.println("Unable to send TCP message. Not connected to server.");
                return;
            }
        }
        
        if (message instanceof PlayerMessage) {
            ((PlayerMessage)message).setPlayerId(getPlayerId());
        }
        if (messageServerTCP != null) {
            messageServerTCP.sendMessage(message, serverAddress, serverPortTCP);
            if (message instanceof PlayerMessage) lastNoopTCP = System.currentTimeMillis();
        } else {
            throw new IOException("No route to destination server. Differing protocols in use.");
        }
    }
    
    /**
     * Sends a Message explicitly to the UDPMessageServer associated with
     * this client.
     * 
     * @param message
     * @throws IOException 
     */
    public void sendToServerUDP(Message message) throws IOException {
        if (serverPortUDP == -1) {
            if (serverPortTCP != -1) {
                sendToServerTCP(message);
            } else {
                System.err.println("Unable to send UDP message. Not connected to server.");
                return;
            }
        }
        
        if (message instanceof PlayerMessage) {
            ((PlayerMessage)message).setPlayerId(getPlayerId());
        }
        if (messageServerUDP != null) {
            messageServerUDP.sendMessage(message, serverAddress, serverPortUDP);
            if (message instanceof PlayerMessage) lastNoopUDP = System.currentTimeMillis();
        } else {
            throw new IOException("No route to destination server. Differing protocols in use.");
        }
    }
	
	/**
	 * @return
	 * 		true if the client is currently connected to a server.
	 */
	public boolean isConnected() {
		if (playerId != -1) {
			return true;
		}
		return false;
	}
	
    /**
     * @return
     *      The local UDPMessageServer being used to send
     *      and receive messages or null if one is not set.
     */
	public UDPMessageServer getUDPMessageServer() {
		return messageServerUDP;
	}
    
    /**
     * @return
     *      The local TCPMessageServer being used to send
     *      and receive messages or null if one is not set.
     */
    public TCPMessageServer getTCPMessageServer() {
        return messageServerTCP;
    }
    
    /**
     * @return
     *      The local MessageServer being used to send
     *      and receive player messages. This favors
     *      TCPMessageServer if it is set.
     */
    public MessageServer getPlayerMessageServer() {
        if ((messageServerTCP != null) && (serverPortTCP > -1)) {
            return messageServerTCP;
        }
        return messageServerUDP;
    }
	
    /**
     * @return
     *      The port in which player messages are
     *      sent and received.
     */
    public int getPlayerServerPort() {
        if (messageServerTCP != null) {
            return serverPortTCP;
        } else {
            return serverPortUDP;
        }
    }
    
	/**
	 * @return
	 * 		This provides the player's id after a successfull
	 * 		connection has been established to the server. If
	 * 		a connection isn't complete returns -1.
	 */
	public short getPlayerId() {
		return playerId;
	}
	
	/**
	 * Set the ClientSession implementation used for communication with
	 * the server. DefaultClientSession is used if this is not set at
	 * runtime.
	 * 
	 * @param clientSession
	 */
	public void setClientSession(ClientSession clientSession) {
		this.clientSession = clientSession;
	}

    /**
     * Add a PlayerListener to this client
     * 
     * @param listener
     */
    public void addPlayerListener(PlayerListener listener) {
        playerListeners.add(listener);
    }
    
    /**
     * Adds a listener to both message servers.
     * 
     * @param listener
     */
    public void addMessageListener(MessageListener listener) {
        if (messageServerUDP != null) {
            messageServerUDP.addMessageListener(listener);
        }
        if (messageServerTCP != null) {
            messageServerTCP.addMessageListener(listener);
        }
    }
    
    /**
     * Adds a sent listener to both message servers.
     * 
     * @param listener
     */
    public void addMessageSentListener(MessageSentListener listener) {
        if (messageServerUDP != null) {
            messageServerUDP.addMessageSentListener(listener);
        }
        if (messageServerTCP != null) {
            messageServerTCP.addMessageSentListener(listener);
        }
    }
    
    /**
     * Dispatches a message to the server informing it that this client is
     * disconnecting.
     * @throws IOException 
     */
    public void disconnect() throws IOException {
        if (playerId != -1) {
            PlayerDisconnectMessage message = new PlayerDisconnectMessage();
            message.setPlayerId(playerId);
            sendToServer(message);
            if (messageServerTCP != null) {
            	messageServerTCP.disconnect(serverAddress, serverPortTCP);
            }
            serverAddress = null;
            serverPortUDP = -1;
            serverPortTCP = -1;
            playerId = -1;
            PlayerListener listener;
            for (int i = 0; i < playerListeners.size(); i++) {
                listener = (PlayerListener)playerListeners.get(i);
                listener.removeLocalPlayer(playerId);
            }
        }
    }
    
	/**
	 * Shuts down all threads created by this system and disconnects
	 * from the server.
	 * @throws IOException 
	 */
	public void shutdown() throws IOException {
		disconnect();
        if (messageServerTCP != null) {
            messageServerTCP.shutdown();
        }
        if (messageServerUDP != null) {
            messageServerUDP.shutdown();
        }
		keepAlive = false;
	}
}
