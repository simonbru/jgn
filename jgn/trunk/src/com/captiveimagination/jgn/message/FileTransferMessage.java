package com.captiveimagination.jgn.message;

/**
 * This message is handled by the FileTransfer class to send a buffered
 * file over with guaranteed delivery and sychronized ordering.
 * 
 * @author Matthew D. Hicks
 */
public class FileTransferMessage extends OrderedMessage {
	private String fileName;
	private String filePath;
	private byte[] bytes;
	private boolean lastMessage;
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public void setLastMessage(boolean lastMessage) {
		this.lastMessage = lastMessage;
	}
	
	public boolean getLastMessage() {
		return lastMessage;
	}
	
	public long getResendTimeout() {
		return 10000;
	}
}
