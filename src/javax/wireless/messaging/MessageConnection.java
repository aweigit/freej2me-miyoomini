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
package javax.wireless.messaging;
import java.io.IOException;
import java.io.InterruptedIOException;
public interface MessageConnection extends javax.microedition.io.Connection
{
	public static final String BINARY_MESSAGE = "binary";
	public static final String MULTIPART_MESSAGE = "multipart";
	public static final String TEXT_MESSAGE = "text";

	public Message newMessage(String type);

	public Message newMessage(String type, String address);

	public int numberOfSegments(Message msg);

	public Message receive() throws IOException, InterruptedIOException;

	public void send(Message msg) throws IOException, InterruptedIOException;

	public void setMessageListener(MessageListener l) throws IOException;
}
