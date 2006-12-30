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
 * Created: Jul 14, 2006
 */
package com.captiveimagination.jgn.clientserver;

import java.io.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.type.*;

/**
 * @author Matthew D. Hicks
 */
public class JGNDirectConnection implements JGNConnection {
	private short playerId;
	private MessageClient reliableClient;
	private MessageClient fastClient;
	
	public JGNDirectConnection() {
		playerId = -1;
	}
	
	public void setPlayerId(short playerId) {
		this.playerId = playerId;
	}
	
	public short getPlayerId() {
		return playerId;
	}

	public MessageClient getFastClient() {
		return fastClient;
	}

	public void setFastClient(MessageClient fastClient) {
		this.fastClient = fastClient;
	}

	public MessageClient getReliableClient() {
		return reliableClient;
	}

	public void setReliableClient(MessageClient reliableClient) {
		this.reliableClient = reliableClient;
	}
	
	public boolean isConnected() {
		if ((reliableClient != null) && (!reliableClient.isConnected())) {
			return false;
		} else if ((fastClient != null) && (!fastClient.isConnected())) {
			return false;
		} else if ((reliableClient == null) && (fastClient == null)) {
			return false;
		}
		return true;
	}

	public void disconnect() throws IOException {
		if (reliableClient != null) {
			reliableClient.disconnect();
		}
		if (fastClient != null) {
			fastClient.disconnect();
		}
	}
	
	public void sendMessage(Message message) {
		if (message.getPlayerId() == -1) {
			message.setPlayerId(getPlayerId());
		}
		if ((message instanceof CertifiedMessage) && (reliableClient != null)) {
			reliableClient.sendMessage(message);
		} else if (fastClient != null) {
			fastClient.sendMessage(message);
		} else {
			// ase -- this may throw NPE
			// reliableClient.sendMessage(message);
			// -- ase
			if (reliableClient != null)
				reliableClient.sendMessage(message);
		}
	}
}
