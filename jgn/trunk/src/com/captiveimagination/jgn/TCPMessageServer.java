/*
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
 */
package com.captiveimagination.jgn;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import com.captiveimagination.jgn.compression.InvalidCompressionMethodException;
import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 * 
 */
public class TCPMessageServer extends MessageServer {
	public static int CAPACITY = 512 * 1000;

	private byte[] buf;

	private ArrayList connections;

	private HashMap connectionsMap;

	private MessageExtractor extractor = new MessageExtractor(this);

	private ByteBuffer receiveBuffer;

	private ByteBuffer sendBuffer;

	private ServerSocketChannel server;

	public TCPMessageServer(InetSocketAddress address) throws IOException {
		this(IP.fromInetAddress(address.getAddress()), address.getPort());
	}

	public TCPMessageServer(IP address, int port) throws IOException {
		super(address, port);

		init();
	}

	protected void closeChannel() {
		try {
			server.close();
		} catch (Exception exc) {
		}
	}

	public void disconnect(IP remoteAddress, int remotePort) {
		try {
			SocketChannel channel = null;
			if (remoteAddress != null) {
				channel = (SocketChannel) connectionsMap.get(remoteAddress
						.toString()
						+ ":" + remotePort);
			}
			if (channel != null) {
				channel.close();
				connections.remove(channel);
				connectionsMap.values().remove(channel);
			}
		} catch (IOException exc) {
			exc.printStackTrace();
		}
	}

	private SocketChannel getConnection(IP remoteAddress, int remotePort)
			throws IOException {
		SocketChannel channel = (SocketChannel) connectionsMap
				.get(remoteAddress.toString() + ":" + remotePort);
		if (channel == null) {
			System.out.println("Establishing new connection to: "
					+ remoteAddress + ":" + remotePort);
			// Create a new connection
			channel = SocketChannel.open(new InetSocketAddress(remoteAddress
					.toString(), remotePort));
			channel.configureBlocking(false);
			connectionsMap.put(remoteAddress.toString() + ":" + remotePort,
					channel);
			connections.add(channel);
		}
		return channel;
	}

	private void init() throws IOException {
		connectionsMap = new HashMap();
		connections = new ArrayList();
		receiveBuffer = ByteBuffer.allocate(CAPACITY);
		sendBuffer = ByteBuffer.allocate(CAPACITY);
		buf = new byte[CAPACITY];

		server = ServerSocketChannel.open();
		server.configureBlocking(false);
		InetAddress addr = null;
		if (getAddress() != null) {
			addr = InetAddress.getByAddress(getAddress().getBytes());
		}
		server.socket().bind(new InetSocketAddress(addr, getPort()));
	}

	protected synchronized Message receiveMessage() throws IOException {
		SocketChannel channel;
		if (extractor.available() == 0) {
			/*
			 * read data from network
			 */
			for (int i = 0; i < connections.size(); i++) {
				channel = (SocketChannel) connections.get(i);
				try {
					if (channel.read(receiveBuffer) > 0) {
						int len = receiveBuffer.position();
						receiveBuffer.rewind();
						receiveBuffer.get(buf, 0, len);
						SocketDescriptor current = new SocketDescriptor(IP
								.fromInetAddress(channel.socket()
										.getInetAddress()), channel.socket()
								.getPort());
						this.extractor.appendData(buf, 0, len, current);
						receiveBuffer.clear();
					}
				} catch (ClosedChannelException exc) {
					if (isKeepAlive()) {
						exc.printStackTrace();
						System.err.println("Closing connection "
								+ channel.socket().getInetAddress()
										.getHostAddress() + ":"
								+ channel.socket().getPort());
						disconnect(IP.fromInetAddress(channel.socket()
								.getInetAddress()), channel.socket().getPort());
					}
				} catch (Exception exc) {
					if ((exc.getMessage() != null)
							&& (exc.getMessage()
									.equals("An existing connection was forcibly closed by the remote host"))) {
						System.err.println("Connection terminated abnormally.");
					} else {
						exc.printStackTrace();
					}
					System.err.println("Closing connection "
							+ channel.socket().getInetAddress()
									.getHostAddress() + ":"
							+ channel.socket().getPort());
					disconnect(IP.fromInetAddress(channel.socket()
							.getInetAddress()), channel.socket().getPort());
				}
			}
			try {
				extractor.updateAvailableMessages();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return extractor.nextMessage();
	}

	public void sendMessage(Message message, IP remoteAddress, int remotePort)
			throws IOException {
		if (remotePort == -1) {
			throw new RuntimeException(
					"Cannot send message when not connected to a server.");
		}
		if (remoteAddress == null)
			remoteAddress = IP.getLocalHost();
		try {
			// Assign unique id
			message.setId(JGN.getUniqueLong());
			// Assign send time
			message.setSentStamp(getConvertedTime(remoteAddress, remotePort));
			// Assign this server to it
			message.setMessageServer(this);

			if (message instanceof OrderedMessage) {
				if (((OrderedMessage) message).getOrderId() == -1) {
					((OrderedMessage) message).setOrderId(OrderedMessage
							.createUniqueId(((OrderedMessage) message)
									.getOrderGroup()));
				}
			}
			sendBuffer.clear();
			sendBuffer.put(extractor.convertMessage(message));
			sendBuffer.flip();
			SocketChannel channel = getConnection(remoteAddress, remotePort);
			channel.write(sendBuffer);
			message.setRemoteAddress(remoteAddress);
			message.setRemotePort(remotePort);
			messageSent(message);
		} catch (IllegalAccessException exc) {
			throw new RuntimeException(exc);
		} catch (IllegalArgumentException exc) {
			throw new RuntimeException(exc);
		} catch (InvocationTargetException exc) {
			throw new RuntimeException(exc);
		}
	}

	public synchronized void updateIncoming() {
		// Accept new incoming connections
		SocketChannel channel;
		try {
			while ((channel = server.accept()) != null) {
				/*
				 * Check to see if the connection is already established. If so,
				 * we need to close the old connection before establishing the
				 * new one
				 */
				if (connectionsMap.get(channel.socket().getInetAddress()
						.getHostAddress()
						+ ":" + channel.socket().getPort()) != null) {
					SocketChannel temp = (SocketChannel) connectionsMap
							.get(channel.socket().getInetAddress()
									.getHostAddress()
									+ ":" + channel.socket().getPort());
					try {
						temp.close();
					} catch (Throwable t) {
					}
					connections.remove(temp);
				}
				channel.configureBlocking(false);
				connectionsMap.put(channel.socket().getInetAddress()
						.getHostAddress()
						+ ":" + channel.socket().getPort(), channel);
				System.out.println("Putting incoming connection: "
						+ channel.socket().getInetAddress().getHostAddress()
						+ ":" + channel.socket().getPort());
				connections.add(channel);
			}
		} catch (ClosedChannelException exc) {
			if (isKeepAlive()) {
				exc.printStackTrace();
			}
		} catch (IOException exc) {
			exc.printStackTrace();
		}

		super.updateIncoming();
	}
}
