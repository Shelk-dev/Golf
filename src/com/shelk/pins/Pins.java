package com.shelk.pins;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import com.mojang.datafixers.util.Pair;
import com.shelk.Golf;
import com.shelk.utils.Utils;

import net.minecraft.core.Vector3f;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.decoration.EntityArmorStand;

public class Pins {

	final double[][][] directions = new double[][][] {
		{
		  {0, 0}, {-0.25, 0.5}, {0.25, 0.5}, {-0.5, 1}, {0, 1}, {0.5, 1}, {-0.75, 1.5},  {-0.25, 1.5}, {0.25, 1.5}, {0.75, 1.5}
		},
		{
			 {0, 0}, {0.25, -0.5}, {-0.25, -0.5}, {-0.5, -1}, {0, -1}, {0.5, -1}, {-0.75, -1.5}, {-0.25, -1.5}, {0.25, -1.5}, {0.75, -1.5}
		},
		{
		  {0, 0}, {0.5, 0.25}, {0.5, -0.25}, {1, -0.5}, {1, 0}, {1, 0.5}, {1.5, -0.75}, {1.5, -0.25}, {1.5, 0.25}, {1.5, 0.75}
			},
		{
				{0, 0}, {-0.5, 0.25}, {-0.5, -0.25}, {-1, -0.5}, {-1, 0}, {-1, 0.5}, {-1.5, -0.75}, {-1.5, -0.25}, {-1.5, 0.25}, {-1.5, 0.75}
			}
	};

	public static ArrayList<Pins> allPins = new ArrayList<>();

	Player player;
	String name;
	Location loc;

	ArrayList<EntityArmorStand> packets = new ArrayList<>();

	
	
	public ArrayList<EntityArmorStand> getPackets() {
		return packets;
	}

	public void setPackets(ArrayList<EntityArmorStand> packets) {
		this.packets = packets;
	}

	public Pins(Player player, Location loc, String name) {
		this.player = player;
		this.loc = loc;
		this.name = name;
		allPins.add(this);
	}

	public void spawn() {
		switch (Utils.getCardinalDirection(loc)) {
		case "N":
			spawn(0);
			break;
		case "S":
			spawn(1);
			break;
		case "W":
			spawn(2);
			break;
		case "E":
			spawn(3);
			break;
		}
	}

	public void spawn(int direction) {
		double[][] coords = directions[direction];
		for (double[] pinCoord : coords) {
			Vector vec = new Vector(pinCoord[0], 0, pinCoord[1]);
			Location newLoc = loc.clone().add(vec).add(0,-0.4,0);

			WorldServer s = ((CraftWorld) newLoc.getWorld()).getHandle();
			EntityArmorStand armorStand = new EntityArmorStand(s, newLoc.getX(), newLoc.getY(), newLoc.getZ());

			armorStand.setInvisible(true);

			
			packets.add(armorStand);
			
			PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving(armorStand);

			for (Player p : Bukkit.getOnlinePlayers()) {

				((CraftPlayer) p).getHandle().b.sendPacket(packet);

				setHelmet(getPinItem(Golf.config.getInt("pinModelData")), p, armorStand);
				armorStand.setHeadPose(new Vector3f(direction, direction, direction));
				PacketPlayOutEntityMetadata entityMetaPacket = new PacketPlayOutEntityMetadata(armorStand.getId(),
						armorStand.getDataWatcher(), true);
				((CraftPlayer) p).getHandle().b.sendPacket(entityMetaPacket);
				
				
			}
		}

	}


	public static ItemStack getPinItem(int modelData) {
		ItemStack itemPin = new ItemStack(Material.valueOf(Golf.config.getString("pinMaterial")));
		ItemMeta pinMeta = itemPin.getItemMeta();
		pinMeta.setCustomModelData(modelData);
		itemPin.setItemMeta(pinMeta);
		return itemPin;
	}

	public static void setHelmet(ItemStack helmet, Player p, EntityArmorStand stand) {
		Pair<EnumItemSlot, net.minecraft.world.item.ItemStack> pair = new Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>(
				EnumItemSlot.f, CraftItemStack.asNMSCopy(helmet));
		List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> pairs = new ArrayList<>();
		pairs.add(pair);
		PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(stand.getId(), pairs);
		((CraftPlayer) p).getHandle().b.sendPacket(packet);
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public Location getLocation() {
		return loc;
	}

	public void setLocation(Location loc) {
		this.loc = loc;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
