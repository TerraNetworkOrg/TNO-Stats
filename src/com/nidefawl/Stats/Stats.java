package com.nidefawl.Stats;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.nidefawl.Achievements.Achievements;
import com.nidefawl.Stats.ItemResolver.hModItemResolver;
import com.nidefawl.Stats.ItemResolver.itemResolver;
import com.nidefawl.Stats.datasource.Category;
import com.nidefawl.Stats.datasource.PlayerStat;
import com.nidefawl.Stats.datasource.PlayerStatSQL;
import com.nidefawl.Stats.datasource.StatsSQLConnectionManager;
import com.nidefawl.Stats.udpates.Update1;
import com.nidefawl.Stats.udpates.Update2;
import com.nidefawl.Stats.util.Updater;

public class Stats extends JavaPlugin {

	public final static Logger log = Logger.getLogger("Minecraft");
	public static final String version = "1.1.1";
	public static final String logprefix = "[Stats 1.1.1]";
	public final static String defaultCategory = "stats";
	public boolean enabled = false;
	public boolean updated = false;
	private HashMap<String, PlayerStat> stats = new HashMap<String, PlayerStat>();
	private itemResolver items = null;
	private StatsPlayerListener playerListener = null;
	private StatsVehicleListener vehicleListener = null;
	private StatsBlockListener blockListener = null;
	private StatsEntityListener entityListener = null;
	private StatsServerListener serverListener = null;
	private StatsCraftListener craftListener = null;
	//private StatsSmeltListener smeltListener = null;
	private Updater updater = null;
	public Permission permission = null;

	/**
	 * @return the Updater instance
	 */
	/*public Updater getUpdater() {
		return updater;
	}*/
	public PlayerStat getPlayerStat(String name) {
		return stats.get(name);
	}

	public static void LogError(String Message) {
		log.log(Level.SEVERE, logprefix + " " + Message);
	}

	public static void LogInfo(String Message) {
		log.info(logprefix + " " + Message);
	}

	private boolean checkSchema() {
		Connection conn = null;
		DatabaseMetaData dbm = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean result = false;
		try {
			conn = StatsSQLConnectionManager.getConnection(StatsSettings.useMySQL);
			dbm = conn.getMetaData();
			rs = dbm.getTables(null, null, StatsSettings.dbTable, null);
			if (!rs.next()) {
				ps = conn.prepareStatement("CREATE TABLE `" + StatsSettings.dbTable + "` (" + "`player` varchar(32) NOT NULL DEFAULT '-'," + "`category` varchar(32) NOT NULL DEFAULT 'stats'," + "`stat` varchar(32) NOT NULL DEFAULT '-'," + "`value` int(11) NOT NULL DEFAULT '0',"
						+ "PRIMARY KEY (`player`,`category`,`stat`));");
				ps.executeUpdate();
				LogInfo("created table '" + StatsSettings.dbTable + "'");
			}
			result = true;
		} catch (SQLException ex) {
			LogError("SQL exception" + ex);
			ex.printStackTrace();
			result = false;
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				LogError("SQL exception (on close)" + ex);
				ex.printStackTrace();
				result = false;
			}
		}
		return result;
	}
	private void updateTimedStats(PlayerStat stat) {
		if (playerListener.distWalked.containsKey(stat.getName())) {
			int distance = (int) Math.floor(playerListener.distWalked.get(stat.getName()));
			if (distance >= 1) {
				Category cat = stat.categories.get("stats");
				if (cat == null)
					cat = stat.newCategory("stats");
				cat.add("move", distance);
				playerListener.distWalked.put(stat.getName(), 0.0f);
				stat.setLastActivity();
			}
		}
		if (vehicleListener.distBoat.containsKey(stat.getName())) {
			int distance = (int) Math.floor(vehicleListener.distBoat.get(stat.getName()));
			if (distance >= 1) {
				Category cat = stat.categories.get("boat");
				if (cat == null)
					cat = stat.newCategory("boat");
				cat.add("move", distance);
				vehicleListener.distBoat.put(stat.getName(), 0.0f);
				stat.setLastActivity();
			}
		}
		if (vehicleListener.distCart.containsKey(stat.getName())) {
			int distance = (int) Math.floor(vehicleListener.distCart.get(stat.getName()));
			if (distance >= 1) {
				Category cat = stat.categories.get("minecart");
				if (cat == null)
					cat = stat.newCategory("minecart");
				cat.add("move", distance);
				vehicleListener.distCart.put(stat.getName(), 0.0f);
				stat.setLastActivity();
			}
		}
		if (StatsSettings.afkTimer > 0 && !stat.isAfk()) {
			updateStat(stat.getName(), defaultCategory, "playedfor", (int) (System.currentTimeMillis() - stat.lastUpdate) / 1000, false);
		}
		stat.lastUpdate = System.currentTimeMillis();
	}
	public void setSavedStats(CommandSender sender, String player, String category, String key, String value) {
		ArrayList<String> tounload = new ArrayList<String>();
		tounload.addAll(stats.keySet());
		for (String name : tounload) {
			unload(name);
		}

		stats.clear();
		int result = 0;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = StatsSQLConnectionManager.getConnection(StatsSettings.useMySQL);
			StringBuilder statement = new StringBuilder();
			int conditions = 0;
			statement.append("UPDATE " + StatsSettings.dbTable + " set value = ?");
			if (!player.equals("*"))
				statement.append((conditions++ == 0 ? " where" : " and") + " player = ?");
			if (!category.equals("*"))
				statement.append((conditions++ == 0 ? " where" : " and") + " category = ?");
			if (!key.equals("*"))
				statement.append((conditions++ == 0 ? " where" : " and") + " stat = ?");

			ps = conn.prepareStatement(statement.toString());
			ps.setString(1, value);
			conditions++;
			if (!key.equals("*"))
				ps.setString(conditions--, key);
			if (!category.equals("*"))
				ps.setString(conditions--, category);
			if (!player.equals("*"))
				ps.setString(conditions--, player);
			result = ps.executeUpdate();
		} catch (SQLException ex) {
			LogError("SQL exception" + ex);
			ex.printStackTrace();
			sender.sendMessage(StatsSettings.premessage + ex.getMessage());
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				LogError("SQL exception (on close)" + ex);
				ex.printStackTrace();
				sender.sendMessage(StatsSettings.premessage + ex.getMessage());
			}
		}
		sender.sendMessage(StatsSettings.premessage + "Updated " + result + " stats.");
		for (Player p : getServer().getOnlinePlayers()) {
			load(p);
		}
	}

	public int editPlayerStat(PlayerStat ps, String category, String key, String value) {

		int statsEdited = 0;
		if (category.equals("*")) {
			for (String catName : ps.categories.keySet()) {
				if (key.equals("*")) {
					for (String keyName : ps.categories.get(catName).getEntries()) {
						ps.categories.get(catName).set(keyName, Integer.valueOf(value));
						statsEdited++;
					}
				} else {
					if (!ps.categories.get(catName).getEntries().contains(key))
						continue;
					ps.categories.get(catName).set(key, Integer.valueOf(value));
					statsEdited++;
				}

			}
		} else {
			if (ps.categories.containsKey(category)) {
				if (key.equals("*")) {
					for (String keyName : ps.categories.get(category).getEntries()) {
						ps.categories.get(category).set(keyName, Integer.valueOf(value));
						statsEdited++;
					}
				} else {
					if (!ps.categories.get(category).getEntries().contains(key))
						return statsEdited;
					ps.categories.get(category).set(key, Integer.valueOf(value));
					statsEdited++;
				}
			}
		}
		return statsEdited;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		Player player = (Player) sender;
		if (sender instanceof Player) {
			if (commandLabel.equals("played") && permission.has(player, "stats.view.playtime")) {
				int playedFor = get(player.getName(), "stats", "playedfor");
				if (playedFor == 0) {
					Messaging.send(player, StatsSettings.premessage + "No Playedtime yet!");
					return true;
				}
				Messaging.send(player, StatsSettings.premessage + "You played for "+ChatColor.WHITE + StatsView.GetTimeString(playedFor));
				return true;
			} 
		}        
		if (commandLabel.equals("stats")) {
			if (args.length == 1 && args[0].equalsIgnoreCase("help") || ((sender instanceof ConsoleCommandSender) && args.length == 0)) {
				if ((sender instanceof Player) && permission.has(player, "stats.view.playtime")) {
					sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + "/played - Shows your play-time");
				}
				if ((sender instanceof Player) && permission.has(player, "stats.view.own")) {
					sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + "/stats - Shows your stats summary");
				}
				if(permission.has(player, "stats.view.others"))  {
					sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + "/stats <player> - Shows players stats summary");
				}
				if (permission.has(player, "stats.admin")) {
					sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + "/stats list - Shows loaded players");
					sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + "/stats set <player> <cat> <stat> <val> - Set stats manually");
					sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + "/stats debug - Prints stat-update messages to console.");
					sender.sendMessage(StatsSettings.premessage + "Usage: " + ChatColor.WHITE + "/stats [category|debug|statname|list|helpset]");
					sender.sendMessage(StatsSettings.premessage + "or /stats [player] [category|statname]");
				} else {
					sender.sendMessage(StatsSettings.premessage + "Usage: " + ChatColor.WHITE + "/stats [category|statname|help] or /stats [player] [category|statname]");
				}
				return true;
			}
			else if (args.length == 1 && args[0].equalsIgnoreCase("list") && permission.has(player, "stats.admin")) {
				sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + "Loaded playerstats (" + stats.size() + "): " + StatsPlayerList());
				return true;
			}
			else if (args.length > 0 && args[0].equalsIgnoreCase("set") && permission.has(player, "stats.admin")) {
				if (args.length < 5) {
					sender.sendMessage(StatsSettings.premessage + ChatColor.RED + "Need more arguments (use * to select all)");
					sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + "/stats set [player] [category] [key] [value]- Set stats manually");
					return true;
				}
				try {
					Integer.valueOf(args[4]);
				} catch (Exception e) {
					sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + "[value] should be a number (" + args[4] + " is not)!");
					return true;
				}
				setSavedStats(sender, args[1], args[2], args[3], args[4]);
				return true;
			}
			else if (args.length == 1 && args[0].equalsIgnoreCase("debug") && permission.has(player, "stats.admin")) {
				StatsSettings.debugOutput = !StatsSettings.debugOutput;
				sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + "Debugging " + (StatsSettings.debugOutput ? "enabled. Check server log." : "disabled."));
				return true;
			}
			return StatsView.onCommand(this, sender, args);
		}

		return false;
	}

	private Boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }

	public String StatsPlayerList() {
		if (stats.size() == 0)
			return "No players loaded";
		int length = (stats.size() - 1);

		int on = 0;
		String list = "";
		for (String currentName : stats.keySet()) {
			if (currentName == null) {
				++on;
				continue;
			}

			list += (on >= length) ? currentName : currentName + ", ";
			++on;
		}
		list += " ";
		return list;
	}
	protected final FilenameFilter filter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			if(name.equals("items.txt")) return false;
			return name.endsWith(".txt");
		}
	};

	protected final FilenameFilter filterOld = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.endsWith(".txt.old");
		}
	};
	public void convertFlatFiles() {
		String[] files = getDataFolder().list(filterOld);
		if (files != null && files.length > 0) {
			for (int i = 0; i < files.length; i++) {
				String basename = files[i].substring(0, files[i].lastIndexOf("."));
				File fnew = new File(getDataFolder(), files[i]);
				File fold = new File(getDataFolder(), basename);
				fnew.renameTo(fold);
			}
		}
		files = getDataFolder().list(filter);
		if (files == null || files.length == 0) {
		}

		int count = 0;
		PlayerStatSQL ps;
		for (int i = 0; i < files.length; i++) {
			File fold = new File(getDataFolder(), files[i]);
			if (!fold.exists())
				continue;

			String basename = files[i].substring(0, files[i].lastIndexOf("."));
			ps = new PlayerStatSQL(basename, this);
			ps.convertFlatFile(getDataFolder().getPath());
			ps.save();
			count++;
		}
		if(count > 0) {
			Stats.LogInfo("Converted " + count + " stat files to " + (StatsSettings.useMySQL ? "MySQL" : "SQLite"));
		}
	}

	public Stats() {

	}

	public void onEnable() {
		
		if (!setupPermissions()) {
            System.out.println("Null perm");
           //use these if you require econ
          //getServer().getPluginManager().disablePlugin(this);
          //return;
        }
		getDataFolder().mkdirs();
		File statsDirectory = new File("stats");
		if (statsDirectory.exists() && statsDirectory.isDirectory()) {
			File intSettings = new File("stats", "internal.ini");
			if (intSettings.exists()) {
				intSettings.delete();
			}
			LogInfo("Moving ./stats/ directory to " + getDataFolder().getPath());
			if (!statsDirectory.renameTo(new File(getDataFolder().getPath()))) {
				LogError("Moving ./stats/ directory to " + getDataFolder().getPath() + " failed");
				LogError("Please move your files manually and delete the old 'stats' directory. Thanks");
				LogError("Disabling Stats");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
		}
		StatsSettings.load(this);
		updater = new Updater(this);
		try {
			updated = updater.updateDist(StatsSettings.autoUpdate);
			if (updated) {
				LogInfo("UPDATE INSTALLED. PLEASE RESTART....");
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Connection conn = StatsSQLConnectionManager.getConnection(StatsSettings.useMySQL);
		try {
			if (conn == null || conn.isClosed()) {
				LogError("Could not establish SQL connection. Disabling Stats");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
		} catch (SQLException e) {
			LogError("Could not establish SQL connection. Disabling Stats");
			e.printStackTrace();
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (!checkSchema()) {
			LogError("Could not create table. Disabling Stats");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		convertFlatFiles();
		if (updated)
			return;
		Update1.execute(this);
		Update2.execute(this);
		items = new hModItemResolver(new File(getDataFolder(),"items.txt"));
		stats = new HashMap<String, PlayerStat>();
		enabled = true;
		playerListener = new StatsPlayerListener(this);
		blockListener = new StatsBlockListener(this);
		entityListener = new StatsEntityListener(this);
		vehicleListener = new StatsVehicleListener(this);
		serverListener = new StatsServerListener(this);
		//smeltListener = new StatsSmeltListener(this);
		initialize();
		LogInfo("Plugin Enabled");
		for (Player p : getServer().getOnlinePlayers()) {
			load(p);
		}
		getServer().getScheduler().scheduleAsyncRepeatingTask(this, new SaveTask(this), StatsSettings.delay * 20, StatsSettings.delay * 20);
		
	}



	public static class SaveTask implements Runnable {
		private Stats statsInstance;

		public SaveTask(Stats plugin) {
			statsInstance = plugin;
		}

		public void run() {
			
			if (!statsInstance.enabled)
				return;
			statsInstance.saveAll();
		}
		
		public void saveAll() {
			if (StatsSettings.debugOutput)
				log.info("Stats debug: saving " + this.statsInstance.stats.size() + " players stats");
			try {
				Connection conn = StatsSQLConnectionManager.getConnection(StatsSettings.useMySQL);
				if (conn == null)
					return;
				conn.setAutoCommit(false);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			for (PlayerStat stat : this.statsInstance.stats.values()) {
				if (stat == null || this.statsInstance.getServer().getPlayer(stat.getName()) == null) {
					stat.unload = true;
					continue;
				}
				this.statsInstance.updateTimedStats(stat);
				stat.save(false);
			}
			StatsSQLConnectionManager.closeConnection(StatsSettings.useMySQL);
			for (PlayerStat stat : this.statsInstance.stats.values()) {
				if (!stat.unload)
					continue;
				LogError("onPlayerQuit did not happen, unloading " + stat.getName() + " now");
				this.statsInstance.logout(stat.getName());
				this.statsInstance.unload(stat.getName());
			}
		}
	}

	public void onDisable() {
		if (enabled) {
			saveAll();
			Plugin achPlugin = getServer().getPluginManager().getPlugin("Achievements");
			if (achPlugin != null && achPlugin.isEnabled()) {
				if (((Achievements) achPlugin).enabled) {
					((Achievements) achPlugin).checkAchievements();
					((Achievements) achPlugin).Disable();
				}
			}
			enabled = false;
			getServer().getScheduler().cancelTasks(this);
			stats = null;
			updater.saveInternal();
			StatsSQLConnectionManager.closeConnection(StatsSettings.useMySQL);
		}
		LogInfo("Plugin Disabled");
	}

	public void initialize() {
		getServer().getPluginManager().registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Highest, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Monitor, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.Monitor, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_DROP_ITEM, playerListener, Priority.Monitor, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Monitor, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Monitor, this);
		if(StatsSettings.logPlayerAnimations) {
			getServer().getPluginManager().registerEvent(Event.Type.PLAYER_ANIMATION, playerListener, Priority.Monitor, this);
		}
		if(StatsSettings.logMove) {
			getServer().getPluginManager().registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Monitor, this);
		}
		if(StatsSettings.logBlockCreate) {
			getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACE, blockListener, Priority.Monitor, this);
		}
		if(StatsSettings.logBlockDestroy) {
			getServer().getPluginManager().registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Monitor, this);
		}
		if(StatsSettings.logItemUse) {
			getServer().getPluginManager().registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Monitor, this);
		}
		if(StatsSettings.logBlockIgnite){ 
			getServer().getPluginManager().registerEvent(Event.Type.BLOCK_IGNITE, blockListener, Priority.Monitor, this);
		}
		if(StatsSettings.logDamage) {
			getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Highest, this);
			getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Priority.Highest, this);
		}
		if(StatsSettings.logVehicle) {
			getServer().getPluginManager().registerEvent(Event.Type.VEHICLE_ENTER, vehicleListener, Priority.Monitor, this);
			getServer().getPluginManager().registerEvent(Event.Type.VEHICLE_MOVE, vehicleListener, Priority.Monitor, this);
		}
		//if(StatsSettings.logSmelt) {
		//	getServer().getPluginManager().registerEvent(Event.Type.FURNACE_SMELT, smeltListener, Priority.Monitor, this);
		//}
		if(craftListener == null){
		    if(getServer().getPluginManager().getPlugin("Spout") != null){
				craftListener = new StatsCraftListener(this);
		    	if(StatsSettings.logCraft) {
		    		getServer().getPluginManager().registerEvent(Event.Type.CUSTOM_EVENT, craftListener, Priority.Monitor, this);
                    System.out.println("[Stats] Successfully linked with Spout.");
			    }
			}
		}
	}

	public void updateStat(Player player, String statType, boolean resetAfkTimer) {
		updateStat(player, statType, 1, resetAfkTimer);
	}

	public void updateStat(Player player, String statType, int num, boolean resetAfkTimer) {
		updateStat(player.getName(), defaultCategory, statType, num, resetAfkTimer);
	}

	public void updateStat(Player player, String statType, Block block, boolean resetAfkTimer) {
		updateStat(player, statType, block, 1, resetAfkTimer);
	}

	public void updateStat(Player player, String statType, Block block, int num, boolean resetAfkTimer) {
		if (block.getTypeId() <= 0)
			return;
		String blockName = getItems().getItem(block.getTypeId());
		updateStat(player.getName(), statType, blockName, num, resetAfkTimer);
	}

	public void updateStat(Player player, String category, String key, int val, boolean resetAfkTimer) {
		updateStat(player.getName(), category, key, val, resetAfkTimer);
	}

	public void updateStat(String player, String category, String key, int val, boolean resetAfkTimer) {
		if (!enabled)
			return;
		if (player == null || player.length() < 1) {
			LogError("updateStat got empty player for [" + category + "] [" + key + "] [" + val + "]");
			return;
		}

		PlayerStat ps = stats.get(player);
		if (ps == null)
			return;
		Category cat = ps.get(category);
		if (cat == null)
			cat = ps.newCategory(category);
		cat.add(key, val);
		if (resetAfkTimer)
			ps.setLastActivity();
		if (StatsSettings.debugOutput)
			log.info(logprefix + " [DEBUG]: adding " + val + " to " + category + "/" + key + " of " + player);
	}

	public void setStat(String player, String category, String key, int val) {
		PlayerStat ps = stats.get(player);
		if (ps == null)
			return;
		ps.put(category, key, val);
	}

	public int get(String player, String category, String key) {
		PlayerStat ps = stats.get(player);
		if (ps == null)
			return 0;
		Category cat = ps.get(category.toLowerCase());
		if (cat == null && ps.get(category) != null)
			cat = ps.get(category);
		if (cat == null)
			return 0;
		if (cat.get(key) == 0 && cat.get(key.toLowerCase()) != 0)
			return cat.get(key.toLowerCase());
		return cat.get(key);
	}

	public void load(Player player) {
		if (!permission.has(player, "stats.log")) {
			if (StatsSettings.debugOutput)
				LogInfo("player " + player.getName() + " has no stats.log permission. Not loading/logging actions");
			return;
		}
		if (stats.containsKey(player.getName())) {
			LogError("attempting to load already loaded player: " + player.getName());
			return;
		}
		PlayerStat ps = new PlayerStatSQL(player.getName(), this);
		ps.load();
		stats.put(player.getName(), ps);
		if (StatsSettings.debugOutput)
			LogInfo("player " + player.getName() + " has been loaded.");
	}

	public void unload(String player) {
		entityListener.UnloadPlayer(player);
		if (stats.containsKey(player)) {
			PlayerStat ps = stats.get(player);
			updateTimedStats(ps);
			ps.save();
			stats.remove(player);
			return;
		}
	}

	public boolean isAfk(Player p) {
		if (!stats.containsKey(p.getName()))
			return false;
		return stats.get(p.getName()).isAfk();
	}

	public void saveAll() {
		if (StatsSettings.debugOutput)
			log.info("Stats debug: saving " + stats.size() + " players stats");
		try {
			Connection conn = StatsSQLConnectionManager.getConnection(StatsSettings.useMySQL);
			if (conn == null)
				return;
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		for (PlayerStat stat : stats.values()) {
			if (stat == null || getServer().getPlayer(stat.getName()) == null) {
				stat.unload = true;
				continue;
			}
			updateTimedStats(stat);
			stat.save(false);
		}
		StatsSQLConnectionManager.closeConnection(StatsSettings.useMySQL);
		for (PlayerStat stat : stats.values()) {
			if (!stat.unload)
				continue;
			LogError("onPlayerQuit did not happen, unloading " + stat.getName() + " now");
			logout(stat.getName());
			unload(stat.getName());
		}
	}

	public void setItems(itemResolver items) {
		this.items = items;
	}

	public itemResolver getItems() {
		return items;
	}

	public void login(Player player) {
		int lastLog = get(player.getName(), defaultCategory, "lastlogin");
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (now - lastLog > StatsSettings.loginRateLimit) {
			updateStat(player, "login", true);
		}
		setStat(player.getName(), defaultCategory, "lastlogin", now);
	}

	public void logout(String player) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		setStat(player, defaultCategory, "lastlogout", now);
	}

	public void updateVehicleEnter(String player, Vehicle vhc) {
		if (!enabled)
			return;
		if (player == null || player.length() < 1) {
			LogError("updateVehicleEnter got empty player for " + player);
			return;
		}
		PlayerStat ps = stats.get(player);
		if (ps == null)
			return;
		int now = (int) (System.currentTimeMillis() / 1000L);

		if (vhc instanceof org.bukkit.entity.Boat) {
			if (now - ps.getLastBoatEnter() > 60) {
				updateStat(player, "boat", "enter", 1, true);
				ps.setLastBoatEnter(now);
			}

		} else if (vhc instanceof org.bukkit.entity.Minecart) {
			if (now - ps.getLastMinecartEnter() > 60) {
				updateStat(player, "minecart", "enter", 1, true);
				ps.setLastMinecartEnter(now);
			}
		}
	}

    public void setStats(HashMap<String, PlayerStat> stats) {
	    this.stats = stats;
    }
    
    public HashMap<String, PlayerStat> getStats() {
	    return stats;
    }
}