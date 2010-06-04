package it.unitn.disi.network;

/*
 * Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

import peersim.core.Protocol;
import peersim.vector.SingleValueHolder;

/**
 * Stores a generic object. Adapted from {@link SingleValueHolder}.
 */
public class GenericValueHolder implements Protocol {

	// --------------------------------------------------------------------------
	// Fields
	// --------------------------------------------------------------------------

	/** Value held by this protocol */
	private Object fValue;

	// --------------------------------------------------------------------------
	// Initialization
	// --------------------------------------------------------------------------

	/**
	 * Does nothing.
	 */
	public GenericValueHolder(String prefix) {
	}

	// --------------------------------------------------------------------------

	/**
	 * Clones the value holder.
	 */
	public Object clone() {
		GenericValueHolder gvh;
		try {
			gvh = (GenericValueHolder) super.clone();
		} catch (Exception ex) {
			throw new RuntimeException();
		}
		
		return gvh;
	}

	// --------------------------------------------------------------------------
	// methods
	// --------------------------------------------------------------------------

	public Object getValue() {
		return fValue;
	}

	// --------------------------------------------------------------------------

	public void setValue(Object value) {
		this.fValue = value;
	}

	// --------------------------------------------------------------------------

	/**
	 * Returns the value as a string.
	 */
	public String toString() {
		return fValue.toString();
	}

}
