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
