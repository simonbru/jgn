/*
 * Created on Jan 23, 2006
 */
package com.captiveimagination.jgn.register.message;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class UnregisterServerMessage extends CertifiedMessage {
    private byte[] address;
    private int portUDP;
    private int portTCP;
    
    public UnregisterServerMessage() {
    }
    
    public UnregisterServerMessage(byte[] address, int portUDP, int portTCP) {
        this.address = address;
        this.portUDP = portUDP;
        this.portTCP = portTCP;
    }
    
    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public int getPortUDP() {
        return portUDP;
    }

    public void setPortUDP(int portUDP) {
        this.portUDP = portUDP;
    }
    
    public int getPortTCP() {
        return portTCP;
    }

    public void setPortTCP(int portTCP) {
        this.portTCP = portTCP;
    }

    public long getResendTimeout() {
        return 1000;
    }

    public int getMaxTries() {
        return 5;
    }
}
