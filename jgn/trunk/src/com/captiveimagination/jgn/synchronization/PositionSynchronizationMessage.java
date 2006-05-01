/**
 * PositionSynchronizationMessage.java
 *
 * Created: Apr 30, 2006
 */
package com.captiveimagination.jgn.synchronization;

import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.player.*;

/**
 * PositionSynchronizationMessage is the basic foundational message
 * for synchronization of the graphical position of an object in a
 * scenegraph.
 * 
 * @author Matthew D. Hicks
 */
public class PositionSynchronizationMessage extends Message implements PlayerMessage {
	private short playerId;
	private long objectId;
	private float positionX;
	private float positionY;
	private float positionZ;
	private float rotationX;
	private float rotationY;
	private float rotationZ;
	
	public long getObjectId() {
		return objectId;
	}
	public void setObjectId(long objectId) {
		this.objectId = objectId;
	}
	public float getPositionX() {
		return positionX;
	}
	public void setPositionX(float positionX) {
		this.positionX = positionX;
	}
	public float getPositionY() {
		return positionY;
	}
	public void setPositionY(float positionY) {
		this.positionY = positionY;
	}
	public float getPositionZ() {
		return positionZ;
	}
	public void setPositionZ(float positionZ) {
		this.positionZ = positionZ;
	}
	public float getRotationX() {
		return rotationX;
	}
	public void setRotationX(float rotationX) {
		this.rotationX = rotationX;
	}
	public float getRotationY() {
		return rotationY;
	}
	public void setRotationY(float rotationY) {
		this.rotationY = rotationY;
	}
	public float getRotationZ() {
		return rotationZ;
	}
	public void setRotationZ(float rotationZ) {
		this.rotationZ = rotationZ;
	}
	public void setPlayerId(short playerId) {
		this.playerId = playerId;
	}
	public short getPlayerId() {
		return playerId;
	}
}
