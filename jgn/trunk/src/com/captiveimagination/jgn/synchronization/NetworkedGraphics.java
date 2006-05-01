/**
 * NetworkedGraphics.java
 *
 * Created: Apr 30, 2006
 */
package com.captiveimagination.jgn.synchronization;

/**
 * @author Matthew D. Hicks
 */
public interface NetworkedGraphics {
	public boolean validateSynchronizationMessage(PositionSynchronizationMessage message);
	
	public void applySynchronizationMessage(PositionSynchronizationMessage message);
	
	public PositionSynchronizationMessage createSynchronizationMessage(long objectId);
}
