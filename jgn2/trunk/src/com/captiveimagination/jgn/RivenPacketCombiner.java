/*
 * Created on 17-jun-2006
 */

package com.captiveimagination.jgn;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.captiveimagination.jgn.convert.ConversionHandler;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.queue.MessageQueue;

public class RivenPacketCombiner {

	private final MessageQueue queue;
	private final int writeBufferSize;

	public RivenPacketCombiner(MessageQueue queue, int writeBufferSize) {
		this.queue = queue;
		this.writeBufferSize = writeBufferSize;

		this.initBuffers();
	}

	private final int maxPackets = 256;
	private ByteBuffer dataBuffer;
	private ByteBuffer sizeBuffer;

	private final void initBuffers() {

		this.dataBuffer = ByteBuffer.allocateDirect(writeBufferSize);
		this.sizeBuffer = ByteBuffer.allocateDirect(maxPackets * 4);
	}

	private Message failedMessage = null;

	public final ByteBuffer[] combineUpTo(int maxSize) {

		if (failedMessage != null) {
			// We only have a failedMessage if it didn't fit in the data-buffer.
			//
			// The old buffer MUST be discarded, as it is impossible to
			// determine whether the callee is still using the previously
			// returned ByteBuffer[], and if so: how much it sent.
			this.initBuffers();
		}

		int packets = 0;
		int byteCount = 0;

		List<Integer> offs = new ArrayList<Integer>();
		List<Integer> ends = new ArrayList<Integer>();

		while (true) {

			if (queue.isEmpty()) break;
			if (byteCount >= maxSize) break;
			if (packets >= maxPackets) break;

			int messageBeginPosition = dataBuffer.position();

			// get the message to send
			Message msg;
			if (failedMessage != null) {
				msg = failedMessage;
				failedMessage = null;
			} else {
				msg = queue.poll();
			}

			// get the handler
			ConversionHandler handler = ConversionHandler.getConversionHandler(msg.getClass());

			try {
				// append the message to the data-buffer
				// - this might throw a BufferOverflowException
				// - this might throw an Exception caused by reflection
				handler.sendMessage(msg, dataBuffer);

				int messageEndPosition = dataBuffer.position();
				int messageSize = messageEndPosition - messageBeginPosition;

				// append the size to the size-buffer
				// - this will never throw a BufferOverflowException
				sizeBuffer.putInt(messageSize);

				// store the packet off/end
				offs.add(messageBeginPosition);
				ends.add(messageEndPosition);

				// increase the byteCount
				byteCount += (messageSize + 4);

				packets++;
			} catch (BufferOverflowException exc) {
				// undo the last message
				dataBuffer.position(messageBeginPosition);
				failedMessage = msg;
				break;
			} catch (Exception exc) {
				throw new IllegalStateException("Failed to convert message", exc);
			}
		}

		// push buffers: pos/lim
		int endPosition = dataBuffer.position();
		int sizeInfoOffset = sizeBuffer.position() - (packets * 4);

		// now we have packets & sizes
		// we take the buffers,
		ByteBuffer[] combined = new ByteBuffer[packets * 2];
		for (int i = 0; i < packets; i++) {
			int off = offs.get(i);
			int end = ends.get(i);

			dataBuffer.limit(end);
			dataBuffer.position(off);

			// append size to sizeBuffer (with a proper view-region)
			sizeBuffer.limit(sizeInfoOffset + (i + 1) * 4);
			sizeBuffer.position(sizeInfoOffset + (i + 0) * 4);

			// append [views to data] to ByteBuffer[]
			combined[i * 2 + 0] = sizeBuffer.slice(); // view to size-region
			combined[i * 2 + 1] = dataBuffer.slice(); // view to data-region
		}

		// pop buffers: pos/lim
		sizeBuffer.limit(sizeBuffer.capacity());
		sizeBuffer.position(sizeInfoOffset + (packets * 4));

		dataBuffer.limit(dataBuffer.capacity());
		dataBuffer.position(endPosition);

		return combined;
	}
}
