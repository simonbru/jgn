package com.captiveimagination.jgn.message;

/**
 * The response from a ping request. This never need to be instantiated
 * manually. Simply call <code>long MessageServer.ping(InetAddress address, int port, long timeout)</code>
 * where the address and port refer to a MessageServer.
 * 
 * @author Matthew D. Hicks
 */
public class PongMessage extends Message {
}
