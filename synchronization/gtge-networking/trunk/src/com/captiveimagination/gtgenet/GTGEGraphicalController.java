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
 * Created: Aug 10, 2006
 */
package com.captiveimagination.gtgenet;

import com.captiveimagination.jgn.synchronization.*;
import com.captiveimagination.jgn.synchronization.message.*;
import com.golden.gamedev.object.*;

/**
 * This is a basic implementation of GraphicalController for the GTGE
 * game engine.
 * 
 * @author Matthew D. Hicks
 */
public class GTGEGraphicalController implements GraphicalController<Sprite> {
    public void applySynchronizationMessage(SynchronizeMessage message, Sprite object) {
        Synchronize2DMessage m = (Synchronize2DMessage)message;
        object.moveTo(10, m.getPositionX(),m.getPositionY(), 5.0);
    }

    public SynchronizeMessage createSynchronizationMessage(Sprite object) {
        Synchronize2DMessage message = new Synchronize2DMessage();
        message.setPositionX((float)object.getX());
        message.setPositionY((float)object.getY());
        return message;
    }


    /**
     * This method will always return 1.0f. It is recommended to override this method
     * in games to provide better efficiency to synchronization.
     */
    public float proximity(Sprite spatial, short playerId) {
        return 1.0f;
    }

    /**
     * This method will always return true. It is recommended to override this method
     * in games to provide a layer of security.
     */
    public boolean validateMessage(SynchronizeMessage message, Sprite spatial) {
        return true;
    }


    public boolean validateCreate(SynchronizeCreateMessage message) {
		return true;
	}


    public boolean validateRemove(SynchronizeRemoveMessage message) {
		return true;
	}
}
