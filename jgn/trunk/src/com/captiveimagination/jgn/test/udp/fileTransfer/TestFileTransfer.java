package com.captiveimagination.jgn.test.udp.fileTransfer;

import java.io.*;
import java.net.*;

import javax.swing.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.util.*;

/**
 * Simple example sending a file over JGN.
 * 
 * @author Matthew D. Hicks
 *
 */
public class TestFileTransfer {
	public static void main(String[] args) throws Exception {
		MessageServer server = new UDPMessageServer(IP.getLocalHost(), 1000);
		server.addMessageListener(new FileTransferListener() {
			public File startFileTransfer(String fileName, String filePath) {
				System.out.println("Server> Beginning save of: " + fileName);
				return new File(fileName);
			}

            public void endFileTransfer(String fileName, String filePath) {
                System.out.println("Server> File transfer complete!");
                System.exit(0);
            }
		});
        // We start the update thread - this is an alternative to calling update() in your game thread
        server.startUpdateThread();
		
        MessageServerMonitor monitor1 = new MessageServerMonitor(server, 1000);
        monitor1.start();
        monitor1.createGUI("Stress Server Monitor");
        
		MessageServer client = new UDPMessageServer(IP.getLocalHost(), 1005);
        // We start the update thread - this is an alternative to calling update() in your game thread
        client.startUpdateThread();
        
        MessageServerMonitor monitor2 = new MessageServerMonitor(client, 1000);
        monitor2.start();
        JFrame frame2 = monitor2.createGUI("Stress Client Monitor");
        frame2.setLocation(300, 0);
        
        //File file = new File("lib/org.eclipse.jdt.core_3.1.2.jar");
        File file = new File("lib/junit.jar");
		FileTransfer transfer = new FileTransfer(file, 256);
		transfer.transfer(client, IP.getLocalHost(), 1000);
	}
}
