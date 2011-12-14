package com.nidefawl.Stats;

import java.io.File;

import org.bukkit.ChatColor;

public class StatsSettings {

	public static String liteDb;
	public static int loginRateLimit = 3600;
	public static long delay = 30;
	public static long afkTimer = 300;
	public static String dbUrl;
	public static String dbUsername;
	public static String dbPassword;
	public static String dbTable;
	public static boolean deathNotifying;
	public static boolean debugOutput;
	public static boolean autoUpdate;
	public static boolean useMySQL;
	public static String libPath;
	public static String premessage = ChatColor.YELLOW + "[Stats]" + ChatColor.WHITE;
	public static boolean logMove;
	public static boolean logVehicle;
	public static boolean logBlockCreate;
	public static boolean logBlockDestroy;
	public static boolean logDamage;
	public static boolean logItemUse;
	public static boolean logBlockIgnite;
	public static boolean logPlayerAnimations;
	public static boolean logCraft;
	public static boolean logSmelt;

	public static void load(Stats plugin) {

		PropertiesFile properties = new PropertiesFile(new File(plugin.getDataFolder(), "stats.properties"));
		liteDb 				= "jdbc:sqlite:" + plugin.getDataFolder().getPath() + File.separator + "stats.db";
		delay 				= properties.getInt("stats-save-delay", 30, "delay between automatic saving (seconds)");
		loginRateLimit 		= properties.getInt("stats-login-delay", 3600, "limit between login-count increases");
		afkTimer 			= properties.getInt("stats-afk-delay", 300, " (seconds) If there is no player-activity in this time playedfor does not get updated. Set to 0 to disable.");

		boolean useSQL = properties.getBoolean("stats-use-sql");
		properties.remove("stats-use-sql");
		String dataSource = properties.getString("stats-datasource", useSQL ? "mysql" : "sqlite", "sqlite or mysql");
		if (dataSource.toLowerCase().equals("mysql")) {
			useMySQL = true;
		} else {
			useMySQL = false;
		}

		premessage 			= properties.getString("stats-message-prefix", "&e[Stats]&f", "");
		debugOutput 		= properties.getBoolean("stats-debug", false, "");
		deathNotifying 		= properties.getBoolean("stats-deathnotify", true, "");
		autoUpdate 			= properties.getBoolean("stats-autoUpdate", false, "");
		logMove 			= properties.getBoolean("stats-log-move", true, "disable/enable logging player move events");
		logVehicle 			= properties.getBoolean("stats-log-vehicle", true, "disable/enable logging of vehicle events");
		logBlockCreate 		= properties.getBoolean("stats-log-blockcreate", true, "disable/enable logging blockcreate");
		logBlockDestroy 	= properties.getBoolean("stats-log-blockdestroy", true, "disable/enable logging blockdestroy");
		logDamage 			= properties.getBoolean("stats-log-damage-events", true, "disable/enable logging of damage AND death events");
		logItemUse 			= properties.getBoolean("stats-log-itemuse", true, "disable/enable logging of item use events");
		logBlockIgnite 		= properties.getBoolean("stats-log-block-ignite", true, "disable/enable logging of block ignite events");
		logPlayerAnimations = properties.getBoolean("stats-log-player-animations", true, "disable/enable logging of player-animations (armswing)");
		logCraft 			= properties.getBoolean("stats-log-crafting", true, "disable/enable logging of crafting");
		//logSmelt 			= properties.getBoolean("stats-log-smelting", true, "disable/enable logging of smelting");
		
		if (premessage.length() > 0)
			if (premessage.charAt(premessage.length() - 1) != ' ')
				premessage += " ";
		premessage 			= premessage.replaceAll("(&([a-f0-9]))", "\u00A7$2");
		
		dbUrl 				= properties.getString("sql-db", "jdbc:mysql://localhost:3306/minecraft", "");
		dbUsername 			= properties.getString("sql-user", "root", "");
		dbPassword 			= properties.getString("sql-pass", "root", "");
		dbTable 			= properties.getString("sql-table-stats", "stats", "");
		properties.save();
	}

}
