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
package org.recompile.mobile;


import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.net.URL;
import java.net.URLClassLoader;

import java.lang.ClassLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.HashMap;
import java.util.ArrayList;


import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.io.*;
import javax.microedition.midlet.MIDletStateChangeException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.StandardOpenOption;
import java.nio.file.DirectoryStream;
import java.nio.file.StandardCopyOption;
import java.net.URI;

//import org.recompile.mobile.Pinyin;


public class MIDletLoader extends URLClassLoader
{
	public String name;
	public String icon;
	private String className;
	
	//private String url;

	public String suitename;

	private Class<?> mainClass;
	private MIDlet mainInst;
	private Method destroy;

	private HashMap<String, String> properties = new HashMap<String, String>(32);
	
	FileSystem zipfs;


	public MIDletLoader(URL urls[])
	{
		super(urls);

		try
		{
			System.setProperty("microedition.platform", "Nokia7650");
			System.setProperty("microedition.profiles", "MIDP-2.0");
			System.setProperty("microedition.configuration", "CLDC-1.0");
			System.setProperty("microedition.locale", "zh-CN");
			System.setProperty("microedition.encoding", "UTF-8");
			//System.setProperty("os.name", "Nokia7650");
			System.setProperty("supports.mixing", "false");
		}
		catch (Exception e)
		{
			System.out.println("Can't add CLDC System Properties");
		}

		try
		{
			loadManifest();

			properties.put("microedition.platform", "Nokia7650");
			properties.put("microedition.profiles", "MIDP-2.0");
			properties.put("microedition.configuration", "CLDC-1.0");
			properties.put("microedition.locale", "zh-CN");
			properties.put("microedition.encoding", "UTF-8");
			//properties.put("os.name", "Nokia7650");
			properties.put("supports.mixing", "false");
		}
		catch (Exception e)
		{
			System.out.println("Can't Read Manifest!");
			return;
		}

	}
	
	public MIDletLoader(URL urls[],String jarname)
	{
		super(urls);
		
		suitename=jarname;

		try
		{
			System.setProperty("microedition.platform", "Nokia7650");
			System.setProperty("microedition.profiles", "MIDP-2.0");
			System.setProperty("microedition.configuration", "CLDC-1.0");
			System.setProperty("microedition.locale", "zh-CN");
			System.setProperty("microedition.encoding", "UTF-8");
			//System.setProperty("os.name", "Nokia7650");
			System.setProperty("supports.mixing", "false");
		}
		catch (Exception e)
		{
			System.out.println("Can't add CLDC System Properties");
		}

		try
		{
			loadManifest();

			properties.put("microedition.platform", "Nokia7650");
			properties.put("microedition.profiles", "MIDP-2.0");
			properties.put("microedition.configuration", "CLDC-1.0");
			properties.put("microedition.locale", "zh-CN");
			properties.put("microedition.encoding", "UTF-8");
			//properties.put("os.name", "Nokia7650");
			properties.put("supports.mixing", "false");
		}
		catch (Exception e)
		{
			System.out.println("Can't Read Manifest!");
			return;
		}

	}
	
	public MIDletLoader(URL urls[],String u,String jarname)
	{
		super(urls);
		
		suitename=jarname;
		String url="";
		if(u.startsWith("/"))
		{
			url="jar:file:"+u;
		}
		else if(u.startsWith("file:"))
		{
			url="jar:"+u;
		}
		else
		{
			url="jar:file:/"+u;
		}
		
		try{
			HashMap<String, String> env = new HashMap<>(); 
			env.put("create", "true");
			// locate file system by using the syntax 
			// defined in java.net.JarURLConnection
			URI uri = URI.create(url);
			zipfs = FileSystems.newFileSystem(uri, env);
			
			/* Path pathInZipfile = zipfs.getPath("/");      
			
			Path targetDirectoryPath = Paths.get("./unzip/"+jarname); // 替换为具体的目标目录路径

			copyDirectory(pathInZipfile, targetDirectoryPath); */
		}
		catch(Exception e)
		{
			System.out.println("创建zip文件系统出错: "+e.getMessage());
		}
		
		

		try
		{
			System.setProperty("microedition.platform", "Nokia7650");//这个很重要
			System.setProperty("microedition.profiles", "MIDP-2.0");
			System.setProperty("microedition.configuration", "CLDC-1.0");
			System.setProperty("microedition.locale", "zh-CN");
			System.setProperty("microedition.encoding", "UTF-8");
			//System.setProperty("os.name", "Nokia7650");
			System.setProperty("supports.mixing", "false");
		}
		catch (Exception e)
		{
			System.out.println("Can't add CLDC System Properties");
		}

		try
		{
			loadManifest();

			properties.put("microedition.platform", "Nokia7650");
			properties.put("microedition.profiles", "MIDP-2.0");
			properties.put("microedition.configuration", "CLDC-1.0");
			properties.put("microedition.locale", "zh-CN");
			properties.put("microedition.encoding", "UTF-8");
			//properties.put("os.name", "Nokia7650");
			properties.put("supports.mixing", "false");
		}
		catch (Exception e)
		{
			System.out.println("Can't Read Manifest!");
			return;
		}

	}
	
	/* public static void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
		if (!Files.exists(targetDir)) {
			Files.createDirectories(targetDir);
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
			//System.out.println("jar targetDir:"+targetDir.toString());
			for (Path file : stream) {
				

				Path target = Paths.get(targetDir.toString()+"/"+file.getFileName());
				
				if (Files.isDirectory(file)) {
					copyDirectory(file, target);
				} else {
					if(!Files.exists(target)){
						Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}
	}
	
	public static String encodeHexString(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte b : data) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	} */


	public void start() throws MIDletStateChangeException
	{
		Method start;

		try
		{
			mainClass = loadClass(className);

			Constructor constructor;
			constructor = mainClass.getConstructor();
			constructor.setAccessible(true);

			MIDlet.initAppProperties(properties);
			mainInst = (MIDlet)constructor.newInstance();
		}
		catch (Exception e)
		{
			System.out.println("Problem Constructing " + name + " class: " +className);
			System.out.println("Reason: "+e.getMessage());
			e.printStackTrace();
			System.exit(0);
			return;
		}

		try
		{
			start = mainClass.getDeclaredMethod("startApp");
			start.setAccessible(true);
			
			destroy = mainClass.getDeclaredMethod("destroyApp",boolean.class);
			destroy.setAccessible(true);
			
		}
		catch (Exception e)
		{
			try
			{
				mainClass = loadClass(mainClass.getSuperclass().getName(),true);
				start = mainClass.getDeclaredMethod("startApp");
				start.setAccessible(true);
				
				destroy = mainClass.getDeclaredMethod("destroyApp",boolean.class);
				destroy.setAccessible(true);
				
			}
			catch (Exception f)
			{
				System.out.println("Can't Find startApp Method");
				f.printStackTrace();
				System.exit(0);
				return;
			}
		}

		try
		{
			System.out.println("MIDlet start");
			start.invoke(mainInst);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void destroy()
	{
		try{
			destroy.invoke(mainInst,true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void loadManifest()
	{
		
		String resource = "META-INF/MANIFEST.MF";
		Path url = findJarResource(resource);
	
		if(url==null)
		{
			resource = "meta-inf/MANIFEST.MF";
			url = findJarResource(resource);
			if(url==null)
			{
				resource = "META-INF/manifest.fm";
				url = findJarResource(resource);
				if(url==null)
				{
					resource = "meta-inf/manifest.fm";
					url = findJarResource(resource);
					if(url==null)
					{
						return;
					}	
				}	
			}
		}
		

		String line;
		String[] parts;
		int split;
		String key;
		String value;
		try
		{
			InputStream is = Files.newInputStream(url,StandardOpenOption.READ);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			
			ArrayList<String> lines = new ArrayList<String>();
			while ((line = br.readLine()) != null) 
			{
				if(line.startsWith(" "))
				{
					line = lines.get(lines.size()-1) + line.trim();
					lines.remove(lines.size()-1);
				}
				lines.add(line);
			}

			for (int i=0; i<lines.size(); i++)
			{
				line = lines.get(i);
				if(line.startsWith("MIDlet-1:"))
				{
					System.out.println(line);
					line = line.substring(9);
					parts = line.split(",");
					if(parts.length == 3)
					{
						name = parts[0].trim();
					
						/* if(!savepath.equals(""))
						{
							name=savepath;
							name=Pinyin.ch2pinyin(savepath);
						}
						else
						{
							name=encodeHexString(name.getBytes());
						} */
						
						icon = parts[1].trim();
						className = parts[2].trim();
						//suitename = name;
					}
					//System.out.println("Loading " + name);
				}

				split = line.indexOf(":");
				if(split>0)
				{
					key = line.substring(0, split).trim();
					value = line.substring(split+1).trim();
					properties.put(key, value);
				}
			}
			// for RecordStore, remove illegal chars from name
			suitename = suitename.replace(":","");
		}
		catch (Exception e)
		{
			System.out.println("Can't Read Jar Manifest!");
			e.printStackTrace();
		}
	}

	public Path findJarResource(String resource)
	{
		String ju="";
		if(!resource.startsWith("/"))
		{
			resource="/"+resource;
		}
		
		Path pathInZipfile = zipfs.getPath(resource);
		//Path pathInZipfile=Paths.get("./unzip/"+suitename+resource);
		
		if(!Files.exists(pathInZipfile))
		{
			return null;
		}
		
		return pathInZipfile;
	}

	public InputStream getResourceAsStream(String resource)
	{
		//System.out.println("resource:"+resource);
		Path url;
		//System.out.println("Loading Resource: " + resource);

		/* if(resource.startsWith("/"))
		{
			resource = resource.substring(1);
		} */

		try
		{
			url = findJarResource(resource);
			if(url==null)
			{
				return null;
			}
			// Read all bytes, return ByteArrayInputStream //
			InputStream stream = Files.newInputStream(url,StandardOpenOption.READ);

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			return new ByteArrayInputStream(buffer.toByteArray());
		}
		catch (Exception e)
		{
			System.out.println(resource + " Not Found");
			return super.getResourceAsStream(resource);
		}
	}

	//(java17可用)
	/* public URL getResource(String resource)
	{
		//System.out.println("Loading getResource:" + resource);
		if(resource.startsWith("/"))
		{
			resource = resource.substring(1);
		}
		try
		{
			URL url = findResource(resource);
			//System.out.println("Loading getResource URL: " + url);
			return url;
		}
		catch (Exception e)
		{
			System.out.println(resource + " Not Found");
			return super.getResource(resource);
		}
	} */

	/*
		********  loadClass Modifies Methods with ObjectWeb ASM  ********
		Replaces java.lang.Class.getResourceAsStream calls with calls
		to Mobile.getResourceAsStream which calls
		MIDletLoader.getResourceAsStream(class, string)
	*/

	public InputStream getMIDletResourceAsStream(String resource)
	{
		//System.out.println("Get Resource: "+resource);

		Path url = findJarResource(resource);
		if(url==null)
		{
			return null;
		}
		// Read all bytes, return ByteArrayInputStream //
		try
		{
			InputStream stream = Files.newInputStream(url,StandardOpenOption.READ);

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			return new ByteArrayInputStream(buffer.toByteArray());
		}
		catch (Exception e)
		{
			//System.out.println("Get getMIDletResourceAsStream exp: "+e.getMessage());
			//System.out.println(e.toString());
			e.printStackTrace();
			return super.getResourceAsStream(resource);
		}
	}

	public byte[] getMIDletResourceAsByteArray(String resource)
	{
		Path url = findJarResource(resource);

		try
		{
			InputStream stream = Files.newInputStream(url,StandardOpenOption.READ);

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			return buffer.toByteArray();
		}
		catch (Exception e)
		{
			System.out.println(resource + " Not Found");
			return new byte[0];
		}
	}


	public Class loadClass(String name) throws ClassNotFoundException
	{
		
		byte[] stream;
		String resource;
		byte[] code;
		
		if(name.startsWith("java.applet.Applet"))
		{
			return null;
		}

		//System.out.println("Load Class "+name);

		if(
			name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("com.nokia.mid.m3d") || 
			name.startsWith("com.nokia.mid.sound") || name.startsWith("com.nokia.mid.ui") || 
			name.startsWith("com.mascotcapsule") || name.startsWith("com.samsung") || name.startsWith("sun.") ||
			name.startsWith("com.siemens") || name.startsWith("org.recompile")
			)
		{
			return loadClass(name, false);
		}

		try
		{
			//System.out.println("Instrumenting Class "+name);
			resource = name.replace(".", "/") + ".class";
			//System.out.println("Instrumenting Class: "+resource);
			stream = getMIDletResourceAsByteArray(resource);
			code = MyProducer.instrument(stream);
			return defineClass(name, code, 0, code.length);
		}
		catch (Exception e)
		{
			System.out.println("Error Adapting Class "+name);
			return null;
		}

	}
	
	/* public Class findClass(String name) throws ClassNotFoundException {
		
		if(
			name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("com.nokia") ||
			name.startsWith("com.mascotcapsule") || name.startsWith("com.samsung") || name.startsWith("sun.") ||
			name.startsWith("com.siemens") || name.startsWith("org.recompile")
			)
		{
			return super.findClass(name);
		}
		
		InputStream stream;
		String resource;
		byte[] code;
		try{
			resource = name.replace(".", "/") + ".class";
			//System.out.println("Load  resource find class："+resource);
			stream = getResourceAsStream(resource);
			code = instrument(stream);
			return super.defineClass(name, code, 0, code.length);
		}
		catch (Exception e)
		{
			System.out.println("自定义加载类出错 "+name);
			return null;
		}
		
	} */



/* **************************************************************
 * Special Siemens Stuff
 * ************************************************************** */

	public InputStream getMIDletResourceAsSiemensStream(String resource)
	{
		Path url = findJarResource(resource);
		if(url==null)
		{
			return null;
		}

		try
		{
			InputStream stream = Files.newInputStream(url,StandardOpenOption.READ);

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int count=0;
			byte[] data = new byte[4096];
			while (count!=-1)
			{
				count = stream.read(data);
				if(count!=-1) { buffer.write(data, 0, count); }
			}
			return new SiemensInputStream(buffer.toByteArray());
		}
		catch (Exception e)
		{
			return super.getResourceAsStream(resource);
		}
	}

	private class SiemensInputStream extends InputStream
	{
		private ByteArrayInputStream iostream;

		public SiemensInputStream(byte[] data)
		{
			iostream = new ByteArrayInputStream(data);
		}

		public int read()
		{
			int t = iostream.read();
			if (t == -1) { return 0; }
			return t;
		}
		public int read(byte[] b, int off, int len)
		{
			int t = iostream.read(b, off, len);
			if (t == -1) { return 0; }
			return t;
		}
	}

}
