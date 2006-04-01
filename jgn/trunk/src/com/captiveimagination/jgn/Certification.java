package com.captiveimagination.jgn;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class Certification {
	private CertifiedMessage message;
	private IP recipientAddress;
	private int recipientPort;
	private long lastTry;
	private int retryCount;
	private long certificationId;
	
	public Certification(CertifiedMessage message, IP recipientAddress, int recipientPort) {
		this.message = message;
		this.recipientAddress = recipientAddress;
		this.recipientPort = recipientPort;
		this.certificationId = message.getId();
		tried();
		retryCount = 0;
	}
	
	public CertifiedMessage getMessage() {
		return message;
	}
	
	public IP getRecipientAddress() {
		return recipientAddress;
	}
	
	public int getRecipientPort() {
		return recipientPort;
	}
	
	public long getLastTry() {
		return lastTry;
	}
	
	public void tried() {
		lastTry = System.currentTimeMillis();
		retryCount++;
		message.failed();
	}

	/**
	 * @return the certificationId
	 */
	public long getCertificationId() {
		return this.certificationId;
	}

	/**
	 * @return the retryCount
	 */
	public int getRetryCount() {
		return this.retryCount;
	}
}