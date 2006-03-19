package com.captiveimagination.jgn.server;

import com.captiveimagination.jgn.*;

public interface JGNPlayer {
	public short getPlayerId();
	
	public IP getAddress();
	
    public int getUDPPort();
    
    public void setUDPPort(int port);
    
	public int getTCPPort();
    
    public void setTCPPort(int port);
	
    public int getPlayerPort();
    
	public long getLastHeardFrom();
	
	public void heardFrom();
}
