package javang.core;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class Network {
	private final Selector selector;
	private final Map<SelectionKey, TCPDataQueue> tcpKey2queue;
	private final Map<SelectionKey, UDPDataQueue> udpKey2queue;
	private final ByteBuffer buffer;
	private final Thread accessThread;
	private NetworkHandlerProvider provider;
	private final Map<SelectionKey, NetworkHandler> key2handler;

	public Network(NetworkHandlerProvider provider) {
		this(provider, 64 * 1024);
	}

	public Network(NetworkHandlerProvider provider, int bufferSize) {
		this.provider = provider;
		this.accessThread = Thread.currentThread();

		this.tcpKey2queue = new HashMap<SelectionKey, TCPDataQueue>();
		this.udpKey2queue = new HashMap<SelectionKey, UDPDataQueue>();

		this.buffer = ByteBuffer.allocate(bufferSize);
		this.key2handler = new HashMap<SelectionKey, NetworkHandler>();

		try {
			this.selector = Selector.open();
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		}
	}

	private final void checkThreadAccess() {
		if (this.accessThread != Thread.currentThread()) throw new IllegalStateException();
	}

	//

	public final SelectionKey createServer(int port) {
		return this.createServer(new InetSocketAddress((InetAddress) null, port));
	}

	public final SelectionKey createServer(InetSocketAddress endpoint) {
		this.checkThreadAccess();

		try {
			ServerSocketChannel ssc = ServerSocketChannel.open();
			ssc.socket().bind(endpoint);
			ssc.configureBlocking(false);
			return ssc.register(selector, SelectionKey.OP_ACCEPT);
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		}
	}

	public final SelectionKey createClient(InetSocketAddress host) {
		return this.createClient(null, host);
	}

	public final SelectionKey createClient(InetSocketAddress bind, InetSocketAddress host) {
		this.checkThreadAccess();

		try {
			SocketChannel sc = SocketChannel.open();
			sc.socket().bind(bind);
			SelectionKey key = this.setupClient(sc);
			sc.connect(host);
			return key;
		} catch (Exception exc) {
			exc.printStackTrace();
			return null;
		}
	}

	public final SelectionKey createDatagramHost(InetSocketAddress bind) {
		this.checkThreadAccess();

		try {
			DatagramChannel dgc = DatagramChannel.open();
			dgc.socket().bind(bind);
			dgc.configureBlocking(false);

			SelectionKey key = dgc.register(selector, SelectionKey.OP_READ);
			udpKey2queue.put(key, new UDPDataQueue());
			key2handler.put(key, provider.provide(this, key));

			return key;
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		}
	}

	// udp

	private static final boolean udp_max_len_warn = true;
	private static final int udp_max_len = 1500;

	public final void write(SelectionKey key, byte[] buf, InetSocketAddress target) {
		this.write(key, buf, 0, buf.length, target);
	}

	public final void write(SelectionKey key, byte[] buf, int off, int len, InetSocketAddress target) {
		this.checkThreadAccess();

		if (len > udp_max_len)
			if (udp_max_len_warn)
				System.out.println("WARNING: UDP-packet is " + len + "B, advised size is < " + udp_max_len + "B");

		UDPDataQueue writeQueue = udpKey2queue.get(key);
		if (writeQueue == null) {
			if (tcpKey2queue.containsKey(key)) // probably user typo
				throw new IllegalStateException("you must not specify the target host for a TCP write");
			throw new IllegalStateException("write udp-packet failed due to lack of write-queue");
		}

		byte[] data = new byte[len];
		System.arraycopy(buf, off, data, 0, len);
		writeQueue.addLast(new UDPPacket(data, target));

		this.adjustInterestOp(key, SelectionKey.OP_WRITE, true);
	}

	// tcp

	public final int write(SelectionKey key, byte[] buf) {
		return this.write(key, buf, 0, buf.length);
	}

	public final int write(SelectionKey key, byte[] buf, int off, int len) {
		this.checkThreadAccess();

		TCPDataQueue writeQueue = tcpKey2queue.get(key);
		if (writeQueue == null) {
			if (udpKey2queue.containsKey(key)) // probably user typo
				throw new IllegalStateException("you must specify the target host for an UDP write");
			throw new IllegalStateException("write tcp-packet failed due to lack of write-queue");
		}

		byte[] data = new byte[len];
		System.arraycopy(buf, off, data, 0, len);
		writeQueue.addLast(data);

		this.adjustInterestOp(key, SelectionKey.OP_WRITE, true);

		return writeQueue.totalEnqueued += data.length;
	}

	//

	public final int countPendingOutboundBytes(SelectionKey key) {
		this.checkThreadAccess();

		if (key.channel() instanceof SocketChannel) return tcpKey2queue.get(key).pending();

		if (key.channel() instanceof DatagramChannel) {
			UDPDataQueue writeQueue = udpKey2queue.get(key);
			int sum = 0;
			for (UDPPacket p : writeQueue)
				sum += p.data.length;
			return sum;
		}

		throw new IllegalStateException();
	}

	public final boolean hasPendingOutboundBytes(SelectionKey key) {
		this.checkThreadAccess();

		if (key.channel() instanceof SocketChannel) {
			return tcpKey2queue.get(key).pending() > 0;
		}

		if (key.channel() instanceof DatagramChannel) {
			UDPDataQueue writeQueue = udpKey2queue.get(key);
			for (UDPPacket p : writeQueue)
				if (p.data.length > 0) return true;
			return false;
		}

		throw new IllegalStateException();
	}
	
	public final boolean hasSent(SelectionKey key, int totalBytes)
	{
		this.checkThreadAccess();

		if (key.channel() instanceof SocketChannel) {
			return tcpKey2queue.get(key).totalSent >= totalBytes;
		}

		if (key.channel() instanceof DatagramChannel) {
			UDPDataQueue writeQueue = udpKey2queue.get(key);
			for (UDPPacket p : writeQueue)
				if (p.data.length > 0) return true;
			return false;
		}

		throw new IllegalStateException();
	}

	//

	private final void adjustInterestOp(SelectionKey key, int op, boolean state) {
		this.checkThreadAccess();

		try {
			int ops = key.interestOps();
			if (state != ((ops & op) == op)) key.interestOps(state ? (ops | op) : (ops & ~op));
		} catch (CancelledKeyException exc) {
			// ignore
		}
	}

	public void execute() throws IOException {
		this.checkThreadAccess();

		this.selector.selectNow();

		for (SelectionKey key : key2handler.keySet()) {
			if (key.isValid()) this.fireExecute(key);
			else this.fireDisconnected(key, null);
		}

		Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

		while (keys.hasNext()) {
			SelectionKey key = keys.next();
			keys.remove();

			if (!key.isValid()) {
				this.fireDisconnected(key, null);
				continue;
			}

			if (key.channel() instanceof ServerSocketChannel) {
				if (key.isValid() && key.isAcceptable()) {
					SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
					this.fireConnected(this.setupClient(sc));
				}
			} else if (key.channel() instanceof SocketChannel) {
				if (key.isValid() && key.isConnectable()) {
					((SocketChannel) key.channel()).finishConnect();
					this.adjustInterestOp(key, SelectionKey.OP_CONNECT, false);
					this.fireConnected(key);
				}

				if (key.isValid() && key.isReadable()) this.readTCP(key);

				if (key.isValid() && key.isWritable()) this.writeTCP(key);
			} else if (key.channel() instanceof DatagramChannel) {
				if (key.isValid() && key.isReadable()) this.readUDP(key);

				if (key.isValid() && key.isWritable()) this.writeUDP(key);
			}
		}
	}

	private final void readTCP(SelectionKey key) {
		try {
			buffer.clear();
			int read = ((SocketChannel) key.channel()).read(buffer);
			if (read == -1) throw new EOFException();

			byte[] data = new byte[read];
			buffer.flip();
			buffer.get(data);

			this.fireReceivedTCP(key, data);
		} catch (IOException exc) {
			this.fireDisconnected(key, exc);
			key.attach(null);
			key.cancel();
		}
	}

	private final void writeTCP(SelectionKey key) {
		TCPDataQueue queue = tcpKey2queue.get(key);
		if (queue.isEmpty()) {
			this.adjustInterestOp(key, SelectionKey.OP_WRITE, false);
		} else {
			try {
				this.joinQueueWriteTCP(key, queue);
			} catch (IOException exc) {
				this.fireDisconnected(key, exc);
				key.attach(null);
				key.cancel();
			}
		}
	}

	private final void readUDP(SelectionKey key) {
		try {
			do {
				buffer.clear();
				InetSocketAddress source = (InetSocketAddress) ((DatagramChannel) key.channel()).receive(buffer);
				if (source == null) break;
				buffer.flip();

				byte[] data = new byte[buffer.remaining()];
				buffer.get(data);
				this.fireReceivedUDP(key, data, source);
			} while (true);
		} catch (IOException exc) {
			this.fireDisconnected(key, exc);
			key.attach(null);
			key.cancel();
		}
	}

	private final void writeUDP(SelectionKey key) {
		try {
			UDPDataQueue queue = udpKey2queue.get(key);

			if (queue.isEmpty()) {
				this.adjustInterestOp(key, SelectionKey.OP_WRITE, false);
			} else {
				do {
					UDPPacket p = queue.removeFirst();
					buffer.clear();
					buffer.put(p.data);
					buffer.flip();

					int sent = ((DatagramChannel) key.channel()).send(buffer, p.target);
					if (sent == 0) {
						queue.addFirst(p);
						break;
					}
					this.fireSent(key, p.data.length);
				} while (!queue.isEmpty());
			}
		} catch (IOException exc) {
			this.fireDisconnected(key, exc);
			key.attach(null);
			key.cancel();
		}
	}

	private final void fireConnected(SelectionKey key) {
		NetworkHandler handler = provider.provide(this, key);
		key2handler.put(key, handler);

		try {
			handler.onConnected(this, key);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	private final void fireExecute(SelectionKey key) {
		try {
			key2handler.get(key).onExecute(this, key);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	private final void fireReceivedTCP(SelectionKey key, byte[] data) {
		try {
			key2handler.get(key).onReceivedTCP(this, key, data);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	private final void fireReceivedUDP(SelectionKey key, byte[] data, InetSocketAddress source) {
		try {
			key2handler.get(key).onReceivedUDP(this, key, data, source);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	private final void fireSent(SelectionKey key, int bytes) {
		try {
			key2handler.get(key).onSent(this, key, bytes);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	private final void fireDisconnected(SelectionKey key, IOException cause) {
		tcpKey2queue.remove(key);
		udpKey2queue.remove(key);

		try {
			key2handler.remove(key).onDisconnected(this, key, cause);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	private final void joinQueueWriteTCP(SelectionKey key, TCPDataQueue queue) throws IOException {
		buffer.clear();

		// copy byte[]s
		while (!queue.isEmpty() && buffer.remaining() >= queue.getFirst().length)
			buffer.put(queue.removeFirst());

		// write partial byte[]
		if (!queue.isEmpty() && buffer.hasRemaining()) {
			byte[] full = queue.removeFirst();
			byte[] sub = new byte[buffer.remaining()];
			byte[] rem = new byte[full.length - sub.length];

			System.arraycopy(full, 0, sub, 0, sub.length);
			System.arraycopy(full, sub.length, rem, 0, rem.length);

			buffer.put(sub);
			queue.addFirst(rem);
		}

		// write to channel
		buffer.flip();
		if (buffer.hasRemaining()) ((SocketChannel) key.channel()).write(buffer);

		// did we even send anything?
		if (buffer.position() != 0) {
			queue.totalSent += buffer.position();
			this.fireSent(key, buffer.position());
		}

		// put unsent data back in queue
		if (buffer.hasRemaining()) {
			buffer.compact().flip();
			byte[] rem = new byte[buffer.remaining()];
			buffer.get(rem);
			queue.addFirst(rem);
		}
	}

	private final SelectionKey setupClient(SocketChannel sc) throws IOException {
		sc.configureBlocking(false);
		sc.socket().setTcpNoDelay(true);

		int ops = 0;
		ops |= SelectionKey.OP_READ;
		ops |= SelectionKey.OP_CONNECT;
		SelectionKey key = sc.register(selector, ops);

		tcpKey2queue.put(key, new TCPDataQueue());

		return key;
	}

	//

	private class UDPPacket {
		final byte[] data;
		final InetSocketAddress target;

		UDPPacket(byte[] data, InetSocketAddress addr) {
			this.data = data;
			this.target = addr;
		}

	}

	// classes for readability
	class TCPDataQueue extends LinkedList<byte[]> {
		public int totalEnqueued = 0;
		public int totalSent = 0;

		public int pending() {
			return totalEnqueued - totalSent;
		}
	}

	class UDPDataQueue extends LinkedList<UDPPacket> {
		//
	}
}