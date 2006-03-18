package com.captiveimagination.jgn.event;

import java.io.*;
import java.util.*;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 *
 */
public abstract class FileTransferListener implements MessageListener {
	private HashMap transfers;
	
	public FileTransferListener() {
		transfers = new HashMap();
	}
	
	public void messageReceived(Message message) {
	}
	
	public void messageReceived(FileTransferMessage message) throws IOException {
		FileOutputStream fos = (FileOutputStream)transfers.get(message.getFilePath() + ":" + message.getFileName());
		if (fos == null) {
			File file = startFileTransfer(message.getFileName(), message.getFilePath());
			if (file == null) return;
			fos = new FileOutputStream(file);
			transfers.put(message.getFilePath() + ":" + message.getFileName(), fos);
		}
		fos.write(message.getBytes());
		if (message.getLastMessage()) {
			fos.flush();
			fos.close();
			transfers.remove(message.getFilePath() + ":" + message.getFileName());
            endFileTransfer(message.getFileName(), message.getFilePath());
		}
	}
	
	public abstract File startFileTransfer(String fileName, String filePath);

    public abstract void endFileTransfer(String fileName, String filePath);
    
	public int getListenerMode() {
		return MessageListener.CLOSEST;
	}
}
