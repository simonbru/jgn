package com.captiveimagination.jgn.test.tcp.stream;

import java.io.*;
import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.stream.*;

/**
 * @author Matthew D. Hicks
 */
public class TestFileStream {
	public static void main(String[] args) throws Exception {
		final MessageServer server1 = new TCPMessageServer(IP.getLocalHost(), 1000);
		server1.startUpdateThread();
		
		final MessageServer server2 = new TCPMessageServer(IP.getLocalHost(), 2000);
		server2.startUpdateThread();
		
		final JGNInputStream is = server2.getInputStream(IP.getLocalHost(), -1);
		final JGNOutputStream os = server1.getOutputStream(IP.getLocalHost(), 2000);
		new Thread() {
			public void run() {
				try {
					FileOutputStream out = new FileOutputStream(new File("test.gif"));
					byte[] b = new byte[512];
					int total = 0;
					int len;
					while ((len = is.read(b)) > -1) {
						out.write(b, 0, len);
						total += len;
						System.out.println("Received: " + len);
					}
					out.flush();
					is.close();
					out.close();
					System.out.println("Successfully received " + total + " bytes.");
				} catch(IOException exc) {
					exc.printStackTrace();
				}
			}
		}.start();
		
		// The output stream
		new Thread() {
			public void run() {
				try {
					InputStream in = getClass().getClassLoader().getResourceAsStream("com/captiveimagination/jgn/test/tcp/stream/tomcat.gif");
					byte[] b = new byte[512];
					int total = 0;
					int len;
					while ((len = in.read(b)) > -1) {
						os.write(b, 0, len);
						total += len;
					}
					os.flush();
					in.close();
					os.close();
					System.out.println("Successfully sent " + total + " bytes.");
				} catch(StreamInUseException exc) {
					exc.printStackTrace();
				} catch(UnknownHostException exc) {
					exc.printStackTrace();
				} catch(IOException exc) {
					exc.printStackTrace();
				}
			}
		}.start();
	}
}
