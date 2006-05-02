package com.captiveimagination.jgn.synchronization;

import java.io.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.client.*;
import com.captiveimagination.jgn.server.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class PositionMessageSender implements MessageSender {
	private long objectId;
	private int frequency;
	private GraphicsConnector connector;
	private NetworkingClient client;
	private NetworkingServer server;
	private int sendType;
	
	public PositionMessageSender(long objectId, int frequency, GraphicsConnector connector, NetworkingClient client, int sendType) {
		this.objectId = objectId;
		this.frequency = frequency;
		this.connector = connector;
		this.client = client;
		this.sendType = sendType;
	}
	
	public PositionMessageSender(long objectId, int frequency, GraphicsConnector connector, NetworkingServer server, int sendType) {
		this.objectId = objectId;
		this.frequency = frequency;
		this.connector = connector;
		this.server = server;
		this.sendType = sendType;
	}
	
	public int getUpdatesPerCycle() {
		return frequency;
	}

	public void sendMessage() {
		try {
			if (client != null) {
				client.sendToServer(connector.createSynchronizationMessage(objectId), sendType);
			} else {
				server.sendToAllClients(connector.createSynchronizationMessage(objectId), sendType);
			}
		} catch(IOException e) {
			e.printStackTrace();
			try {
				if (client != null) client.disconnect();
			} catch(IOException exc) {}
		}
	}

	public boolean isEnabled() {
		return true;
	}
}
