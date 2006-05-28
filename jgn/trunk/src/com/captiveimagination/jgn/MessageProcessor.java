/**
 * MessageProcessor.java
 *
 * Created: May 28, 2006
 */
package com.captiveimagination.jgn;

/**
 * MessageProcessor implementations may be added to a MessageServer
 * in order to do something to a message before it is sent or before
 * it is received.
 * 
 * @author Matthew D. Hicks
 */
public interface MessageProcessor {
	/**
	 * Invoked on the byte[] representing
	 * an in-bound Message object.
	 * 
	 * @param b
	 * @return
	 * 		a modification of the byte[]
	 * 		passed into it
	 */
	public byte[] inbound(byte[] b);

	/**
	 * Invoked on the byte[] representing
	 * an out-bound Message object.
	 * 
	 * @param b
	 * @return
	 * 		a modification of the byte[]
	 * 		passed into it
	 */
	public byte[] outbound(byte[] b);

	/**
	 * The priority of this MessageProcessor
	 * 
	 * @return
	 * 		the priority higher occurring before
	 * 		lower.
	 */
	public short getPriority();
}
