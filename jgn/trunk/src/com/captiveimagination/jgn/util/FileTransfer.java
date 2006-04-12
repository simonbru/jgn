/*
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
 */
package com.captiveimagination.jgn.util;

import java.io.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.message.*;

/**
 * Simple utility to send a file over JGN with guaranteed delivery,
 * guaranteed order, and over UDP.
 * 
 * @author Matthew D. Hicks
 */
public class FileTransfer {
	private File file;
	private int bufferLength;
	private short group;
	
	public FileTransfer(File file, int bufferLength) {
		this.file = file;
		this.bufferLength = bufferLength;
		group = (short)Math.round(Math.random() * Short.MAX_VALUE);
	}
	
	public void transfer(MessageServer server, IP remoteAddress, int remotePort) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		FileTransferMessage message;
		String name = file.getName();
		String path = file.getPath();
		byte[] buf = new byte[bufferLength];
		int len;
		while ((len = fis.read(buf)) != -1) {
			message = new FileTransferMessage();
			message.setFileName(name);
			message.setFilePath(path);
			message.setOrderGroup(group);
			if (bufferLength > len) {
				byte[] tmp = new byte[len];
				System.arraycopy(buf, 0, tmp, 0, len);
				message.setBytes(tmp);
			} else {
				message.setBytes(buf);
			}
			server.sendMessage(message, remoteAddress, remotePort);
			buf = new byte[bufferLength];
		}
        message = new FileTransferMessage();
        message.setFileName(name);
        message.setFilePath(path);
        message.setBytes(new byte[0]);
        message.setLastMessage(true);
        message.setOrderGroup(group);
        server.sendMessage(message, remoteAddress, remotePort);
        fis.close();
	}
}
