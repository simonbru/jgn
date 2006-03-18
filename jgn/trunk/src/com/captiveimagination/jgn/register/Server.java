/*
 * Created on Jan 23, 2006
 */
package com.captiveimagination.jgn.register;

/**
 * @author Matthew D. Hicks
 */
public class Server {
    private String serverName;
    private String host;
    private byte[] address;
    private int portUDP;
    private int portTCP;
    private String game;
    private String map;
    private String info;
    private int players;
    private int maxPlayers;
    
    public Server() {
    }
    
    public Server(String serverName, String host, byte[] address, int portUDP, int portTCP, String game, String map, String info, int players, int maxPlayers) {
        this.serverName = serverName;
        this.host = host;
        this.address = address;
        this.portUDP = portUDP;
        this.portTCP = portTCP;
        this.game = game;
        this.map = map;
        this.info = info;
        this.players = players;
        this.maxPlayers = maxPlayers;
    }
    
    public byte[] getAddress() {
        return address;
    }
    public void setAddress(byte[] address) {
        this.address = address;
    }
    public String getGame() {
        return game;
    }
    public void setGame(String game) {
        this.game = game;
    }
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public String getInfo() {
        return info;
    }
    public void setInfo(String info) {
        this.info = info;
    }
    public String getMap() {
        return map;
    }
    public void setMap(String map) {
        this.map = map;
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
    public String getServerName() {
        return serverName;
    }
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    public int getPlayers() {
        return players;
    }
    public void setPlayers(int players) {
        this.players = players;
    }
    public int getMaxPlayers() {
        return maxPlayers;
    }
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}
