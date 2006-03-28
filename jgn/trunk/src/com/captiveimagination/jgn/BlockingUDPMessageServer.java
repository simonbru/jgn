/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;

import com.captiveimagination.jgn.message.*;

/**
 * MessageServer is one of the main foundational
 * classes that make up this API and is responsible
 * for listening on a specific address and port for
 * incoming UDP messages.
 * 
 * @author Matthew D. Hicks
 */
public class BlockingUDPMessageServer extends MessageServer {
    private DatagramSocket socket;
    private DatagramPacket packet;
    private MessageCertifier certifier;
    private InternalMessageListener internalMessageListener;
    
    /**
     * @param address
     * @throws SocketException
     */
    public BlockingUDPMessageServer(InetSocketAddress address) throws SocketException, UnknownHostException {
    	this(IP.fromInetAddress(address.getAddress()), address.getPort());
    }
    
    /**
     * @param hostname
     *      The hostname to listen for messages on. If <code>null</code>
     *      will listen on all local addresses.
     * @param port
     *      The port to listen for messages on.
     * @throws SocketException
     */
    public BlockingUDPMessageServer(IP host, int port) throws SocketException, UnknownHostException {
    	super(host, port);
    	
        boolean validated = false;
        while (!validated) {
            try {
                if (host != null) {
                    socket = new DatagramSocket(port, InetAddress.getByAddress(host.getBytes()));
                } else {
                    socket = new DatagramSocket(port);
                }
                validated = true;
            } catch(BindException exc) {
                System.err.println("Unable to bind to port: " + port + ", trying: " + (++port) + ".");
            }
        }
        
        socket.setSoTimeout(1000);
        
        byte[] buffer = new byte[512];
        packet = new DatagramPacket(buffer, buffer.length);
        //certifier = new MessageCertifier(this);

    	// Add Receipt Listener to certify messages
        internalMessageListener = new InternalMessageListener(this);
        addMessageListener(internalMessageListener);
    }
        
    public Message receiveMessage() throws IOException {
    	try {
    		socket.receive(packet);
            return JGN.receiveMessage(packet.getData(), 0, packet.getLength(), IP.fromInetAddress(packet.getAddress()), packet.getPort());
    	} catch(SocketTimeoutException exc) {
    	} catch(IllegalAccessException exc) {
            exc.printStackTrace();
        } catch(InvocationTargetException exc) {
            exc.printStackTrace();
        } catch(InstantiationException exc) {
            exc.printStackTrace();
        }
    	return null;
    }
    
    public void sendMessage(Message message, IP remoteAddress, int remotePort) throws IOException {
		// Assign unique id
        message.setId(JGN.getUniqueLong());
        // Assign send time
        message.setSentStamp(System.currentTimeMillis());
        
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
    	try {
			byte[] messageBytes = JGN.convertMessage(message);
			
			DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, InetAddress.getByAddress(remoteAddress.getBytes()), remotePort);
	        DatagramSocket socket;
	        socket = getDatagramSocket();
	        socket.send(packet);
            messageSent(message);
		} catch(IllegalAccessException exc) {
			throw new RuntimeException(exc);
		} catch (IllegalArgumentException exc) {
			throw new RuntimeException(exc);
		} catch (InvocationTargetException exc) {
			throw new RuntimeException(exc);
		}
    }
    
    public void update() {
    	super.update();
        certifier.update();
    }
    
    /**
     * @return
     *      The DatagramSocket that is listening for messages.
     */
    public DatagramSocket getDatagramSocket() {
        return socket;
    }
    
    /**
     * @return
     *      The MessageCertifier responsible for handling certification
     *      of received CertifiedMessages.
     */
    public MessageCertifier getMessageCertifier() {
        return certifier;
    }
        
    public float ping(IP remoteAddress, int remotePort, long timeout) {
    	return internalMessageListener.pingAndWait(remoteAddress, remotePort, timeout) / 1000000000.0f;
    }
    
    /**
     * Sends this certified message to the remote server and waits
     * for delivery confirmation.
     * 
     * @param message
     * @param remoteAddress
     * @param remotePort
     * @param timeout
     * @return
     * 		true only if it was certified within the timeout
     * 		specified
     */
    public boolean sendCertified(CertifiedMessage message, IP remoteAddress, int remotePort, long timeout) {
    	return internalMessageListener.sendCertified(message, remoteAddress, remotePort, timeout);
    }

    public void timeSync(IP address, int port) {
    }

    protected void closeChannel() {
    	try {
    		socket.close();
    	} catch(Exception exc) {}
    }
}
