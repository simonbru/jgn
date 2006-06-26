/*
 * Created on 19-jun-2006
 */

package com.captiveimagination.jgn;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.captiveimagination.jgn.convert.ConversionHandler;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.queue.MessageQueue;

/**
 * The <code>PacketCombiner</code> combines packets. Yes, it does.
 * 
 * @author Skip M. B. Balk
 */

class PacketCombiner {
	// map holding the last failed message of each client, if any
	private static Map<Object, Message> clientToFailedMessage = new HashMap<Object, Message>();

	private static final int bigBufferSize = 512 * 1024;
	private static volatile ByteBuffer buffer;

	static {
		replaceBackingBuffer();
	}

	/**
	 * Combines as much as possible Messages from the client into a single
	 * packet.
	 * 
	 * @param client
	 * @return CombinedPacket containing the buffer containing multiple packets, list of messages, and the message positions in the buffer
	 */
	public synchronized static final CombinedPacket combine(MessageClient client, int maxBytes)
					throws MessageHandlingException {
		CombinedPacket combined = null;
		int chunkPos0 = buffer.position();

		int sumBytes = 0;

		Message failed = clientToFailedMessage.get(client);
		Message initialFailed = failed;
		MessageQueue queue = client.getOutgoingQueue();

		boolean bufferFull = false;

		while (true) {
			Message msg;

			// either get a new message or the last failed message
			if (failed == null) {
				msg = queue.poll();
			} else {
				msg = failed;
				failed = null;
			}

			// no message to send
			if (msg == null) break;

			msg.setMessageClient(client);					// Assign the MessageClient
			msg.setTimestamp(System.currentTimeMillis());	// Set the timestamp

			// handle message
			ConversionHandler handler;
			handler = ConversionHandler.getConversionHandler(msg.getClass());

			// not enough space left for size-info
			if ((sumBytes + 4 > maxBytes)) {
				failed = msg;
				break;
			}

			// A: write the size later (see B)
			try {
				buffer.putInt(-1);
			} catch (BufferOverflowException exc) {
				// buffer full
				bufferFull = true;
				failed = msg;
				break;
			}

			int packetPos0 = buffer.position();
			try {
				handler.sendMessage(msg, buffer);
			} catch (BufferOverflowException exc) {
				// buffer full
				bufferFull = true;
				failed = msg;

				// restore the buffer
				buffer.position(packetPos0 - 4);

				// failed on 2nd attempt?
				if (failed == initialFailed) {
					throw new MessageHandlingException("Message size larger than backing-buffer: "
									+ (bigBufferSize / 1024) + "K", failed);
				}

				break;
			} catch (MessageHandlingException exc) {
				// something serious went wrong, give up

				// restore the buffer
				buffer.position(packetPos0 - 4);

				// don't set this, as the message is seriously broken
				// NOT: failed = msg;

				// rethrow exception
				throw exc;
			}
			int packetPos1 = buffer.position();
			int packetSize = packetPos1 - packetPos0;

			// we managed to write the message to the buffer, but are we allowed?
			if (sumBytes + 4 + packetSize > maxBytes) {
				// not enough space to write message-data
				failed = msg;

				// restore the buffer
				buffer.position(packetPos0 - 4);

				break;
			}

			sumBytes += 4 + packetSize;

			// B: write size at start of the packet (see A)
			// this does not modify position/limit
			buffer.putInt(packetPos0 - 4, packetSize);

			// Add it to the message sent queue
			//client.getOutgoingMessageQueue().add(msg);
			if (combined == null) combined = new CombinedPacket(client);
			combined.add(msg, buffer.position() - chunkPos0);
		}

		int chunkPos1 = buffer.position();
		// int chunkSize = chunkPos1 - chunkPos0;

		ByteBuffer chunk;

		if (sumBytes != 0) {
			// setup the position/limit to be sliced
			buffer.limit(chunkPos1);
			buffer.position(chunkPos0);
			chunk = buffer.slice();

			// restore the position/limit for next write
			buffer.limit(buffer.capacity());
			buffer.position(chunkPos1);
		} else {
			chunk = null;
		}

		if (bufferFull) {
			replaceBackingBuffer();
		}

		// remember what the last packet was that failed, if any
		clientToFailedMessage.put(client, failed);
		
		if (chunk == null) return null;
		combined.setBuffer(chunk);
		return combined;
	}

	private static final void replaceBackingBuffer() {
		buffer = ByteBuffer.allocateDirect(bigBufferSize);
	}
}
