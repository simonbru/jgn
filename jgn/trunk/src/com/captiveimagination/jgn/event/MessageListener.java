/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn.event;

import com.captiveimagination.jgn.message.*;

/**
 * MessageListener can be added directly to
 * the MessageServer in order to receive events
 * of received messages.
 * 
 * @author Matthew D. Hicks
 */
public interface MessageListener {
    /**
     * The BASIC listener mode simply calls the
     * messageReceived(Message), this is a little
     * bit faster than the other methods.
     */
    public static final int BASIC = 1;
    
    /**
     * The ALL listener mode will call any
     * messageReceived method that this
     * extension of Message can be cast as.
     */
    public static final int ALL = 2;
    
    /**
     * Cycles from top-most class down
     * attempting to find the closest
     * matching method to call.
     */
    public static final int CLOSEST = 3;
    
    /**
     * This method is called when a Message
     * is received.
     * 
     * @param message
     */
    public void messageReceived(Message message);
    
    /**
     * @return
     *      The mode this listener corresponds to.
     *      Uses BASIC, ALL_METHODS, or METHOD.
     */
    public int getListenerMode();
}
