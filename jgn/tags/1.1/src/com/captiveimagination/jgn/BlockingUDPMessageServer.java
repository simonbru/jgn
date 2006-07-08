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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.captiveimagination.jgn.message.CertifiedMessage;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.OrderedMessage;

/**
 * MessageServer is one of the main foundational classes that make up this API
 * and is responsible for listening on a specific address and port for incoming
 * UDP messages.
 * 
 * @author Matthew D. Hicks
 */
public class BlockingUDPMessageServer extends MessageServer {
	private MessageCertifier certifier;

	private MessageExtractor extractor;

	private InternalMessageListener internalMessageListener;

	private DatagramPacket packet;

	private DatagramSocket socket;

	/**
	 * @param address
	 * @throws SocketException
	 */
	public BlockingUDPMessageServer(InetSocketAddress address)
			throws SocketException, UnknownHostException {
		this(IP.fromInetAddress(address.getAddress()), address.getPort());
	}

	/**
	 * @param hostname
	 *            The hostname to listen for messages on. If <code>null</code>
	 *            will listen on all local addresses.
	 * @param port
	 *            The port to listen for messages on.
	 * @throws SocketException
	 */
	public BlockingUDPMessageServer(IP host, int port) throws SocketException,
			UnknownHostException {
		super(host, port);

		boolean validated = false;
		while (!validated) {
			try {
				if (host != null) {
					socket = new DatagramSocket(port, InetAddress
							.getByAddress(host.getBytes()));
				} else {
					socket = new DatagramSocket(port);
				}
				validated = true;
			} catch (BindException exc) {
				System.err.println("Unable to bind to port: " + port
						+ ", trying: " + (++port) + ".");
			}
		}

		socket.setSoTimeout(1000);

		byte[] buffer = new byte[512];
		packet = new DatagramPacket(buffer, buffer.length);
		// certifier = new MessageCertifier(this);

		// Add Receipt Listener to certify messages
		internalMessageListener = new InternalMessageListener(this);
		addMessageListener(internalMessageListener);
		extractor = new MessageExtractor(this);
	}

	protected void closeChannel() {
		try {
			socket.close();
		} catch (Exception exc) {
		}
	}

	/**
	 * @return The DatagramSocket that is listening for messages.
	 */
	public DatagramSocket getDatagramSocket() {
		return socket;
	}

	/**
	 * @return The MessageCertifier responsible for handling certification of
	 *         received CertifiedMessages.
	 */
	public MessageCertifier getMessageCertifier() {
		return certifier;
	}

	public float ping(IP remoteAddress, int remotePort, long timeout) {
		return internalMessageListener.pingAndWait(remoteAddress, remotePort,
				timeout) / 1000000000.0f;
	}

	public Message receiveMessage() throws IOException {
		try {
			socket.receive(packet);
			extractor.appendData(packet.getData(), 0, packet.getLength(),
					new SocketDescriptor(IP
							.fromInetAddress(packet.getAddress()), packet
							.getPort()));
			extractor.updateAvailableMessages();
			return extractor.nextMessage();
		} catch (SocketTimeoutException exc) {
		} catch (IllegalAccessException exc) {
			exc.printStackTrace();
		} catch (InvocationTargetException exc) {
			exc.printStackTrace();
		} catch (InstantiationException exc) {
			exc.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void resendMessage(Message message, IP remoteAddress, int remotePort)
			throws IOException {
		try {
			byte[] messageBytes = extractor.convertMessage(message);

			DatagramPacket packet = new DatagramPacket(messageBytes,
					messageBytes.length, InetAddress.getByAddress(remoteAddress
							.getBytes()), remotePort);
			DatagramSocket socket;
			socket = getDatagramSocket();
			socket.send(packet);
			messageSent(message);
		} catch (IllegalAccessException exc) {
			throw new RuntimeException(exc);
		} catch (IllegalArgumentException exc) {
			throw new RuntimeException(exc);
		} catch (InvocationTargetException exc) {
			throw new RuntimeException(exc);
		}
	}

	/**
	 * Sends this certified message to the remote server and waits for delivery
	 * confirmation.
	 * 
	 * @param message
	 * @param remoteAddress
	 * @param remotePort
	 * @param timeout
	 * @return true only if it was certified within the timeout specified
	 */
	public boolean sendCertified(CertifiedMessage message, IP remoteAddress,
			int remotePort, long timeout) {
		return internalMessageListener.sendCertified(message, remoteAddress,
				remotePort, timeout);
	}

	public void sendMessage(Message message, IP remoteAddress, int remotePort)
			throws IOException {
		// Assign unique id
		message.setId(JGN.getUniqueLong());
		// Assign send time
		message.setSentStamp(System.currentTimeMillis());

		if (message instanceof CertifiedMessage) {
			getMessageCertifier().enqueue((CertifiedMessage) message,
					remoteAddress, remotePort);
		}
		if (message instanceof OrderedMessage) {
			if (((OrderedMessage) message).getOrderId() == -1) {
				((OrderedMessage) message).setOrderId(OrderedMessage
						.createUniqueId(((OrderedMessage) message)
								.getOrderGroup()));
			}
		}
		resendMessage(message, remoteAddress, remotePort);
	}

	public void timeSync(IP address, int port) {
	}

	public void update() {
		super.update();
		certifier.update();
	}
}
