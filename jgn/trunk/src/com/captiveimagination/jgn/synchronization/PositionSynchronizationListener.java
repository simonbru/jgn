package com.captiveimagination.jgn.synchronization;

import java.io.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

public class PositionSynchronizationListener implements MessageListener {
	private NetworkedGraphics ng;
	
	public PositionSynchronizationListener(NetworkedGraphics ng) {
		this.ng = ng;
	}
	
	public void messageReceived(Message message) {
	}
	
	public void messageReceived(PositionSynchronizationMessage message) {
		if (ng.validateSynchronizationMessage(message)) {
			ng.applySynchronizationMessage(message);
		} else {
			PositionSynchronizationMessage psm = ng.createSynchronizationMessage(message.getId());
			try {
				message.getMessageServer().sendMessage(psm, message.getRemoteAddress(), message.getRemotePort());
			} catch(IOException exc) {
				// TODO handle in exception handler
				exc.printStackTrace();
			}
		}
	}

	public int getListenerMode() {
		return MessageListener.CLOSEST;
	}

}
