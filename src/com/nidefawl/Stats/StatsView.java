package com.nidefawl.Stats;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import com.nidefawl.Stats.datasource.Category;
import com.nidefawl.Stats.datasource.PlayerStat;

public class StatsView {

	public static boolean onCommand(Stats plugin, CommandSender sender, String[] args) {
		Player player = (Player) sender;
		if (!plugin.permission.has(player, "stats.view.own")) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to view your stats!");
			return true;
		}
		Player who = null;
		if (sender instanceof Player) {
			who = (Player) sender;
		}
		int offs = 0;
		if (args.length > 0) {
			who = playerMatch(plugin.getServer(), args[0]);
			if (who != null) {
				if (!plugin.permission.has(player, "stats.view.others")) {
					sender.sendMessage(ChatColor.RED + "You don't have permission to view others stats!");
					return true;
				}
				offs++;
			} else {
				if ((sender instanceof ConsoleCommandSender)) {
					sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' is not online!");
					return false;
				} else {
					who = (Player) sender;
				}
			}
		}
		if (args.length == offs + 1) {
			if (isStat(plugin, who.getName(), args[offs])) {
				printStat(plugin, sender, who, "stats", args[offs]);
				return true;
			} else if (plugin.getItems().getItem(args[offs]) != 0 && !(args[offs].equals("boat") || args[offs].equals("minecart"))) {
				printStat(plugin, sender, who, "blockcreate", args[offs]);
				printStat(plugin, sender, who, "blockdestroy", args[offs]);
				return true;
			} else if (isCat(plugin, who.getName(), args[offs])) {
				sender.sendMessage(StatsSettings.premessage + "Please choose: (/stats " + args[offs] + " <stat-name>)");
				sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + getCatEntries(plugin, who.getName(), args[offs]));
				return true;
			} else {
				sender.sendMessage(StatsSettings.premessage + ChatColor.RED + "stat/category '" + args[offs] + "' not found. Possible values:");
				sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + getCats(plugin, who.getName()));
				return true;
			}
		} else if (args.length == offs + 2) {
			if (isCat(plugin, who.getName(), args[offs])) {
				printStat(plugin, sender, who, args[offs], args[offs + 1]);
				return true;
			} else {
				sender.sendMessage(StatsSettings.premessage + ChatColor.RED + "stat/category '" + args[offs] + "' not found. Possible values:");
				sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + getCats(plugin, who.getName()));
				return true;
			}
		}
		int playedTime = plugin.get(who.getName(), "stats", "playedfor");
		int movedBlocks = plugin.get(who.getName(), "stats", "move");
		int totalCreate = plugin.get(who.getName(), "stats", "totalblockcreate");
		int totalDestroy = plugin.get(who.getName(), "stats", "totalblockdestroy");
		int totalCrafting = plugin.get(who.getName(), "crafting", "total_sum");
		int totalCraftingProcess = plugin.get(who.getName(), "crafting", "total");
		int tkills = plugin.get(who.getName(), "kills", "total");
		int tdeaths = plugin.get(who.getName(), "deaths", "total");
		int pdeaths = plugin.get(who.getName(), "deaths", "player");
		int pkills = plugin.get(who.getName(), "kills", "player");
		int totalDamage = plugin.get(who.getName(), "damagetaken", "total");
		int totalDamageDealt = plugin.get(who.getName(), "damagedealt", "total");
		try {
			sender.sendMessage("------------------------------------------------");
			sender.sendMessage(ChatColor.GOLD + " stats for " + ChatColor.WHITE + who.getName() + ChatColor.GOLD + ": (" + ChatColor.WHITE + "/stats help for more" + ChatColor.GOLD + ")");
			sender.sendMessage("------------------------------------------------");
			String s1 = ChatColor.GOLD + "[" + ChatColor.YELLOW + "Playedtime" + ChatColor.GOLD + "]" + ChatColor.YELLOW;
			while (MinecraftFontWidthCalculator.getStringWidth(sender, s1) < 120)
				s1 += " ";
			s1 += ChatColor.WHITE + GetTimeString(playedTime);
			sender.sendMessage(s1);
			s1 = ChatColor.GOLD + "[" + ChatColor.YELLOW + "Moved" + ChatColor.GOLD + "]" + ChatColor.YELLOW;
			while (MinecraftFontWidthCalculator.getStringWidth(sender, s1) < 120)
				s1 += " ";
			s1 += ChatColor.WHITE + String.valueOf(movedBlocks) + " blocks";
			sender.sendMessage(s1);
			printStatFormatted(sender, "Blocks", "created", totalCreate, "destroyed", totalDestroy);
			printStatFormatted(sender, "Crafted", "operations", totalCraftingProcess, "items", totalCrafting);
			printStatFormatted(sender, "Deaths", "total", tdeaths, "player", pdeaths);
			printStatFormatted(sender, "Kills", "total", tkills, "player", pkills);
			printStatFormatted(sender, "Damage", "dealt", totalDamageDealt, "taken", totalDamage);
			sender.sendMessage("------------------------------------------------");
		} catch (Exception e) {
			// TODO: handle exception
		}
		return true;
	}

	public static Player playerMatch(Server server, String name) {
		List<Player> list = server.matchPlayer(name);
		for (Player p : list)
			if (p != null && p.getName().equalsIgnoreCase(name))
				return p;
		return null;
	}

	private static void printStatFormatted(CommandSender sender, String name, String title1, int value1, String title2, int value2) {
		String s1 = ChatColor.GOLD + "[" + ChatColor.YELLOW + name + ChatColor.GOLD + "]" + ChatColor.YELLOW;
		while (MinecraftFontWidthCalculator.getStringWidth(sender, s1) < 120)
			s1 += " ";
		if (title2 != null)
			s1 += ChatColor.WHITE + title1 + "/" + title2;
		else
			s1 += ChatColor.WHITE + title1;
		while (MinecraftFontWidthCalculator.getStringWidth(sender, s1) < 240)
			s1 += " ";
		if (title2 != null)
			s1 += value1 + "/" + value2;
		else
			s1 += value1;
		sender.sendMessage(s1);
	}

	public static boolean isCat(Stats plugin, String player, String category) {
		PlayerStat ps = plugin.getStats().get(player);
		if (ps == null)
			return false;
		Category cat = ps.get(category);
		if (cat == null)
			return false;
		return true;
	}

	public static void printStat(Stats plugin, CommandSender sendTo, Player statPlayer, String cat, String stat) {
		long statVal = plugin.get(statPlayer.getName(), cat, stat);
		String statString = "" + statVal;
		if (stat.equalsIgnoreCase("playedfor")) {
			statString = GetTimeString((int) statVal);
		}
		if (stat.equalsIgnoreCase("lastlogout") || stat.equalsIgnoreCase("lastlogin")) {
			Date logDate = new Date(statVal * 1000);
			SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yy hh:mm");
			statString = format.format(logDate);
		}
		sendTo.sendMessage(StatsSettings.premessage + cat + "/" + stat + ": " + ChatColor.WHITE + statString);
	}

	public static String GetTimeString(int Seconds) {
		int days = (int) Math.ceil(Seconds / (24 * 3600));
		int hours = (int) Math.ceil((Seconds - (24 * 3600 * days)) / 3600);
		int minutes = (int) Math.ceil((Seconds - (24 * 3600 * days + 3600 * hours)) / 60);
		String timeString = "";
		timeString += days + "d " + hours + "h " + minutes + "m";
		return timeString;
	}

	public static String getCatEntries(Stats plugin, String player, String category) {
		PlayerStat ps = plugin.getStats().get(player);
		if (ps == null)
			return "player not found";
		Set<String> cats = ps.getCats();
		if (cats.size() == 0)
			return "no categories founnd";
		Category cat = ps.get(category);
		if (cat == null)
			return "category not found";
		Set<String> entris = cat.getEntries();
		int length = (entris.size() - 1);
		int on = 0;
		String list = "";
		for (String currentName : entris) {
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

	public static String getCats(Stats plugin, String player) {
		PlayerStat ps = plugin.getStats().get(player);
		if (ps == null)
			return "no categories found";
		Set<String> cats = ps.getCats();
		if (cats.size() == 0)
			return "no categories found";
		int length = (cats.size() - 1);
		int on = 0;
		String list = "";
		for (String currentName : cats) {
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

	public static boolean isStat(Stats plugin, String player, String stat) {
		PlayerStat ps = plugin.getStats().get(player);
		if (ps == null)
			return false;
		Category cat = ps.get("stats");
		if (cat == null)
			return false;
		if (cat.get(stat) == 0)
			return false;
		return true;
	}
}
