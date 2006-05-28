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
package com.captiveimagination.jgn.core.compression;

/**
 * A named property is not available for the implementation of a
 * {@link CompressionMethod}.<br>
 * s
 * 
 * @author Christian Laireiter
 */
public class UnknownPropertyException extends Exception {

	/**
	 * The compressio method which caused the exception.
	 */
	/**
	 * 
	 */
	private CompressionMethod compressionMethod;

	/**
	 * The name of the invalid property which caused this exception.
	 */
	private String propertyName;

	/**
	 * Creates an instance.<br>
	 * This constructor is not to be used.
	 */
	private UnknownPropertyException() {
		// Nothing to do
	}

	/**
	 * Creates an instance.<br>
	 * 
	 * @param property
	 *            The invalid property.
	 * @param method
	 *            The method which caused the error.
	 */
	public UnknownPropertyException(String property, CompressionMethod method) {
		this.propertyName = property;
		this.compressionMethod = method;
	}

	/**
	 * Returns the compression method which caused the exception.
	 * 
	 * @return the compression method which caused the exception.
	 */
	public CompressionMethod getCompressionMethod() {
		return this.compressionMethod;
	}

	/**
	 * Returns the name of the invalid property.
	 * 
	 * @return the name of the invalid property.
	 */
	public String getPropertyName() {
		return this.propertyName;
	}

}
