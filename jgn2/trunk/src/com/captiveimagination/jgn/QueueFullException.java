/*
 * Created on 6-jun-2006
 */

package com.captiveimagination.jgn;

/**
 * @author Skip M. B. Balk
 */
public class QueueFullException extends RuntimeException {
	public QueueFullException() {
		super();
	}

	public QueueFullException(String msg) {
		super(msg);
	}

	public QueueFullException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public QueueFullException(Throwable cause) {
		super(cause);
	}
}
