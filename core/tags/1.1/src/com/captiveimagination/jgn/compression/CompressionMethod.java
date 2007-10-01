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
package com.captiveimagination.jgn.compression;

/**
 * A &quot;<b>CompressionMethod</b>&quot; is a specific implementation of an
 * compression algorithm.<br>
 * This interface defines methods for adjusting the compression level (provided
 * by some algorithms) as well as compressing data.<br>
 * <br>
 * <br>
 * <b>Information</b>:<br>
 * <br>
 * The properties are specified for adjusting the implemented algorithm. If your
 * implementation does not have anything like that, simple throw an
 * {@link UnknownPropertyException} for each call of
 * {@link #getProperty(String)} and {@link #setProperty(String, Object)}.<br>
 * If your Implementation can only be adjusted using such properties and has no
 * level, try to provide the levels by using a preset for each level.
 * 
 * @author Christian Laireiter
 * @since 1.5
 */
public interface CompressionMethod extends Compressor {

	/**
	 * This method returns the current compression level used by this instance.<br>
	 * If this method does not support compression levels &quot;0&quot; is
	 * returned.<br>
	 * See {@link #getMaximumCompressionLevel()} for more information about
	 * compression levels.<br>
	 * 
	 * @return The current compression level used by the current instance.
	 *         &quot;0&quot; if compression leves are unsupported.
	 */
	int getCompressionLevel();

	/**
	 * If the implementation supports compression levels this method returns the
	 * maximum level available for the implementation.<br>
	 * Compression level starts at &quot;0&quot;. However the &quot;0&quot;
	 * value does not mean that no compression happens. Its the lowest value.<br>
	 * If no compression levels are suppoerted, &quot;0&quot; is always
	 * returned.<br>
	 * The higher the value the more complex the compression execution and
	 * potentially better the compression result.<br>
	 * 
	 * @return The highest available compression level. &quot;0&quot; if no
	 *         compression levels are supported.<br>
	 */
	int getMaximumCompressionLevel();

	/**
	 * Returns an identifier for the implementation (e.g. &quot;Huffman&quot;).<br>
	 * The returned identifier should be unique along all available
	 * implementations in an application.<br>
	 * 
	 * @return Identifier (descriptor) for the implementation.
	 */
	String getMethod();

	/**
	 * Returns a byte sequence which uniquely identifies the implementation.<br>
	 * 
	 * @return unique sequence.
	 */
	byte[] getMethodId();

	/**
	 * This method returns a value for the specified <code>property</code>.<br>
	 * See {@link #setProperty(String, Object)} for more details.
	 * 
	 * @param property
	 *            The name of the property
	 * @return the current value of the property.
	 * @throws UnknownPropertyException
	 *             If the property is unknown to the implementation.
	 */
	Object getProperty(String property) throws UnknownPropertyException;

	/**
	 * This method adjusts the compression level of the current instance to the
	 * specified <code>level</code>.<br>
	 * Valid values are ranging from &quot;0&quot; to the value of
	 * {@link #getMaximumCompressionLevel()}.<br>
	 * <br>
	 * The higher the level, the better are the compression results achieved.<br>
	 * 
	 * @param level
	 *            The level (complexity) of compression to be used.
	 * @throws LevelUnsupportedException
	 *             If the specified level is not supported. (see
	 *             {@link #getMaximumCompressionLevel()}.
	 */
	void setCompressionLevel(int level) throws LevelUnsupportedException;

	/**
	 * Some implementations might support more qualified compression parameters,
	 * which can be adjusted using this method.<br>
	 * 
	 * @param property
	 *            The name of the property to set.<br>
	 *            The used property names should be available using constants in
	 *            the implementation.<br>
	 * @param value
	 *            A value object which can be anything. However it must be
	 *            specified by the implementation.
	 * @throws UnknownPropertyException
	 *             If the propertyname is not available at the implementation.
	 * @throws InvalidPropertyValueException
	 *             If the specified <code>value</code> does not comply to the
	 *             properties value definition.
	 */
	void setProperty(String property, Object value)
			throws UnknownPropertyException, InvalidPropertyValueException;
}
