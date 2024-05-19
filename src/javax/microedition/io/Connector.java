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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.recompile.mobile.Mobile;

import org.microemu.microedition.ImplFactory;
import java.io.IOException;

public class Connector
{

	public static final int READ = 1;
	public static final int READ_WRITE = 3;
	public static final int WRITE = 2;

	
	public static InputStream openInputStream(String name) throws IOException
	{
		System.out.println("Connector: " + name);
		if(name.startsWith("resource:")) // older Siemens phones?
		{
			return Mobile.getPlatform().loader.getMIDletResourceAsSiemensStream(name.substring(9));
		}
		/*else
		{
			//return Mobile.getPlatform().loader.getMIDletResourceAsStream(name); // possible
			System.out.println("Faked InputStream for "+name); // just in case //
			return new fakeIS();
		} */
		return ImplFactory.getCGFImplementation(name).openInputStream(name);
		
	}


	public static DataInputStream openDataInputStream(String name) throws IOException
	{
		/* System.out.println("Faked DataInputStream: "+name);
		return new DataInputStream(new fakeIS()); */
		return ImplFactory.getCGFImplementation(name).openDataInputStream(name);
	}



	public static Connection open(String name) throws IOException { 
		//System.out.println("Connector: " + name);
		
		
		/* Throwable ex = new Throwable();

		StackTraceElement[] stackElements = ex.getStackTrace();

		if (stackElements != null) {

			for (int i = 0; i < stackElements.length; i++) {

			System.out.print(stackElements[i].getClassName()+":");

			System.out.print(stackElements[i].getFileName()+":");

			System.out.print(stackElements[i].getLineNumber()+":");

			System.out.println(stackElements[i].getMethodName());

			System.out.println("-----------------------------------");

			}

		}
		
		Exception e = new Exception("this is a log");

		e.printStackTrace(); */
		
		return ImplFactory.getCGFImplementation(name).open(name);
		
	}

	public static Connection open(String name, int mode) throws IOException {
		//System.out.println("Connector: " + name);
		return ImplFactory.getCGFImplementation(name).open(name, mode);
	}

	public static Connection open(String name, int mode, boolean timeouts) throws IOException {
		//System.out.println("Connector: " + name);
		return ImplFactory.getCGFImplementation(name).open(name, mode, timeouts);
	}

	public static DataOutputStream openDataOutputStream(String name) throws IOException{ 
		/* return new DataOutputStream(new fakeOS());  */
		//System.out.println("Connector: " + name);
		return ImplFactory.getCGFImplementation(name).openDataOutputStream(name);
	}

	public static OutputStream openOutputStream(String name) throws IOException {
		//System.out.println("Connector: " + name);
		/* return new fakeOS();  */
		return ImplFactory.getCGFImplementation(name).openOutputStream(name);
	}



	/* // fake inputstream 
	private static class fakeIS extends InputStream
	{
		public int avaliable() { return 0; }

		public void close() { }

		public void mark() { }

		public boolean markSupported() { return false; }

		public int read() { return 0; }

		public int read(byte[] b) { return 0; }
		
		public int read(byte[] b, int off, int len) { return 0; }

		public void reset() { }

		public long skip(long n) { return (long)0; }
	}
	
	// fake outputstream 
	private static class fakeOS extends OutputStream
	{
		public int avaliable() { return 0; }

		public void close() { }

		public void mark() { }

		public boolean markSupported() { return false; }

		public void write(int b) { }

		public void write(byte[] b) {  }
		
		public void  write(byte[] b, int off, int len) {  }

		public void reset() { }

		public long skip(long n) { return (long)0; }
	} */

}
