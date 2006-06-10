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
 * Created: Jun 10, 2006
 */
package com.captiveimagination.jgn;

import java.util.*;

import com.captiveimagination.jgn.convert.*;
import com.captiveimagination.jgn.message.*;

/**
 * Foundational static class for various functionality that is abstract from any other
 * specific class but is necessary for the JGN project.
 * 
 * @author Matthew D. Hicks
 */
public class JGN {
	private static final HashMap<Short,Class<? extends Message>> registry = new HashMap<Short,Class<? extends Message>>();
	private static final HashMap<Class<? extends Message>,ConversionHandler> converters = new HashMap<Class<? extends Message>,ConversionHandler>();
	static {
		// Certain messages must be known before negotiation so this is explicitly done here
		register(LocalRegistrationMessage.class, (short)0);
	}
	
	/**
	 * Messages must be registered via this method preferrably before any communication
	 * occurs for efficiency in the initial connectivity negotiation process.
	 * 
	 * @param c
	 */
	public static final void register(Class<? extends Message> c) {
		if (registry.containsValue(c)) {
			return;
		}
		
		short id = generateId();
        while (registry.containsKey(id)) {
        	id = generateId();
        }
        register(c, id);
	}
	
	private static final void register(Class<? extends Message> c, short id) {
		converters.put(c, ConversionHandler.getConversionHandler(c));
        registry.put(id, c);
	}
	
	private static final short generateId() {
		short id = (short)Math.round(Math.random() * Short.MAX_VALUE);
        id += Math.round(Math.random() * Short.MIN_VALUE);
        return id;
	}
	
	/**
	 * Request the ConversionHandler associated with this Message class.
	 * 
	 * @param c
	 * @return
	 * 		ConversionHandler associated with the Class <code>c</code>
	 * 		if the Message class referenced has not be registered yet
	 * 		<code>null</code> will be returned.
	 */
	public static final ConversionHandler getConverter(Class<? extends Message> c) {
		return converters.get(c);
	}
}
