package com.captiveimagination.jgn.test;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.peer.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class DebugMessageListener implements MessageListener {
	private String ident;
	
	public DebugMessageListener(String ident) {
		this.ident = ident;
	}
	
	public void messageReceived(Message message) {
		System.out.println(ident + ": received message " + message.getClass().getName());
	}
	
	public void messageReceived(BasicMessage message) {
		System.out.println(ident + ": received BasicMessage: " + message.getText());
	}
	
	public void messageReceived(PeerMessage message) {
		System.out.println(ident + ": received peer message " + message.getClass().getName() + ": " + message.getPeerId() + ": " + PeerMessage.getRequestType(message.getRequestType()));
	}

	public int getListenerMode() {
		return MessageListener.CLOSEST;
	}
}
