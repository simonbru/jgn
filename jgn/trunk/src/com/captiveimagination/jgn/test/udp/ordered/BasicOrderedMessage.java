package com.captiveimagination.jgn.test.udp.ordered;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class BasicOrderedMessage extends OrderedMessage {
	private String value;
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public long getResendTimeout() {
		return 10000;
	}
}
