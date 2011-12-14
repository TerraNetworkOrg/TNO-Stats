package com.nidefawl.Stats.udpates;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.nidefawl.Stats.Stats;
import com.nidefawl.Stats.StatsSettings;
import com.nidefawl.Stats.ItemResolver.hModItemResolver;
import com.nidefawl.Stats.ItemResolver.itemResolver;
import com.nidefawl.Stats.datasource.StatsSQLConnectionManager;

public class Update2 {
	public static void execute(Stats plugin) {
		FileWriter writer;
		@SuppressWarnings("unused")
		itemResolver items = new hModItemResolver(new File(plugin.getDataFolder(),"items.txt"));
		try {
			writer = new FileWriter(new File(plugin.getDataFolder(),"items.txt"),true);
			if (writer != null) {
				try {
					writer.close();
					plugin.setItems( new hModItemResolver(new File(plugin.getDataFolder(),"items.txt")));
				} catch (IOException e) {
					Stats.LogError("Exception while updating "+plugin.getDataFolder().getPath()+"/items.txt "+ e);
					e.printStackTrace();
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	static void UpdateItemStatKey(String oldKey,String newKey) {

		int result = 0;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = StatsSQLConnectionManager.getConnection(StatsSettings.useMySQL);
			ps = conn.prepareStatement("UPDATE " + StatsSettings.dbTable + " set stat = ? where stat = ? and (category = 'blockdestroy' or category = 'blockcreate' or category = 'itemuse' or category = 'itemdrop' or category = 'itempickup');");
			ps.setString(1, newKey);
			ps.setString(2, oldKey);
			result = ps.executeUpdate();
		} catch (SQLException ex) {
			Stats.LogError("SQL exception" + ex);
			ex.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				Stats.LogError("SQL exception on close"+ ex);
				ex.printStackTrace();
			}
		}
		Stats.LogInfo("Updated " + result + " stats.");
	}
}
