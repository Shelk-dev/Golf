package com.shelk.listeners;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.shelk.Golf;
import com.shelk.GolfArea;

public class BreakListener implements Listener {

	@SuppressWarnings("unchecked")
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBreak(BlockBreakEvent e) {
		if (e.getBlock().getType() == Material.CAULDRON) {
			for (GolfArea area : Golf.mySql.getAreas()) {
				if (area.getCauldrons() != null) {
					for (Entry<String, Location> cauldron : ((HashMap<String, Location>) area.getCauldrons().clone())
							.entrySet())
						if (cauldron.getValue().distance(e.getBlock().getLocation()) < 1) {
							e.getPlayer().sendMessage("§eThe hole §a" + cauldron.getKey() + " §efrom area §a"
									+ area.getAreaId() + " §ewas removed.");
							area.getCauldrons().remove(cauldron.getKey());
							Golf.mySql.updateArea(area);

						}
				}
			}
		}
	}

}
