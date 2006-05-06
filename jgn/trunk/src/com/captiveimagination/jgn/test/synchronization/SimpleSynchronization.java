package com.captiveimagination.jgn.test.synchronization;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.client.*;
import com.captiveimagination.jgn.server.*;
import com.captiveimagination.jgn.synchronization.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class SimpleSynchronization extends JFrame implements GraphicsConnector, KeyListener {
	private static final long serialVersionUID = 1L;
	private static final long SERVER_OBJECT = 1;
	private static final long CLIENT_OBJECT = 2;
	
	private JPanel panel;
	private JPanel serverPanel;
	private JPanel clientPanel;
	private long type;
	
	public SimpleSynchronization(long type) {
		if (type == SERVER_OBJECT) {
			setTitle("Server");
		} else {
			setTitle("Client");
		}
		setSize(400, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addKeyListener(this);
		
		panel = new JPanel();
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		c.add(BorderLayout.CENTER, panel);
		
		panel.setLayout(null);
		panel.addKeyListener(this);
		
		serverPanel = new JPanel();
		serverPanel.addKeyListener(this);
		serverPanel.setBounds(0, 0, 50, 50);
		serverPanel.setBackground(Color.BLUE);
		
		clientPanel = new JPanel();
		clientPanel.addKeyListener(this);
		clientPanel.setBounds(300, 300, 50, 50);
		clientPanel.setBackground(Color.RED);
		panel.add(serverPanel);
		panel.add(clientPanel);
		
		this.type = type;
	}
	
	public void keyTyped(KeyEvent e) {
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
		int x = 0;
		int y = 0;
		if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			x -= 5;
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			x += 5;
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {
			y -= 5;
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			y += 5;
		}
		
		if (type == SERVER_OBJECT) {
			serverPanel.setBounds(serverPanel.getX() + x, serverPanel.getY() + y, 50, 50);
		} else {
			clientPanel.setBounds(clientPanel.getX() + x, clientPanel.getY() + y, 50, 50);
		}
	}

	public boolean validateSynchronizationMessage(PositionSynchronizationMessage message) {
		return true;
	}

	public void applySynchronizationMessage(PositionSynchronizationMessage message) {
		if (message.getObjectId() == SERVER_OBJECT) {
			serverPanel.setBounds((int)message.getPositionX(), (int)message.getPositionY(), 50, 50);
		} else {
			clientPanel.setBounds((int)message.getPositionX(), (int)message.getPositionY(), 50, 50);
		}
	}

	public PositionSynchronizationMessage createSynchronizationMessage(long objectId) {
		PositionSynchronizationMessage message = new PositionSynchronizationMessage();
		JPanel panel;
		if (objectId == SERVER_OBJECT) {
			panel = serverPanel;
		} else {
			panel = clientPanel;
		}
		message.setObjectId(objectId);
		message.setPositionX(panel.getX());
		message.setPositionY(panel.getY());
		return message;
	}
	
	public static void main(String[] args) throws Exception {
		// First we need to register the PositionSynchronizationMessage
		JGN.registerMessage(PositionSynchronizationMessage.class, (short)1);
		
		// Instantiate the SimpleSynchronization GUI for the server
		SimpleSynchronization ssServer = new SimpleSynchronization(SERVER_OBJECT);
		ssServer.setVisible(true);
		
		// Start the NetworkingServer to handle synchronization for the ssServer
		NetworkingServer server = new NetworkingServer(1000, 2000);
		server.addMessageListener(new PositionSynchronizationListener(ssServer));
		new Thread(server).start();
		
		// Instantiate the SimpleSynchronization GUI for the client
		SimpleSynchronization ssClient = new SimpleSynchronization(CLIENT_OBJECT);
		ssClient.setLocation(410, 0);
		ssClient.setVisible(true);
		
		// Start the NetworkingClient to handle synchronization for the ssClient
		NetworkingClient client = new NetworkingClient(3000, 4000);
		client.addMessageListener(new PositionSynchronizationListener(ssClient));
		new Thread(client).start();
		client.connectAndWait(IP.getLocalHost(), 1000, 2000, 5000);
		
		// serverUpdater handles the sending of messages from the server to the client
		Updater serverUpdater = new Updater(1000);
		serverUpdater.add(new PositionMessageSender(SERVER_OBJECT, 2, ssServer, server, MessageServer.UDP));
		serverUpdater.start();
		
		// clientUpdater handles the sending of messages from the client to the server
		Updater clientUpdater = new Updater(1000);
		clientUpdater.add(new PositionMessageSender(CLIENT_OBJECT, 2, ssClient, client, MessageServer.UDP));
		clientUpdater.start();
	}
}
