package com.nidefawl.Stats.datasource;

import java.io.File;
import java.util.HashMap;
import java.util.Set;
import com.nidefawl.Stats.StatsSettings;

public abstract class PlayerStat {

	private String name;
	public HashMap<String, Category> categories;
	private int lastBoatEnter = 0;
	private int lastMinecartEnter = 0;
	public long lastUpdate = System.currentTimeMillis();
	public boolean unload = false;
	private long lastActivity = System.currentTimeMillis();

	public PlayerStat(String name) {
		this.name = name;
		this.categories = new HashMap<String, Category>();
		int now = (int) (System.currentTimeMillis() / 1000L);
		lastBoatEnter = lastMinecartEnter = now;
	}

	public Category get(String name) {
		return categories.get(name);
	}

	public Set<String> getCats() {
		return categories.keySet();
	}

	public Category newCategory(String name) {
		Category category = new Category();
		categories.put(name, category);
		return category;
	}

	public void put(String category, String key, int val) {
		Category cat;
		if (!categories.containsKey(category))
			cat = newCategory(category);
		else
			cat = categories.get(category);
		cat.put(key, val);
	}

	protected void copy(PlayerStat from) {
		this.name = from.name;
		this.categories = new HashMap<String, Category>(from.categories);
	}

	public void convertFlatFile(String directory) {
		PlayerStat psold = new PlayerStatFile(name, directory);
		psold.load();
		copy(psold);
		String location = directory + "/" + name + ".txt";
		File fold = new File(location);
		File fnew = new File(location + ".bak");
		fold.renameTo(fnew);
	}

	public abstract void save();

	public abstract void save(boolean close);

	public abstract void load();

	public void setLastMinecartEnter(int lastMinecartEnter) {
		this.lastMinecartEnter = lastMinecartEnter;
	}

	public int getLastMinecartEnter() {
		return lastMinecartEnter;
	}

	public void setLastBoatEnter(int lastBoatEnter) {
		this.lastBoatEnter = lastBoatEnter;
	}

	public int getLastBoatEnter() {
		return lastBoatEnter;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param lastUpdate
	 *            the lastUpdate to set
	 */
	public void setLastActivity() {
		this.lastActivity = System.currentTimeMillis();
	}

	/**
	 * @return the lastUpdate
	 */
	public boolean isAfk() {
		return System.currentTimeMillis() - lastActivity > StatsSettings.afkTimer * 1000;
	}
}