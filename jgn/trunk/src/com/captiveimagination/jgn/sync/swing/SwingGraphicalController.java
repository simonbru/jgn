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
package com.captiveimagination.jgn.sync.swing;

import java.awt.*;

import com.captiveimagination.jgn.sync.*;
import com.captiveimagination.jgn.sync.message.*;

/**
 * This is an example implementation of the GraphicalController for use
 * with Swing. The objects specified are JPanels.
 * 
 * @author Matthew D. Hicks
 */
public class SwingGraphicalController implements GraphicalController<Component> {
	public void applySynchronizationMessage(SynchronizeMessage message, Component component) {
		Synchronize2DMessage m = (Synchronize2DMessage)message;
		component.setBounds((int)m.getPositionX(), (int)m.getPositionY(), 50, 50);
	}

	public SynchronizeMessage createSynchronizationMessage(Component component) {
		Synchronize2DMessage message = new Synchronize2DMessage();
		message.setPositionX(component.getX());
		message.setPositionY(component.getY());
		return message;
	}

	public float proximity(Component component, short playerId) {
		return 1.0f;
	}

	public boolean validateMessage(SynchronizeMessage message, Component component) {
		return true;
	}
}
