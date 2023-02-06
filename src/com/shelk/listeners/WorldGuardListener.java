package com.shelk.listeners;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.shelk.Golf;
import com.shelk.GolfArea;
import com.shelk.pins.Pins;

import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.raidstone.wgevents.events.RegionLeftEvent;

public class WorldGuardListener implements Listener {

	@EventHandler
	public void onLeave(RegionLeftEvent e) {
		if (e.getPlayer() != null) {
			GolfArea area = Golf.mySql.getArea(e.getRegion().getId());
			if (area != null) {
				Player p = e.getPlayer();
				Snowball ball = InteractListener.findBall(p);
				if (ball != null) {
					InteractListener.balls.remove(ball);
					ball.remove();
					if (InteractListener.getPins(e.getPlayer()) != null) {
						for (Player player : Bukkit.getOnlinePlayers()) {
							for (EntityArmorStand standPacket : InteractListener.getPins(e.getPlayer()).getPackets()) {
								(((CraftPlayer) player).getHandle()).b.sendPacket(
										 new PacketPlayOutEntityDestroy(new int[] { standPacket.getId() }));
							}
						}
						Pins.allPins.remove(InteractListener.getPins(e.getPlayer()));
					}
				}

			}
		}
	}
}
