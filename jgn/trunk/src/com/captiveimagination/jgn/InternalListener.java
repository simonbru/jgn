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
package com.captiveimagination.jgn;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.type.*;

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
class InternalListener implements MessageListener, ConnectionListener {
	private static InternalListener instance;

	private InternalListener() {
	}

	public static final InternalListener getInstance() {
			if (instance == null) {
				instance = new InternalListener();
			}
			return instance;
		}

	@SuppressWarnings({"unchecked"})
	public void messageReceived(Message message) {
		MessageClient myClient = message.getMessageClient();

		if (message instanceof LocalRegistrationMessage) {

			// Verify if connection is valid
			String filterMessage = myClient.getMessageServer().shouldFilterConnection(myClient);
			if (filterMessage != null) {
				// They have been filtered, so we kick them
				myClient.kick(filterMessage);
				return;
			}

			// Handle incoming negotiation information
			/* [ase] this message holds the Message NAMES and their MessageIds
			*  as they are used on the other side of the 'wire'. We have to store
			*  them within the MC, because they will differ from mine!
			* [/ase]
			*/
			LocalRegistrationMessage m = (LocalRegistrationMessage)message;
			myClient.setId(m.getId());
			String[] messages = m.getMessageClasses();
			short[] ids = m.getIds();

			int i = 0;
			try {
				for (; i < messages.length; i++) {
					myClient.register(ids[i], (Class<? extends Message>)Class.forName(messages[i]));
				}
				// transfer ok, put client into CONNECTED state, and prepare to notify ConnectionListeners
				myClient.setStatus(MessageClient.Status.CONNECTED);
				myClient.getMessageServer().getNegotiatedConnectionQueue().add(myClient);

			} catch (ClassNotFoundException exc) {
				System.err.println("Unable to find the message: " + messages[i]
												+ " in the ClassLoader. Trace follows:");
				// TODO handle more gracefully
				throw new RuntimeException(exc);
			}

			if (!myClient.hasSentRegistration()) {
				myClient.getMessageServer().getConnectionController().negotiate(myClient);
			}

		} else if (message instanceof DisconnectMessage) {
			// Disconnect from the remote client
			myClient.setKickReason(((DisconnectMessage)message).getReason());
			myClient.setStatus(MessageClient.Status.DISCONNECTING);
		}

		if (message instanceof CertifiedMessage) {
			// Send back a message to the sender to let them know the message was received
			Receipt receipt = new Receipt();
			receipt.setCertifiedId(message.getId());
			myClient.sendMessage(receipt);

		} else if (message instanceof Receipt) {
			// Received confirmation of a CertifiedMessage
			myClient.certifyMessage(((Receipt)message).getCertifiedId());
		}
	}

	public void messageSent(Message message) {
		if (message instanceof CertifiedMessage) {
			message.getMessageClient().getCertifiableMessageQueue().add(message);
		}
	}

	public void messageCertified(Message message) {
	}

	public void messageFailed(Message message) {
	}


	public void connected(MessageClient client) {
	}

	public void negotiationComplete(MessageClient client) {
	}

	public void disconnected(MessageClient client) {
	}

	public void kicked(MessageClient client, String reason) {
	}
}
