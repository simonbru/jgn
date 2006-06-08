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
 * Created: Jun 7, 2006
 */
package com.captiveimagination.jgn;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;

/**
 * @author Matthew D. Hicks
 */
public class TCPMessageServer extends MessageServer {
	private Selector selector;
	
	public TCPMessageServer(InetSocketAddress address) throws IOException {
		super(address);
		selector = Selector.open();
		
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().bind(address);
		ssc.configureBlocking(false);
		ssc.register(selector, SelectionKey.OP_ACCEPT);
	}

	public MessageClient connect(InetSocketAddress address) {
		return null;
	}

	public void sendMessage(MessageClient client, Message message) {
	}

	public boolean disconnect(MessageClient client) {
		return false;
	}

	public void close() {
	}
	
	public void updateTraffic() throws IOException {
		int selectedKeys = selector.selectNow();
		System.out.println("server->selectedKeys=" + selectedKeys);

		Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
		while (keys.hasNext()) {
			SelectionKey activeKey = keys.next();
			keys.remove();
			
			if (activeKey.isAcceptable()) {
				accept(activeKey.channel());
			} else if (activeKey.isReadable()) {
				read(activeKey.channel());
			} else if (activeKey.isWritable()) {
				write(activeKey.channel());
			}
		}
	}
	
	private void accept(SelectableChannel channel) {
		// TODO accept connection
		// TODO call off to ConnectionListener
	}
	
	private void read(SelectableChannel channel) {
		// TODO read the incoming message
	}
	
	private void write(SelectableChannel channel) {
		// TODO writable...
	}
}
