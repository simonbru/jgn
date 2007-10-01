/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn.test.udp.chat;

import com.captiveimagination.jgn.*;

/**
 * @author Matthew D. Hicks
 */
public class Client {
    private String nickname;
    private IP address;
    private int port;
    
    public IP getAddress() {
        return address;
    }
    public void setAddress(IP address) {
        this.address = address;
    }
    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
}
