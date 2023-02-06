package com.shelk.placeholder;

import org.bukkit.OfflinePlayer;

import com.shelk.Golf;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class GolfPlaceholder extends PlaceholderExpansion {

	Golf plugin;

	public GolfPlaceholder(Golf golf) {
		this.plugin = golf;
	}

	@Override
	public String getAuthor() {
		return "Shelk";
	}

	@Override
	public String getIdentifier() {
		return "golf";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getRequiredPlugin() {
		return "Golf";
	}

	@Override
	public boolean canRegister() {
		return true;
	}

	@Override
	public String onRequest(OfflinePlayer player, String params) {
		if (params.equals("holes_in_one")) {
			return "" + Golf.mySql.getScoresInOne(player.getUniqueId().toString());
		}
		return null;

	}

}
