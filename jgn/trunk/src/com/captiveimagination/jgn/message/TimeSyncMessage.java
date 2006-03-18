package com.captiveimagination.jgn.message;

/**
 * @author Matthew D. Hicks
 *
 */
public class TimeSyncMessage extends CertifiedMessage {
	private long localTime;
	private long remoteTime;
	
	public long getLocalTime() {
		return localTime;
	}

	public void setLocalTime(long localTime) {
		this.localTime = localTime;
	}

	public long getRemoteTime() {
		return remoteTime;
	}

	public void setRemoteTime(long remoteTime) {
		this.remoteTime = remoteTime;
	}

	public long getResendTimeout() {
		return 1000;
	}
	
	public int getMaxTries() {
		return 3;
	}
}
