package com.captiveimagination.jgn.so;

import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.type.*;

public class ObjectUpdateMessage extends Message implements CertifiedMessage {
	private String[] fields;
	private byte[] data;

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public String[] getFields() {
		return fields;
	}

	public void setFields(String[] fields) {
		this.fields = fields;
	}
}
