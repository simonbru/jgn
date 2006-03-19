package com.captiveimagination.jgn;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class UDPMessageServer extends MessageServer {
	public static int CAPACITY = 512 * 1000;
	
	private DatagramChannel channel;
	private ByteBuffer receiveBuffer;
	private ByteBuffer sendBuffer;
	private ArrayList messageBuffer;
	private byte[] buf;
	private MessageCertifier certifier;
	
	public UDPMessageServer(InetSocketAddress address) throws IOException {
		this(IP.fromInetAddress(address.getAddress()), address.getPort());
	}
	
	public UDPMessageServer(IP address, int port) throws IOException {
		super(address, port);
		
		init();
	}
	
	private void init() throws IOException {
		messageBuffer = new ArrayList();
		receiveBuffer = ByteBuffer.allocate(CAPACITY);
		sendBuffer = ByteBuffer.allocate(CAPACITY);
		buf = new byte[CAPACITY];
		
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		
		InetAddress host = null;
		if (getAddress() != null) {
			host = InetAddress.getByAddress(getAddress().getBytes());
		}
		channel.socket().bind(new InetSocketAddress(host, getPort()));
		channel.socket().setReceiveBufferSize(CAPACITY);
		
		certifier = new MessageCertifier(this);
	}
	
	public Message receiveMessage() throws IOException {
		if (messageBuffer.size() > 0) {
            Message m = (Message)messageBuffer.get(0);
            messageBuffer.remove(0);
            return m;
        }
		InetSocketAddress remoteAddress = (InetSocketAddress)channel.receive(receiveBuffer);
		if (remoteAddress != null) {
			int len  = receiveBuffer.position();
			receiveBuffer.rewind();
			receiveBuffer.get(buf, 0, len);
			try {
				IP address = IP.fromInetAddress(remoteAddress.getAddress());
				Message m = JGN.receiveMessage(buf, 0, len, address, remoteAddress.getPort());
                m.setMessageServer(this);
				if (m.getMessageLength() < len) {
					int pos = m.getMessageLength();
					while (pos < len) {
						Message temp = JGN.receiveMessage(buf, pos, len, address, remoteAddress.getPort());
                        temp.setMessageServer(this);
						messageBuffer.add(temp);
						pos += temp.getMessageLength();
					}
				}
				receiveBuffer.clear();
				return m;
			} catch(EOFException exc) {
				exc.printStackTrace();
				System.err.println(buf.length + ", " + len);
			} catch(InvocationTargetException exc) {
				exc.printStackTrace();
			} catch(IllegalAccessException exc) {
				exc.printStackTrace();
			} catch(InstantiationException exc) {
				exc.printStackTrace();
			}
		}
		return null;
	}

	public void sendMessage(Message message, IP remoteAddress, int remotePort) throws IOException {
		message.setId(JGN.getUniqueLong());
		
		if (message instanceof CertifiedMessage) {
        	getMessageCertifier().enqueue((CertifiedMessage)message, remoteAddress, remotePort);
        }
		if (message instanceof OrderedMessage) {
        	if (((OrderedMessage)message).getOrderId() == -1) {
        		((OrderedMessage)message).setOrderId(OrderedMessage.createUniqueId(((OrderedMessage)message).getOrderGroup()));
        	}
        }
		resendMessage(message, remoteAddress, remotePort);
	}
	
	public void resendMessage(Message message, IP remoteAddress, int remotePort) throws IOException {
		if (remoteAddress == null) remoteAddress = IP.getLocalHost();
		
		message.setSentStamp(getConvertedTime(remoteAddress, remotePort));
        message.setMessageServer(this);
		try {
			byte[] messageBytes = JGN.convertMessage(message);
			sendBuffer.clear();
			sendBuffer.put(messageBytes);
			sendBuffer.flip();
			channel.send(sendBuffer, new InetSocketAddress(remoteAddress.toString(), remotePort));
			message.setRemoteAddress(remoteAddress);
			message.setRemotePort(remotePort);
			messageSent(message);
		} catch(IllegalAccessException exc) {
			throw new RuntimeException(exc);
		} catch (IllegalArgumentException exc) {
			throw new RuntimeException(exc);
		} catch (InvocationTargetException exc) {
			throw new RuntimeException(exc);
		}
	}
	
	/**
     * @return
     *      The MessageCertifier responsible for handling certification
     *      of received CertifiedMessages.
     */
    public MessageCertifier getMessageCertifier() {
        return certifier;
    }
}
