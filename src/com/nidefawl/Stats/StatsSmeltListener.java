package com.nidefawl.Stats;


import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryListener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
public class StatsSmeltListener extends InventoryListener{
	
	private Stats plugin;
	
	public StatsSmeltListener(Stats plugin){
		this.plugin = plugin;
	}
	
	public void onFurnaceSmelt(PlayerInteractEvent event){
		if (event.isCancelled())
			return;
		if (!(event.getPlayer() instanceof Player))
			return;
		ItemStack blockKey = event.getItem();
		int blockID = blockKey.getTypeId();
		String blockName = (new Integer(blockID)).toString();
		int amount = blockKey.getAmount();
		plugin.updateStat(event.getPlayer(), "smelting", blockName, amount, true);
		plugin.updateStat(event.getPlayer(), "smelting", "total", 1, true);
		plugin.updateStat(event.getPlayer(), "smelting", "total_sum", amount, true);
	}
}
