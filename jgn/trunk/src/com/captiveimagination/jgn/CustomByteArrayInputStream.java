/*
 * Created on Feb 11, 2006
 */
package com.captiveimagination.jgn;

import java.io.*;

/**
 * @author Matthew D. Hicks
 */
public class CustomByteArrayInputStream extends ByteArrayInputStream {
    private int position;
    
    public CustomByteArrayInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
        position = offset;
    }
    
    public int read() {
        position++;
        return super.read();
    }
    
    public int read(byte[] b) throws IOException {
        position += b.length;
        return super.read(b);
    }
    
    public int read(byte[] b, int off, int len) {
        position += len - off;
        return super.read(b, off, len);
    }
    
    public int getPosition() {
        return position;
    }
}
