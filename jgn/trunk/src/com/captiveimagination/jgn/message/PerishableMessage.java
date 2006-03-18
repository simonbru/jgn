/*
 * Created on Dec 8, 2005
 */
package com.captiveimagination.jgn.message;

/**
 * PerishableMessage provides a convenient way to handle
 * messages that are only valid within a certain time frame.
 * Internally messages are given a timestamp on send and the
 * remote receiver of the message is instructed to discard
 * the message if the expiration has elapsed when it gets
 * around to using it.
 * 
 * @author Matthew D. Hicks
 */
public abstract class PerishableMessage extends Message { 
    /**
     * @return
     *      The expiration time in milliseconds. This is the
     *      amount of time in milliseconds the message should
     *      be considered valid and if read after that amount
     *      of time has elapsed since creation, it is discarded.
     */
    public abstract long getExpiration();
    
    public boolean isExpired() {
        if (getSentStamp() + getExpiration() < System.currentTimeMillis()) {
            return true;
        }
        return false;
    }
}
