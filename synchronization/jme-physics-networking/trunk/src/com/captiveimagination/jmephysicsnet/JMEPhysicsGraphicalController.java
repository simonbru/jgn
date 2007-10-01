/**
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created: Jul 29, 2006
 */
package com.captiveimagination.jmephysicsnet;

import com.captiveimagination.jgn.synchronization.*;
import com.captiveimagination.jgn.synchronization.message.*;
import com.jme.math.*;
import com.jmex.physics.*;

/**
 * This is a basic implementation of the GraphicalControler for the
 * jME-Physics project.
 * 
 * @author Matthew D. Hicks
 */
public class JMEPhysicsGraphicalController implements GraphicalController<DynamicPhysicsNode> {
	private Vector3f store;
	
	public JMEPhysicsGraphicalController() {
		store = new Vector3f();
	}
	
    public void applySynchronizationMessage(SynchronizeMessage message, DynamicPhysicsNode dpn) {
        SynchronizePhysicsMessage m = (SynchronizePhysicsMessage)message;
        dpn.setLocalTranslation(new Vector3f(m.getPositionX(), m.getPositionY(), m.getPositionZ()));
        dpn.setLocalRotation(new Quaternion(m.getRotationX(), m.getRotationY(), m.getRotationZ(), m.getRotationW()));
        
        dpn.setLinearVelocity(new Vector3f(m.getLinearVelocityX(), m.getLinearVelocityY(), m.getLinearVelocityZ()));
        dpn.setAngularVelocity(new Vector3f(m.getAngularVelocityX(), m.getAngularVelocityY(), m.getAngularVelocityZ()));
    }

    public SynchronizeMessage createSynchronizationMessage(DynamicPhysicsNode dpn) {
        SynchronizePhysicsMessage message = new SynchronizePhysicsMessage();
        message.setPositionX(dpn.getLocalTranslation().x);
        message.setPositionY(dpn.getLocalTranslation().y);
        message.setPositionZ(dpn.getLocalTranslation().z);
        message.setRotationX(dpn.getLocalRotation().x);
        message.setRotationY(dpn.getLocalRotation().y);
        message.setRotationZ(dpn.getLocalRotation().z);
        message.setRotationW(dpn.getLocalRotation().w);
        
        dpn.getLinearVelocity(store);
        message.setLinearVelocityX(store.x);
        message.setLinearVelocityY(store.y);
        message.setLinearVelocityZ(store.z);
        dpn.getAngularVelocity(store);
        message.setAngularVelocityX(store.x);
        message.setAngularVelocityY(store.y);
        message.setAngularVelocityZ(store.z);
        
        return message;
    }

    /**
     * This method will always return 1.0f. It is recommended to override this method
     * in games to provide better efficiency to synchronization.
     */
    public float proximity(DynamicPhysicsNode dpn, short playerId) {
        return 1.0f;
    }

    /**
     * This method will always return true. It is recommended to override this method
     * in games to provide a layer of security.
     */
    public boolean validateMessage(SynchronizeMessage message, DynamicPhysicsNode dpn) {
        return true;
    }


	public boolean validateCreate(SynchronizeCreateMessage message) {
		return true;
	}


	public boolean validateRemove(SynchronizeRemoveMessage message) {
		return true;
	}
}