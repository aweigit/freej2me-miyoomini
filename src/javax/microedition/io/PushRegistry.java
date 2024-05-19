/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.io;


import org.microemu.microedition.ImplFactory;
import org.microemu.microedition.io.PushRegistryDelegate;

import java.io.IOException;

public class PushRegistry
{

	private static PushRegistryDelegate impl;

	static {
		impl = (PushRegistryDelegate) ImplFactory.getImplementation(PushRegistry.class, PushRegistryDelegate.class);
	}

	public static void registerConnection(String connection, String midlet, String filter)
			throws ClassNotFoundException, IOException {
		impl.registerConnection(connection, midlet, filter);
	}

	public static boolean unregisterConnection(String connection) {
		return impl.unregisterConnection(connection);
	}

	public static String[] listConnections(boolean available) {
		return impl.listConnections(available);
	}

	public static String getMIDlet(String connection) {
		return impl.getMIDlet(connection);
	}

	public static String getFilter(String connection) {
		return impl.getFilter(connection);
	}

	public static long registerAlarm(String midlet, long time) throws ClassNotFoundException,
			ConnectionNotFoundException {
		return impl.registerAlarm(midlet, time);
	}

}
