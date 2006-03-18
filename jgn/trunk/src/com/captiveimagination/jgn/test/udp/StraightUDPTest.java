package com.captiveimagination.jgn.test.udp;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class StraightUDPTest {
	public static void main(String[] args) throws Exception {
        startReceiver();
        
        startSender();
		
		/*InetSocketAddress address = new InetSocketAddress(1000);
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		Selector selector = Selector.open();
		channel.register(selector, SelectionKey.OP_READ);
		channel.send()*/
    }
    
    private static void startReceiver() {
        Thread t = new Thread() {
            public void run() {
                try {
                    DatagramChannel server = DatagramChannel.open();
                    server.configureBlocking(false);
                    server.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
                    //Selector selector = Selector.open();
            		//server.register(selector, SelectionKey.OP_READ);
                    ByteBuffer buffer = ByteBuffer.allocate(512 * 1000);
                    byte[] buf = new byte[512 * 1000];
                    long time = 0;
                    while (true) {
                        Thread.sleep(1);
                        SocketAddress address = server.receive(buffer);
                        if (address != null) {
                            int len = buffer.position();
                            buffer.rewind();
                            buffer.get(buf, 0, len);
                            buffer.clear();
                            ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, len);
                            DataInputStream dis = new DataInputStream(bais);
                            int bytesLength;
                            byte[] bytes;
                            int packets = 0;
                            try {
                                while ((bytesLength = dis.readInt()) > -1) {
                                    dis.read(bytes = new byte[bytesLength]);
                                    packets++;
                                    String s = new String(bytes);
                                    if (s.equals("Testing 1000")) {
                                        System.out.println("Took " + ((System.nanoTime() - time) / 1000000) + "ms to receive " + packets + " packets.");
                                        System.exit(0);
                                    }
                                }
                            } catch(EOFException exc) {
                                //System.out.println("End of stream reached!");
                            }
                            bais.close();
                        }
                    }
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
        };
        t.start();
    }
    
    private static void startSender() {
        Thread t = new Thread() {
            public void run() {
                try {
                    DatagramChannel channel = DatagramChannel.open();
                    channel.configureBlocking(false);
                    //channel.socket().connect(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
                    ByteBuffer buffer = ByteBuffer.allocate(512 * 1000);
                    byte[] buf;
                    long time = System.nanoTime();
                    for (int i = 0; i < 1000; i++) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        byte[] textBytes = ("Testing " + (i + 1)).getBytes();
                        dos.writeInt(textBytes.length);
                        dos.write(textBytes);
                        buf = baos.toByteArray();
                        baos.close();
                        buffer.clear();
                        buffer.put(buf);
                        buffer.flip();
                        //channel.write(buffer);
                        channel.send(buffer, new InetSocketAddress(InetAddress.getLocalHost(), 1000));
                        Thread.sleep(5);
                    }
                    System.out.println("Took " + ((System.nanoTime() - time) / 1000000) + "ms to send.");
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
        };
        t.start();
    }
}
