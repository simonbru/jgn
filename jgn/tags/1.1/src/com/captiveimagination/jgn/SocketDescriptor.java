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
package com.captiveimagination.jgn;

/**
 * Since &quot;JGN&quot; has its own {@link IP} representation, it is helpful to
 * have an own socket representation as well.<br>
 * This class takes an {@link IP} object as well as a port number.<br>
 * The most important is: it implements {@link Object#equals(Object)} and
 * {@link Object#hashCode()} for the use with collections and hashing mechanism.<br>
 * 
 * @author Christian Laireiter
 */
public class SocketDescriptor {

	/**
	 * Stores the IP of the represented socket.<br>
	 */
	private IP ip;

	/**
	 * Stores the port number of the represented socket.<br>
	 */
	private int port;

	/**
	 * Creates an instance.
	 * 
	 * @param socketIP
	 *            The IP of the socket
	 * @param SocketPort
	 *            The port of the socket.
	 */
	public SocketDescriptor(IP socketIP, int SocketPort) {
		this.ip = socketIP;
		this.port = SocketPort;
	}

	/**
	 * (overridden)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof SocketDescriptor) {
			SocketDescriptor other = (SocketDescriptor) obj;
			return this.ip.equals(other.ip) && this.port == other.port;
		}
		return false;
	}

	/**
	 * Returns the {@link IP} of the socket.
	 * 
	 * @return the {@link IP} of the socket.
	 */
	public IP getIp() {
		return ip;
	}

	/**
	 * Returns the port number of the socket.
	 * 
	 * @return the port number of the socket.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * (overridden)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return ip.hashCode() * 31 + port;
	}
}
