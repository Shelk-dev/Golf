package com.shelk.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class Utils {

	public static ItemStack getItem(ItemStack is, String name, List<String> lore, int modelData) {
		ItemMeta meta = is.getItemMeta();
		if (name != null)
			meta.setDisplayName(name);
		if (lore != null) {
			List<String> loreL = new ArrayList<>();
			for (String line : lore)
				loreL.add(line.replace("&", "§"));
			meta.setLore(loreL);
		}
		meta.setCustomModelData(modelData);
		is.setItemMeta(meta);

		return is;
	}

	public static ItemStack getItem(ConfigurationSection section) {
		ItemStack is;
		if (!section.isString("name")) return null;
		String name =  section.getString("name").replace("&", "§");
		
		is = new ItemStack(Material.valueOf(section.getString("material").toUpperCase().replace(" ", "_")));
		return getItem(is, name, section.getStringList("lore"), section.getInt("modeldata"));
	}

	public static String getStringLocation(Location l) {
		if (l == null)
			return null;
		return String.valueOf(l.getWorld().getName()) + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ()
				+ ":" + l.getYaw() + ":" + l.getPitch();
	} 
	
    public static float getLookAtYaw(Vector motion) {
        double dx = motion.getX();
        double dz = motion.getZ();
        double yaw = 0;
        // Set yaw
        if (dx != 0) {
            // Set yaw start value based on dx
            if (dx < 0) {
                yaw = 1.5 * Math.PI;
            } else {
                yaw = 0.5 * Math.PI;
            }
            yaw -= Math.atan(dz / dx);
        } else if (dz < 0) {
            yaw = Math.PI;
        }
        return (float) (-yaw * 180 / Math.PI - 90);
    }

	public static Location getLocationString(String s) {
		if (s == null || s.trim() == "")
			return null;
		String[] parts = s.split(":");
		if (parts.length == 6) {
			World w = Bukkit.getServer().getWorld(parts[0]);
			double x = Double.parseDouble(parts[1]);
			double y = Double.parseDouble(parts[2]);
			double z = Double.parseDouble(parts[3]);
			double yaw = Double.parseDouble(parts[4]);
			double pitch = Double.parseDouble(parts[5]);

			return new Location(w, x, y, z, (float) yaw, (float) pitch);
		}
		return null;
	}

	public static String convertMap(HashMap<String, Location> map) {
		if (map == null || map.size() == 0)
			return null;

		StringBuilder mapAsString = new StringBuilder();
		for (String key : map.keySet()) {
			mapAsString.append(key + "=" + getStringLocation(map.get(key)) + ",");
		}
		mapAsString.delete(mapAsString.length() - 2, mapAsString.length());
		return mapAsString.toString();
	}

	public static HashMap<String, Location> convertString(String map) {
		if (map == null || map.length() == 0)
			return null;

		String[] keyValues = map.split(",");
		HashMap<String, Location> returnMap = new HashMap<>();
		for (String keyValue : keyValues) {
			returnMap.put(keyValue.split("=")[0], getLocationString(keyValue.split("=")[1]));
		}
		return returnMap;
	}

	public static String getCardinalDirection(Location loc) {

		double rotation = (loc.getYaw() - 90.0F) % 360.0F;

		if (rotation < 0.0D) {
			rotation += 360.0D;
		}
		if ((0.0D <= rotation) && (rotation < 45.0D))
			return "W";
		if ((45.0D <= rotation) && (rotation < 135.0D))
			return "N";
		if ((135.0D <= rotation) && (rotation < 225.0D))
			return "E";
		if ((225.0D <= rotation) && (rotation < 315.0D))
			return "S";
		if ((315.0D <= rotation) && (rotation < 360.0D)) {
			return "W";
		}
		return null;
	}

}
