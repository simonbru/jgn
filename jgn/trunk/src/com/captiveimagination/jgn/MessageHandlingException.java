/*
 * Created on 20-jun-2006
 */

package com.captiveimagination.jgn;

import com.captiveimagination.jgn.message.Message;

public class MessageHandlingException extends Exception {
	private static final long serialVersionUID = 1L;

	public MessageHandlingException(String msg) {
		this(msg, null, null);
	}

	public MessageHandlingException(String msg, Message failed) {
		this(msg, failed, null);
	}

	public MessageHandlingException(String msg, Message failed, Throwable cause) {
		super(msg, cause);

		this.failed = failed;
	}

	private final Message failed;

	public final Message getFailedMessage() {
		return failed;
	}
}
