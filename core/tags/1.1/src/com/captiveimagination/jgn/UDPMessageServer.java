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
public class UDPMessageServer extends MessageServer {
    public static int CAPACITY = 512 * 1000;
    
    private byte[] buf;
    private MessageCertifier certifier;
    private DatagramChannel channel;
    private MessageExtractor extractor;
    private ArrayList messageBuffer;
    private ByteBuffer receiveBuffer;
    private ByteBuffer sendBuffer;
    
    public UDPMessageServer(InetSocketAddress address) throws IOException {
        this(IP.fromInetAddress(address.getAddress()), address.getPort());
}
    
    public UDPMessageServer(IP address, int port) throws IOException {
        super(address, port);
        
        init();
}
    
    protected void closeChannel() {
        try {
            channel.close();
} catch(Exception exc) {}
}
    
    /**
     * @return
     *      The MessageCertifier responsible for handling certification
     *      of received CertifiedMessages.
    */
    public MessageCertifier getMessageCertifier() {
        return certifier;
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
        extractor = new MessageExtractor(this);
}

    protected synchronized Message receiveMessage() throws IOException {
        if (messageBuffer.size() > 0) {
            Message m = (Message)messageBuffer.get(0);
            messageBuffer.remove(0);
            return m;
}
        try {
            InetSocketAddress remoteAddress = (InetSocketAddress)channel.receive(receiveBuffer);
            if (remoteAddress != null) {
                int len  = receiveBuffer.position();
                receiveBuffer.rewind();
                receiveBuffer.get(buf, 0, len);
                try {
                    IP address = IP.fromInetAddress(remoteAddress.getAddress());
                    int port = remoteAddress.getPort();
                    extractor.appendData(buf, 0, len, new SocketDescriptor(address,port));
                    receiveBuffer.clear();
                    extractor.updateAvailableMessages();
                    return extractor.nextMessage();
} catch(EOFException exc) {
                    exc.printStackTrace();
                    System.err.println(buf.length + ", " + len);
} catch(InvocationTargetException exc) {
                    exc.printStackTrace();
} catch(IllegalAccessException exc) {
                    exc.printStackTrace();
} catch(InstantiationException exc) {
                    exc.printStackTrace();
} catch (IllegalArgumentException e) {
                    e.printStackTrace();
} catch (InvalidCompressionMethodException e) {
                    e.printStackTrace();
}
}
} catch(ClosedChannelException exc) {
            if (isKeepAlive()) {
                throw exc;
}
}
        return null;
}
    
    public void resendMessage(Message message, IP remoteAddress, int remotePort) throws IOException {
        if (remoteAddress == null) remoteAddress = IP.getLocalHost();
        
        message.setSentStamp(getConvertedTime(remoteAddress, remotePort));
        message.setMessageServer(this);
        try {
            sendBuffer.clear();
            sendBuffer.put(extractor.convertMessage(message));
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

    public synchronized void updateIncoming() {
        super.updateIncoming();
        
        certifier.update();
}
}
