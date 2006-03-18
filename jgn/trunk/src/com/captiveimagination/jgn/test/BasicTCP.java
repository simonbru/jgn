/*
 * Created on Feb 22, 2006
 */
package com.captiveimagination.jgn.test;

import java.net.*;

public class BasicTCP {
    public static void main(String[] args) throws Exception {
        Thread s = new Thread() {
            public void run() {
                try {
                    ServerSocket server = new ServerSocket(9902);
                    Socket socket = server.accept();
                    System.out.println("Connected! " + socket);
                } catch(Exception exc) {
                    exc.printStackTrace();
                }
            }
        };
        s.start();
        
        Thread c = new Thread() {
            public void run() {
                try {
                    Socket socket = new Socket(InetAddress.getByName("captiveimagination.com"), 9902);
                    System.out.println("Connection Client: " + socket);
                } catch(Exception exc) {
                    exc.printStackTrace();
                }
            }
        };
        //c.start();
    }
}
