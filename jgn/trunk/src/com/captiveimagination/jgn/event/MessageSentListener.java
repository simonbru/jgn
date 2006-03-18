/*
 * Created on Feb 4, 2005
 */
package com.captiveimagination.jgn.event;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.message.*;

/**
 * MessageSentListener can be added directly to
 * the MessageServer in order to receive events
 * of sent messages.
 * 
 * @author Matthew D. Hicks
 */
public interface MessageSentListener {
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
     * is sent.
     * 
     * @param message
     */
    public void messageSent(Message message, MessageServer sendingServer);
    
    /**
     * @return
     *      The mode this listener corresponds to.
     *      Uses BASIC, ALL, or CLOSEST.
     */
    public int getListenerMode();
}
