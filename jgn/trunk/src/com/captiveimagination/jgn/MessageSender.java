/*
 * Created on Nov 30, 2005
 */
package com.captiveimagination.jgn;

/**
 * MessageSender should be implemented by
 * applications utilizing Updater. The
 * MessageSender is used to determine how
 * many times a message should be sent per
 * cycle and sendMessage() is called to
 * send the message.
 * 
 * @author Matthew D. Hicks
 */
public interface MessageSender {
    /**
     * @return
     *      The number of times sendMessage() should be called per updater cycle.
     */
    public int getUpdatesPerCycle();
    
    /**
     * This method is called <code>updatesPerCycle</code> numbers of times per
     * cycle.
     */
    public void sendMessage();
    
    public boolean isEnabled();
}
