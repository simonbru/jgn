/*
 * Created on Jan 23, 2006
 */
package com.captiveimagination.jgn.register.message;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class RequestServersMessage extends CertifiedMessage {
    private String filter;
    
    public RequestServersMessage() {
    }
    
    public RequestServersMessage(String filter) {
        this.filter = filter;
    }
    
    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public long getResendTimeout() {
        return 1000;
    }

    public int getMaxTries() {
        return 5;
    }
}
