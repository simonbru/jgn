import java.net.*;
import java.nio.channels.*;

public class TestTCP {
	public static void main(String[] args) throws Exception {
		Thread server = new Thread() {
			public void run() {
				try {
					serverThread();
				} catch(Exception exc) {
					exc.printStackTrace();
				}
			}
		};
		server.start();
		
		Thread client = new Thread() {
			public void run() {
				try {
					clientThread();
				} catch(Exception exc) {
					exc.printStackTrace();
				}
			}
		};
		client.start();
	}
	
	public static void serverThread() throws Exception {
		Selector selector = Selector.open();
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
		ssc.configureBlocking(false);
		SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);
		// Attach the message server to the key
		//key.attach(messageServer);
		while (true) {
			if (selector.selectNow() > 0) {
				SelectionKey testKey = selector.selectedKeys().iterator().next();
				System.out.println("Received event on " + testKey);
			}
			Thread.sleep(1000);
		}
	}
	
	public static void clientThread() throws Exception {
		Selector selector = Selector.open();
		SocketChannel channel = SocketChannel.open();
		channel.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), 2000));
		channel.socket().connect(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		System.out.println("Connection established...I think....");
	}
}
