/*
 * Created on Dec 19, 2005
 */
package com.captiveimagination.jgn;

import java.io.*;

import com.captiveimagination.jgn.message.*;

public interface MessageHandler {
    public Message receiveMessage(DataInputStream dis) throws IOException;
    
    public void sendMessage(Message message, DataOutputStream dos) throws IOException;
}
