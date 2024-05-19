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
package org.recompile.freej2me;

import java.util.HashMap;


import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.recompile.mobile.Mobile;

public class SDLConfig
{

	private int width;
	private int height;


	private File file;
	private String configPath = "";
	private String configFile = "";



	HashMap<String, String> settings = new HashMap<String, String>(6);

	public SDLConfig()
	{
		width = Mobile.getPlatform().lcdWidth;
		height = Mobile.getPlatform().lcdHeight;
	}

	public void init(String appname)
	{
		//String appname = Mobile.getPlatform().loader.suitename;
		configPath = Mobile.getPlatform().dataPath + "./config/"+appname;
		configFile = configPath + "/game.conf";
		// Load Config //
		try
		{
			Files.createDirectories(Paths.get(configPath));
		}
		catch (Exception e)
		{
			System.out.println("Problem Creating Config Path "+configPath);
			System.out.println(e.getMessage());
		}

		try // Check Config File
		{
			file = new File(configFile);
			if(!file.exists())
			{
				file.createNewFile();
				settings.put("width", ""+width);
				settings.put("height", ""+height);
				settings.put("sound", "100");
				settings.put("phone", "p");
				settings.put("fps", "60");
				saveConfig();
			}
		}
		catch (Exception e)
		{
			System.out.println("Problem Opening Config "+configFile);
			System.out.println(e.getMessage());
		}

		try // Read Records
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			String[] parts;
			while((line = reader.readLine())!=null)
			{
				parts = line.split(":");
				if(parts.length==2)
				{
					parts[0] = parts[0].trim();
					parts[1] = parts[1].trim();
					if(parts[0]!="" && parts[1]!="")
					{
						settings.put(parts[0], parts[1]);
					}
				}
			}
			if(!settings.containsKey("width")) { settings.put("width", ""+width); }
			if(!settings.containsKey("height")) { settings.put("height", ""+height); }
			if(!settings.containsKey("sound")) { settings.put("sound", "100"); }
			if(!settings.containsKey("phone")) { settings.put("phone", "p"); }
			if(!settings.containsKey("fps")) { settings.put("fps", "60"); }

			int w = Integer.parseInt(settings.get("width"));
			int h = Integer.parseInt(settings.get("height"));
			if(width!=w || height!=h)
			{
				width = w;
				height = h;
			}
		}
		catch (Exception e)
		{
			System.out.println("Problem Reading Config: "+configFile);
			System.out.println(e.getMessage());
		}

	}

	public void saveConfig()
	{
		try
		{
			FileOutputStream fout = new FileOutputStream(file);

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fout));

			for (String key : settings.keySet())
			{
				writer.write(key+":"+settings.get(key)+"\n");
			}
			writer.close();
		}
		catch (Exception e)
		{
			System.out.println("Problem Opening Config "+configFile);
			System.out.println(e.getMessage());
		}
	}
}
