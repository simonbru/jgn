/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn.test.udp.chat;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class ConnectMessage extends CertifiedMessage {
    private String nickname;
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getNickname() {
        return nickname;
    }

    public long getResendTimeout() {
        return 5000;
    }
    
    public int getMaxTries() {
        return 3;
    }
}
