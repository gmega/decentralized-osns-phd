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

package it.unitn.disi.utils.peersim;

import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.OracleIdleProtocol;
import peersim.core.Protocol;

/**
 * Based on {@link OracleIdleProtocol}, but modified for a fast
 * {@link Linkable#contains(Node)} operation.
 */
public final class FastOracleLinkable implements Protocol, Linkable {

	private static final FastOracleLinkable fInstance = new FastOracleLinkable(null);
	
	public static FastOracleLinkable singletonInstance() {
		return fInstance;
	}
	
	// =================== initialization, creation ======================
	// ===================================================================

	/** Does nothing */
	public FastOracleLinkable(String prefix) { }

	// --------------------------------------------------------------------

	/** Returns <tt>this</tt> to maximize memory saving. It contains no fields. */
	public Object clone() {
		return this;
	}

	// ===================== public methods ===============================
	// ====================================================================

	/**
	 * Cheap operation using {@link NodeRegistry}.
	 */
	public boolean contains(Node n) {
		return NodeRegistry.getInstance().contains(n.getID());
	}

	// --------------------------------------------------------------------

	/** Unsupported operation */
	public boolean addNeighbor(Node n) {

		throw new UnsupportedOperationException();
	}

	// --------------------------------------------------------------------

	/**
	 * The neighborhood contains the node itself, ie it contains the loop edge.
	 */
	public Node getNeighbor(int i) {

		return Network.get(i);
	}

	// --------------------------------------------------------------------

	public int degree() {

		return Network.size();
	}

	// --------------------------------------------------------------------

	public void pack() {
	}

	// --------------------------------------------------------------------

	public void onKill() {
	}

	// --------------------------------------------------------------------

	public String toString() {

		return degree() + " [all the nodes of the overlay network]";
	}

}
