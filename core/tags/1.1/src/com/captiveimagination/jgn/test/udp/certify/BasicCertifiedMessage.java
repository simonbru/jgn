package com.captiveimagination.jgn.test.udp.certify;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class BasicCertifiedMessage extends CertifiedMessage {
	public long getResendTimeout() {
		return 5000;
	}

	public int getMaxTries() {
		return 5;
	}
}
