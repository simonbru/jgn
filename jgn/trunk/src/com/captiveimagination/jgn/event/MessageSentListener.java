/*
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
 */
package com.captiveimagination.jgn.event;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.message.*;

/**
 * MessageSentListener can be added directly to
 * the MessageServer in order to receive events
 * of sent messages.
 * 
 * @author Matthew D. Hicks
 */
public interface MessageSentListener {
    /**
     * The BASIC listener mode simply calls the
     * messageReceived(Message), this is a little
     * bit faster than the other methods.
     */
    public static final int BASIC = 1;
    
    /**
     * The ALL listener mode will call any
     * messageReceived method that this
     * extension of Message can be cast as.
     */
    public static final int ALL = 2;
    
    /**
     * Cycles from top-most class down
     * attempting to find the closest
     * matching method to call.
     */
    public static final int CLOSEST = 3;
    
    /**
     * This method is called when a Message
     * is sent.
     * 
     * @param message
     */
    public void messageSent(Message message);
    
    /**
     * @return
     *      The mode this listener corresponds to.
     *      Uses BASIC, ALL, or CLOSEST.
     */
    public int getListenerMode();
}
