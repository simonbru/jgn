package com.captiveimagination.jgn.message;

import java.util.*;

/**
 * This message type allows a series of messages to be sent in a guaranteed order
 * of delivery. This message type extends the CertifiedMessage type for guaranteed
 * delivery. The only method that should ever be explicitly called by implementing
 * applications is setOrderGroup(). All other methods are used internally for
 * verification of proper delivery.
 * 
 * @author Matthew D. Hicks
 */
public abstract class OrderedMessage extends CertifiedMessage {
	private static HashMap countGroups;
	private static long machineCode = createUniversalUniqueId();
	
	private long orderId;
	private short orderGroup;
	private long orderOriginator;
	
	public OrderedMessage() {
		orderId = -1;
		orderOriginator = machineCode;
	}
	
	/**
	 * This is set internally set on send if this is not explicitly set. If set explicitly the numbers
	 * MUST start at 0 and never skip or the next in sequence will never be received.
	 * 
	 * @param orderId
	 */
	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}
	
	/**
	 * @return
	 * 		the sequence id of this message
	 */
	public long getOrderId() {
		return orderId;
	}
	
	/**
	 * This defaults to 0, but may be set the application needs to use multiple groupings
	 * of ordered messages.
	 * 
	 * @param orderGroup
	 */
	public void setOrderGroup(short orderGroup) {
		this.orderGroup = orderGroup;
	}
	
	/**
	 * @return
	 * 		The order group number differentiating one group from another
	 */
	public short getOrderGroup() {
		return orderGroup;
	}
	
	/**
	 * The orderOriginator is generated when this class is first used in order to define
	 * a universal unique id to be used to globally differentiate one machine from another.
	 * 
	 * @param orderOriginator
	 */
	public void setOrderOriginator(long orderOriginator) {
		this.orderOriginator = orderOriginator;
	}
	
	/**
	 * @return
	 * 		The universal unique identifier for the originating machine.
	 */
	public long getOrderOriginator() {
		return orderOriginator;
	}
	
	public int getMaxTries() {
		return 0;
	}
	
	public static synchronized final long createUniqueId(short orderGroup) {
		if (countGroups == null) countGroups = new HashMap();
		Long count = (Long)countGroups.get(new Short(orderGroup));
		if (count == null) {
			count = new Long(0);
		} else {
			count = new Long(count.longValue() + 1);
		}
		countGroups.put(new Short(orderGroup), count);
		return count.longValue();
	}
	
	private static long createUniversalUniqueId() {
		long id = Math.round(Math.random() * Long.MAX_VALUE);
		return id;
	}
}
