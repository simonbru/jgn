/*
 * Created on Feb 11, 2006
 */
package com.captiveimagination.jgn.test.tcp;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import com.captiveimagination.jgn.*;

public class StraightTCPTest {
    public static void main(String[] args) throws Exception {
        startReceiver();
        
        startSender();
    }
    
    private static void startReceiver() {
        Thread t = new Thread() {
            public void run() {
                try {
                    ServerSocketChannel server = ServerSocketChannel.open();
                    server.configureBlocking(false);
                    server.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
                    SocketChannel channel = null;
                    ByteBuffer buffer = ByteBuffer.allocate(512);
                    byte[] buf = new byte[512 * 1000];
                    long time = 0;
                    while (true) {
                        Thread.sleep(1);
                        if (channel == null) {
                            channel = server.accept();
                            if (channel != null) {
                                channel.configureBlocking(false);
                                time = JGN.getNanoTime();
                            }
                        } else if (channel.read(buffer) > 0) {
                            int len = buffer.position();
                            //System.out.println("Length: " + len);
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
                                        System.out.println("Took " + ((JGN.getNanoTime() - time) / 1000000) + "ms to receive " + packets + " packets.");
                                        System.exit(0);
                                    }
                                    //System.out.println("Received: " + s);
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
                    SocketChannel channel = SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
                    channel.configureBlocking(false);
                    ByteBuffer buffer = ByteBuffer.allocate(512);
                    byte[] buf;
                    long time = JGN.getNanoTime();
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
                        channel.write(buffer);
                        Thread.sleep(5);
                    }
                    System.out.println("Took " + ((JGN.getNanoTime() - time) / 1000000) + "ms to send.");
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
        };
        t.start();
    }
}
