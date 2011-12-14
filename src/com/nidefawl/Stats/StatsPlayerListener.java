package com.nidefawl.Stats;

import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftItem;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class StatsPlayerListener extends PlayerListener {

	public HashMap<String, Float> distWalked = new HashMap<String, Float>();
	protected Stats plugin;

	public StatsPlayerListener(Stats plugin) {
		this.plugin = plugin;
	}

	/**
	 * Called when a player leaves a server
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		plugin.logout(event.getPlayer().getName());
		plugin.unload(event.getPlayer().getName());

	}

	/**
	 * Called when a player sends a chat message
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		if (event.isCancelled())
			return;
		plugin.updateStat(event.getPlayer(), "chat", false);
		plugin.updateStat(event.getPlayer(), "chatletters", event.getMessage().length(), false);
	}

	/**
	 * Called when a player attempts to use a command
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (event.isCancelled())
			return;
		plugin.updateStat(event.getPlayer(), "command", false);
	}

	/**
	 * Called when a player attempts to move location in a world
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.isCancelled())
			return;
		if (distWalked.containsKey(event.getPlayer().getName())) {
			float newDistance = distWalked.get(event.getPlayer().getName()) + (float) (event.getFrom().toVector().distance(event.getTo().toVector()));
			distWalked.put(event.getPlayer().getName(), newDistance);
		} else {
			distWalked.put(event.getPlayer().getName(), (float) (event.getFrom().toVector().distance(event.getTo().toVector())));
		}
	}

	/**
	 * Called when a player attempts to teleport to a new location in a world
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (event.isCancelled())
			return;
		if(event.getFrom().toVector().distance(event.getTo().toVector())>5)
		plugin.updateStat(event.getPlayer(), "teleport", true);
	}

	/**
	 * Called when a player uses an item
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerInteract(PlayerInteractEvent  event) {
		if (event.isCancelled() || event.getBlockFace() == null)
			return;
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getClickedBlock().getType() == Material.CHEST) {
				plugin.updateStat(event.getPlayer(), "openchest", true);
			}
		} else
		switch (event.getMaterial()) {
		case LAVA_BUCKET:
		case WATER_BUCKET:
		case SIGN:
		case BUCKET:
		case CAKE_BLOCK:
		case FLINT_AND_STEEL:
		case TNT:
			plugin.updateStat(event.getPlayer(), "itemuse", plugin.getItems().getItem(event.getMaterial().getId()), 1, false);
			break;
		default:
			break;
		}

	}

	/**
	 * Called when a player attempts to log in to the server
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		plugin.load(event.getPlayer());
		plugin.login(event.getPlayer());
	}

	/**
	 * Called when a player plays an animation, such as an arm swing
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerAnimation(PlayerAnimationEvent event) {
		if (event.getAnimationType() == PlayerAnimationType.ARM_SWING && event.getPlayer().getItemInHand().getType() == Material.AIR) {
			plugin.updateStat(event.getPlayer(), "armswing", false);
		}
	}

	/**
	 * Called when a player throws an egg and it might hatch
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerEggThrow(PlayerEggThrowEvent event) {
		plugin.updateStat(event.getPlayer(), "eggthrow", false);
	}

	/**
	 * Called when a player drops an item from their inventory
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (event.isCancelled())
			return;
		if (event.getItemDrop() instanceof CraftItem) {
			if (((CraftItem) event.getItemDrop()).getItemStack() == null)
				return;
			ItemStack blockKey = ((CraftItem) event.getItemDrop()).getItemStack();
			int blockID = blockKey.getTypeId();
			MaterialData blockByte = blockKey.getData();
			int blockParam = blockByte.getData();
			String blockName = (new Integer(blockID)).toString();
			int amount = blockKey.getAmount();
			String blockInput;
			if (blockParam == 0) {
				blockInput = blockName;
			}
			else {
				blockInput = "" + blockName + ":" + blockParam;
			}
			plugin.updateStat(event.getPlayer(), "itemdrop", blockInput, amount, false);
		}

	}

	/**
	 * Called when a player gets kicked from the server
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerKick(PlayerKickEvent event) {
		if (event.isCancelled())
			return;
		plugin.updateStat(event.getPlayer(), "kick", false);
	}

	/**
	 * Called when a player respawns
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		plugin.updateStat(event.getPlayer(), "respawn", false);
	}

	/**
	 * Called when a player attempts to log in to the server
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerLogin(PlayerLoginEvent event) {
	}

	/**
	 * Called when a player picks an item up off the ground
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (event.isCancelled())
			return;
		if (event.getItem() instanceof CraftItem) {
			if (((CraftItem) event.getItem()).getItemStack() == null)
				return;
			ItemStack blockKey = ((CraftItem) event.getItem()).getItemStack();
			int blockID = blockKey.getTypeId();
			MaterialData blockByte = blockKey.getData();
			int blockParam = blockByte.getData();
			String blockName = (new Integer(blockID)).toString();
			int amount = blockKey.getAmount();
			String blockInput;
			if (blockParam == 0) {
				blockInput = blockName;
			}
			else {
				blockInput = "" + blockName + ":" + blockParam;
			}
			plugin.updateStat(event.getPlayer(), "itempickup", blockInput, amount, false);
		}
	}

	/**
	 * Called when a player opens an inventory
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onInventoryOpen(PlayerInventoryEvent event) {
	}

	/**
	 * Called when a player changes their held item
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onItemHeldChange(PlayerItemHeldEvent event) {
	}

	/**
	 * Called when a player toggles sneak mode
	 * 
	 * @param event
	 *            Relevant event details
	 */
	@Override
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
	}
}
