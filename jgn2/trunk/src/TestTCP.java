import java.net.*;
import java.nio.channels.*;

public class TestTCP {
	public static void main(String[] args) throws Exception {
		Selector selector = Selector.open();
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
		ssc.configureBlocking(false);
		SelectionKey key = ssc.register(selector, SelectionKey.OP_ACCEPT);
		// Attach the message server to the key
		//key.attach(messageServer);
		
	}
}
