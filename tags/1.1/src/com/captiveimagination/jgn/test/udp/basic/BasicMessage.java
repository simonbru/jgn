/*
 * Created on Nov 30, 2005
 */
package com.captiveimagination.jgn.test.udp.basic;

import com.captiveimagination.jgn.message.*;

/**
 * This is a very basic message. Note that
 * the only thing explicitly that needed to
 * be defined was the abstract getType() method
 * which is used to determine the message type
 * when received on the remote server. The
 * message type MUST be unique from other
 * messages.
 * 
 * Notice that this is a simple bean that
 * we just add a getText setText method to
 * and it gets passed to the recipient.
 * You can any primitives, primitive wrapper,
 * or String. No other types are currently
 * supported as this is meant to be a very
 * fast transfer protocol.
 * 
 * @author Matthew D. Hicks
 */
public class BasicMessage extends Message {
    private String text;
    private int[] numbers;
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getText() {
        return text;
    }
    
    public void setNumbers(int[] numbers) {
        this.numbers = numbers;
    }
    
    public int[] getNumbers() {
        return numbers;
    }
}
