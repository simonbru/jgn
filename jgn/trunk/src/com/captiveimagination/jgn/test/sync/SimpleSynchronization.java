/**
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created: Jul 29, 2006
 */
package com.captiveimagination.jgn.test.sync;
import java.awt.*;
import java.awt.event.*;
import java.net.*;

import javax.swing.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.clientserver.*;
import com.captiveimagination.jgn.sync.*;
import com.captiveimagination.jgn.sync.swing.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class SimpleSynchronization extends JFrame implements KeyListener {
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
	
	protected JPanel getServerPanel() {
		return serverPanel;
	}
	
	protected JPanel getClientPanel() {
		return clientPanel;
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
	
	public static void main(String[] args) throws Exception {
		// Instantiate the SimpleSynchronization GUI for the server
		SimpleSynchronization ssServer = new SimpleSynchronization(SERVER_OBJECT);
		ssServer.setVisible(true);
		
		// Instantiate an instance of a SwingGraphicalController
		SwingGraphicalController controller = new SwingGraphicalController();
		
		// Start the server
		InetSocketAddress serverReliable = new InetSocketAddress(InetAddress.getLocalHost(), 1000);
		InetSocketAddress serverFast = new InetSocketAddress(InetAddress.getLocalHost(), 2000);
		JGNServer server = new JGNServer(serverReliable, serverFast);
		JGN.createThread(server).start();
		ServerSynchronizer sSynchronizer = new ServerSynchronizer(controller, server);
		sSynchronizer.register((short)0, ssServer.getServerPanel(), 500, 0);
		sSynchronizer.register((short)1, ssServer.getClientPanel());
		JGN.createThread(sSynchronizer).start();
		ServerSynchronizationListener serverListener = new ServerSynchronizationListener(sSynchronizer, false, server);
		server.addMessageListener(serverListener);
		
		// Instantiate the SimpleSynchronization GUI for the client
		SimpleSynchronization ssClient = new SimpleSynchronization(CLIENT_OBJECT);
		ssClient.setLocation(410, 0);
		ssClient.setVisible(true);
		
		// Start the client
		JGNClient client = new JGNClient(new InetSocketAddress(InetAddress.getLocalHost(), 3000), new InetSocketAddress(InetAddress.getLocalHost(), 4000));
		JGN.createThread(client).start();
		client.connectAndWait(serverReliable, serverFast, 5000);
		System.out.println("**** Connected! ****");
		ClientSynchronizer cSynchronizer = new ClientSynchronizer(controller, client);
		cSynchronizer.register((short)0, ssClient.getServerPanel());
		cSynchronizer.register((short)1, ssClient.getClientPanel(), 500, 0);
		JGN.createThread(cSynchronizer).start();
		SynchronizationListener clientListener = new SynchronizationListener(cSynchronizer, false);
		client.addMessageListener(clientListener);
	}
}