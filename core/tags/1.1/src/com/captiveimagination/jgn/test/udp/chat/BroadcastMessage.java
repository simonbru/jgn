/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn.test.udp.chat;

import com.captiveimagination.jgn.message.*;

/**
 * Simple ChatMessage Test
 * 
 * @author Matthew D. Hicks
 */
public class BroadcastMessage extends Message {
    private String message;
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
}
