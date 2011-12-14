package com.nidefawl.Stats.ItemResolver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import com.nidefawl.Stats.Stats;

public class hModItemResolver implements itemResolver {
	static final Logger log = Logger.getLogger("Minecraft");
	protected Map<String, Integer> items;
	String location = null;
	

	public hModItemResolver(File itemsFile) {
		location = itemsFile.getPath();
		loadItems(itemsFile);
	}

	public void loadItems(File itemsFile) {
		if (!itemsFile.exists()) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(location);
				writer.write("#This file is part of the modified Stats-Version used by Webstatistics for Minecraft\r\n");
				writer.write("#You don't need to edit this file!\r\n");
			} catch (Exception e) {
				Stats.LogError("Exception while creating " + location + " " + e);
				e.printStackTrace();
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						Stats.LogError("Exception while closing writer for " + location + " " + e);
						e.printStackTrace();
					}
				}
			}
		}
		items = new HashMap<String, Integer>();
		try {
			Scanner scanner = new Scanner(itemsFile);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith("#")) {
					continue;
				}
				if (line.equals("")) {
					continue;
				}
				String[] split = line.split(":");
				String name = split[0];

				this.items.put(name, Integer.parseInt(split[1]));
			}
			scanner.close();
		} catch (Exception e) {
			Stats.LogError("Exception while reading " + location + " (Are you sure you formatted it correctly?)"+ e);
			e.printStackTrace();
		}
	}


	@Override
	public int getItem(String name) {
		if (items.containsKey(name)) {
			return items.get(name);
		}
		try {
			int i = Integer.valueOf(name);
			if(i>0 && i < 3000)  {
				if(!getItem(i).equals(name)) {
					return i;
				}
			}
		} catch (Exception e) {
		}
		return 0;
	}


	@Override
	public String getItem(int id) {
		for (String name : items.keySet()) {
			if (items.get(name) == id) {
				return name;
			}
		}
		return String.valueOf(id);
	}

}