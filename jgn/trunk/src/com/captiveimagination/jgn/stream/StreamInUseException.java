/**
 * StreamInUseException.java
 *
 * Created: May 28, 2006
 */
package com.captiveimagination.jgn.stream;

import java.io.*;

/**
 * StreamInUseException is thrown when a stream with the same
 * streamId has already been requested for input or output
 * without closing the first.
 * 
 * @author Matthew D. Hicks
 */
public class StreamInUseException extends IOException {
	private static final long serialVersionUID = 1L;
	
	public StreamInUseException() {
		super();
	}
	
	public StreamInUseException(String s) {
		super(s);
	}
}
