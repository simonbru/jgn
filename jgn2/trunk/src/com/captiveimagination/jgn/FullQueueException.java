/*
 * Created on 6-jun-2006
 */

package com.captiveimagination.jgn;

public class FullQueueException extends RuntimeException {
	public FullQueueException() {
		super();
	}

	public FullQueueException(String msg) {
		super(msg);
	}

	public FullQueueException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public FullQueueException(Throwable cause) {
		super(cause);
	}
}
