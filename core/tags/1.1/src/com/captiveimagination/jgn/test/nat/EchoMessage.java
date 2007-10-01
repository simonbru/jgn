package com.captiveimagination.jgn.test.nat;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class EchoMessage extends Message {
	private String string;
	
	public void setString(String string) {
		this.string = string;
	}
	
	public String getString() {
		return string;
	}
}
