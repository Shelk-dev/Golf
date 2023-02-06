package com.shelk.listeners;


import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.shelk.Golf;
import com.shelk.pins.Pins;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.world.entity.decoration.EntityArmorStand;

public class JoinQuitListener implements Listener {

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {

			if (InteractListener.findBall(e.getPlayer()) != null) {
				Player p = e.getPlayer();
				Snowball ball = InteractListener.findBall(p);
				InteractListener.balls.remove(ball);
				InteractListener.ballPreviousLoc.remove(p.getName());
				InteractListener.ballSpawnName.remove(p.getName());
				ball.remove();

			}
			if (InteractListener.getPins(e.getPlayer()) != null) {
				for (Player p : Bukkit.getOnlinePlayers()) {
					for (EntityArmorStand standPacket : InteractListener.getPins(e.getPlayer()).getPackets()) {
						((CraftPlayer) p).getHandle().b.sendPacket(new PacketPlayOutEntityDestroy(standPacket.getId()));
					}
				}
				Pins.allPins.remove(InteractListener.getPins(e.getPlayer()));
			}
		}
	

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {

		// Send pins packets
		for (Pins pins : Pins.allPins) {
			for (EntityArmorStand pin : pins.getPackets()) {
				PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving(pin);
				for (Player p : Bukkit.getOnlinePlayers()) {

					((CraftPlayer) p).getHandle().b.sendPacket(packet);
					Pins.setHelmet(Pins.getPinItem(Golf.config.getInt("pinModelData")), p, pin);
					
					pin.setHeadRotation(0);
					
					PacketPlayOutEntityMetadata entityMetaPacket = new PacketPlayOutEntityMetadata(
							pin.getId(), pin.getDataWatcher(), true);
					((CraftPlayer) p).getHandle().b.sendPacket(entityMetaPacket);
				}
			}
		}

	}

}
