/**
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created: Jun 10, 2006
 */
package com.captiveimagination.jgn.event;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.message.*;

/**
 * This listener gets added by default to every new MessageServer as a MessageListener
 * and ConnectionListener and handles internal event handling. Removing the InternalListener
 * from a MessageServer is not a good idea unless you have replicated all functionality
 * contained within this listener.
 * 
 * This is a singleton object.
 * 
 * @author Matthew D. Hicks
 */
public class InternalListener implements MessageListener, ConnectionListener {
	private static InternalListener instance;
	
	private InternalListener() {
	}
	
	public void messageReceived(Message message) {
		if (message instanceof LocalRegistrationMessage) {
			LocalRegistrationMessage m = (LocalRegistrationMessage)message;
			String[] messages = m.getMessageClasses();
			short[] ids = m.getIds();
			int i = 0;
			try {
				for (; i < messages.length; i++) {
					message.getMessageClient().register(ids[i], (Class<? extends Message>)Class.forName(messages[i]));
				}
				message.getMessageClient().setStatus(MessageClient.STATUS_CONNECTED);
			} catch(ClassNotFoundException exc) {
				System.err.println("Unable to find the message: " + messages[i] + " in the ClassLoader. Trace follows:");
				// TODO handle more gracefully
				throw new RuntimeException(exc);
			}
		}
	}

	public void messageSent(Message message) {
	}
	
	public static final InternalListener getInstance() {
		if (instance == null) {
			instance = new InternalListener();
		}
		return instance;
	}

	public void connected(MessageClient client) {
		// Send the registration message
		client.sendMessage(JGN.generateRegistrationMessage());
	}

	public void negotiationComplete(MessageClient client) {	
	}

	public void disconnected(MessageClient client) {
	}
}
