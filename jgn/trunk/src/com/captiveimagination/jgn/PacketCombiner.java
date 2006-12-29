/*
 * Created on 19-jun-2006
 */

package com.captiveimagination.jgn;

import java.nio.*;

import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.queue.*;

/**
 * The <code>PacketCombiner</code> combines packets.
 * 
 * @author Skip M. B. Balk
 * @author Matthew D. Hicks
 */
public class PacketCombiner {
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
	public static final synchronized CombinedPacket combine(MessageClient client, int maxBytes) throws MessageHandlingException {
		MessageQueue queue = client.getOutgoingQueue();
		CombinedPacket packet = new CombinedPacket(client);
		
		int startPosition = buffer.position();
		boolean bufferFull = false;
		
		while (true) {
			Message message = client.getOverhangMessage();
			client.setOverhangMessage(null);
			if (message == null) {
				message = queue.poll();
			}
			if (message == null) break;
			// We mustn't try to send messages other than registration until the client is connected
			if ((!(message instanceof LocalRegistrationMessage)) && (!(message instanceof DisconnectMessage))) {
				if ((client.getStatus() != MessageClient.Status.CONNECTED) && (client.getStatus() != MessageClient.Status.DISCONNECTING)) {
					queue.add(message);
					break;
				}
			}
			
			message.setMessageClient(client);
			if (message instanceof OrderedMessage) {
				OrderedMessage.assignOrderId((OrderedMessage)message);
			}
			message.setTimestamp(System.currentTimeMillis());
			
			int messageStart = buffer.position();
			try {
				// Write the length int to -1 initially until we know how large it is
				buffer.putInt(-1);
				
				// Attempt to put this message into the buffer
				message.getMessageClient().getMessageServer().convertMessage(message, buffer);
				int messageEnd = buffer.position();
				//System.out.println("MESSAGE: " + message + ", " + (messageEnd - messageStart - 4));
				buffer.position(messageStart);
				buffer.putInt(messageEnd - messageStart - 4);
				buffer.position(messageEnd);
				packet.add(message, messageEnd - startPosition);
				//System.out.println("MessageLength(Write): " + (messageEnd - messageStart - 4));
			} catch(BufferOverflowException exc) {
				buffer.position(messageStart);
				client.setOverhangMessage(message);
				bufferFull = true;
				break;
			}
		}

		if (buffer.position() > startPosition) {
			int endPosition = buffer.position();
			buffer.limit(endPosition);
			buffer.position(startPosition);
			ByteBuffer slice = buffer.slice();
			
			packet.setBuffer(slice);
			
			buffer.limit(buffer.capacity());
			buffer.position(endPosition);
		} else {
			packet = null;		// There is nothing to send, so we return null
		}
		
		if (bufferFull) {
			replaceBackingBuffer();		// The buffer was filled, so we need to replace it
			if ((startPosition == 0) && (packet.size() == 0)) {
				throw new MessageHandlingException("Message is larger than ByteBuffer's capacity!");
			}
		}
		
		return packet;
	}
	
	private static final void replaceBackingBuffer() {
		buffer = ByteBuffer.allocateDirect(bigBufferSize);
	}
}
