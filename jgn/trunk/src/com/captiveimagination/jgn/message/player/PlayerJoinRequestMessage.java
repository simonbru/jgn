package com.captiveimagination.jgn.message.player;

/**
 * Should be sent from client to server to request joining of that
 * server. A server should send back a <code>PlayerJoinResponseMessage</code>
 * that tells if the client's request is accepted, and if accepted the playerId
 * assigned to this player.
 * 
 * @author Matthew D. Hicks
 */
public class PlayerJoinRequestMessage extends PlayerMessage {
}
