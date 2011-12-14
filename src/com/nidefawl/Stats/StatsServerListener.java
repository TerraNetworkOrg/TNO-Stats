package com.nidefawl.Stats;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

public class StatsServerListener extends ServerListener {
	Stats stats = null;
	public StatsServerListener(Stats plugin) {
		this.stats = plugin;
	}
    @Override
    public void onPluginEnable(PluginEnableEvent event) {
    }

    /**
     * Called when a plugin is disabled
     *
     * @param event Relevant event details
     */
    @Override
    public void onPluginDisable(PluginDisableEvent event) {
    }

}
