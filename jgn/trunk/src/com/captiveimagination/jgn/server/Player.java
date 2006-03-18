package com.captiveimagination.jgn.server;

import com.captiveimagination.jgn.*;

public interface Player {
	public short getPlayerId();
	
	public IP getAddress();
	
    public int getUDPPort();
    
	public int getTCPPort();
	
    public int getPlayerPort();
    
	public long getLastHeardFrom();
	
	public void heardFrom();
}
