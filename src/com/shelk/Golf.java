package com.shelk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.shelk.listeners.BreakListener;
import com.shelk.listeners.InteractListener;
import com.shelk.listeners.JoinQuitListener;
import com.shelk.listeners.ProjectileListener;
import com.shelk.listeners.WorldGuardListener;
import com.shelk.pins.Pins;
import com.shelk.placeholder.GolfPlaceholder;
import com.shelk.sql.MySQLAccess;
import com.shelk.utils.Utils;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.world.entity.decoration.EntityArmorStand;

public class Golf extends JavaPlugin implements CommandExecutor {

	public static Plugin pl;
	public static FileConfiguration config;
	public static MySQLAccess mySql;

	@Override
	public void onEnable() {
		pl = this;
		saveDefaultConfig();
		config = getConfig();

		getCommand("golf").setExecutor(this);
		getCommand("golfgive").setExecutor(this);

		Bukkit.getPluginManager().registerEvents(new BreakListener(), this);
		Bukkit.getPluginManager().registerEvents(new JoinQuitListener(), this);
		Bukkit.getPluginManager().registerEvents(new InteractListener(), this);
		Bukkit.getPluginManager().registerEvents(new ProjectileListener(), this);
		Bukkit.getPluginManager().registerEvents(new WorldGuardListener(), this);
		mySql = new MySQLAccess(config);
		if (!mySql.init()) {
			for (int i = 0; i < 3; i++)
				Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_RED + "[Golf] NO ACCESS TO DATABASE !");
			this.setEnabled(false);
		} else {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[Golf] Access to database");
		}

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new GolfPlaceholder(this).register();
		}

	}

	@Override
	public void onDisable() {
		for (Snowball ball : InteractListener.balls) {
			ball.remove();
		}
		// Remove pins packets
		for (Pins pins : Pins.allPins) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				for (EntityArmorStand standPacket : pins.getPackets()) {
					((CraftPlayer) player).getHandle().b
							.sendPacket(new PacketPlayOutEntityDestroy(standPacket.getId()));
				}
			}
		}
	}

	public static BukkitRunnable particles = null;

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("golf")) {
			if (sender.hasPermission("golf.admin") && sender instanceof Player) {
				Player p = (Player) sender;
				if (args.length > 0) {
					boolean bowling = false;
					if (findGolfArea(p) != null && findGolfArea(p).getAreaId().contains("bowling"))
						bowling = true;

					if (args[0].equals("addgolfarea")) {
						if (args.length > 1) {
							String area = args[1];
							ProtectedRegion r = WorldGuard.getInstance().getPlatform().getRegionContainer()
									.get(BukkitAdapter.adapt(p.getWorld())).getRegion(area);
							if (r != null) {
								if (mySql.getArea(area) == null) {
									mySql.addGolfArea(new GolfArea(area));
									p.sendMessage("§eRegion §a" + area + " §ewas registered as a golf area.");
								} else
									p.sendMessage("§cThis region is already registered as a golf area !");
							} else
								p.sendMessage("§cRegion not found. Are you in the same world ?");

						} else
							p.sendMessage("§c/golf addgolfarea [Name]");
					}
					if (args[0].equals("removegolfarea")) {
						if (args.length > 1) {
							GolfArea area = mySql.getArea(args[1]);
							if (area != null) {
								mySql.deleteArea(area.getAreaId());
								p.sendMessage("§eGolf area§a " + area.getAreaId() + " §ewas deleted.");

							} else
								p.sendMessage("§cArea not found.");

						} else
							p.sendMessage("§c/golf addgolfarea [Name]");
					}
					if (args[0].equals("addspawn")) {
						if (args.length > 1) {
							GolfArea area = findGolfArea(p);
							if (area != null) {

								area.getBalls().put(args[1], p.getLocation().add(0, -1, 0));
								mySql.updateArea(area);
								p.sendMessage("§eBall spawn §a" + args[1] + " §eadded to golf area §a"
										+ area.getAreaId() + "§e.");

							} else
								p.sendMessage("§cNo golf area found on this location.");
						} else
							p.sendMessage("§c/golf addspawn [Name]");
					}
					if (args[0].equals("removespawn")) {
						GolfArea area = findGolfArea(p);
						if (area != null) {
							String closest = null;
							for (Entry<String, Location> ball : area.getBalls().entrySet()) {
								if (closest == null || p.getLocation().distance(ball.getValue()) < p.getLocation()
										.distance(area.getBalls().get(closest))) {
									closest = ball.getKey();
								}
							}

							if (closest != null) {
								area.getBalls().remove(closest);

								mySql.updateArea(area);
								p.sendMessage("§eThe closest ball spawn from golf area §a" + area.getAreaId()
										+ "§e was removed.");
							} else
								p.sendMessage("§cThere's no ball spawn in this area.");
						} else
							p.sendMessage("§cNo golf area found on this location.");
					}
					if (args[0].equals("sethole")) {
						if (args.length > 1) {
							String name = args[1];
							GolfArea area = findGolfArea(p);
							if (area != null) {
								if (!bowling) {
									if (p.getLocation().getBlock().getRelative(BlockFace.DOWN)
											.getType() == Material.CAULDRON) {
										if (area.getCauldrons() != null)
											area.getCauldrons().put(name,
													p.getLocation().add(0, -1, 0).getBlock().getLocation());
										else {
											HashMap<String, Location> cauldrons = new HashMap<>();
											cauldrons.put(name, p.getLocation().add(0, -1, 0).getBlock().getLocation());
											area.setCauldron(cauldrons);
										}
										mySql.updateArea(area);

										p.sendMessage("§eHole §a" + name + " §ecreated.");
									} else
										p.sendMessage("§cNo cauldron found.");
								} else {
									if (area.getCauldrons() != null)
										area.getCauldrons().put(name, p.getLocation().add(0, -1, 0));
									else {
										HashMap<String, Location> cauldrons = new HashMap<>();
										cauldrons.put(name, p.getLocation().add(0, -1, 0));
										area.setCauldron(cauldrons);
									}

									area.getCauldrons().put(name, p.getLocation().add(0, -1, 0));
									mySql.updateArea(area);
									p.sendMessage("§ePins §a" + name + " §eplaced.");
								}

							} else
								p.sendMessage("§cArea not found.");

						} else
							p.sendMessage("§c/golf sethole [Name]");
					}
					if (args[0].equals("listspawns")) {
						if (particles != null) {
							particles.cancel();
							particles = null;
							p.sendMessage("§eParticles off");
						} else {
							particles = new BukkitRunnable() {
								public void run() {
									for (GolfArea area : mySql.getAreas()) {

										for (Location ball : area.getBalls().values()) {
											ball.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, ball.getX() + 0.5,
													ball.getY() + 1, ball.getZ() + 0.5, 30, 0.2, 0.2, 0.2, 0.01);
										}
									}
								}
							};
							particles.runTaskTimer(this, 0, 20);
							p.sendMessage("§eParticles on");

						}

					}

				} else {
					for (String line : config.getStringList("help-admin")) {
						p.sendMessage(line.replace("&", "§"));
					}
				}
			}
			if (sender.hasPermission("golf.player") && sender instanceof Player) {
				Player p = (Player) sender;
				if (args.length > 0) {
					if (args[0].equals("newball")) {
						if (InteractListener.findBall(p) == null)
							giveNewBall(p, true);
						else
							p.sendMessage(config.getString("already-placed-ball").replace("&", "§"));
					}
					if (args[0].equals("hide")) {
						if (Golf.mySql.getHide(p.getUniqueId().toString())) {
							Golf.mySql.setHide(p.getUniqueId().toString(), false);
							p.sendMessage(config.getString("hide-off").replace("&", "§"));
							if (InteractListener.findBall(p) != null) {
								InteractListener.findBall(p).setCustomNameVisible(true);
							}
						} else {
							Golf.mySql.setHide(p.getUniqueId().toString(), true);
							p.sendMessage(config.getString("hide-on").replace("&", "§"));
							if (InteractListener.findBall(p) != null) {
								InteractListener.findBall(p).setCustomNameVisible(false);
							}
						}
					}
					if (args[0].equals("stats")) {

						int page = 0;
						if (args.length > 1)
							page = Integer.parseInt(args[1]) - 1;

						ArrayList<String> scores = new ArrayList<>();

						p.sendMessage((config.getString("stats-first-line").replace("&", "§")).replace("%page%",
								(page + 1) + ""));

						for (GolfArea area : mySql.getAreas()) {
							for (String cauldron : area.getCauldrons().keySet()) {

								if (mySql.getScore(p, cauldron) != 0)
									scores.add(cauldron + "=" + mySql.getScore(p, cauldron));

								else
									scores.add(cauldron + "=null");

							}
						}
						Collections.sort(scores);
						if (scores.size() > page * 10) {
							for (int i = page * 10; i < (page * 10 + 10); i++) {
								if (scores.size() > i) {
									String line = scores.get(i);
									if (line.split("=")[1].equals("null")) {
										p.sendMessage((config.getString("stats-format-not-completed").replace("&", "§"))
												.replace("%cauldron%", line.split("=")[0]));
									} else {
										p.sendMessage(((config.getString("stats-format-completed").replace("&", "§"))
												.replace("%cauldron%", line.split("=")[0])).replace("%best%",
														line.split("=")[1] + ""));
									}
								}
							}
						} else
							p.sendMessage(Golf.config.getString("stats-page-doesnt-exist").replace("&", "§"));
						if (scores.size() > page * 10 + 10) {
							p.sendMessage((Golf.config.getString("stats-next-page").replace("&", "§"))
									.replace("%nextpage%", (page + 2) + ""));
						}

					}

				} else {
					for (String line : config.getStringList("help-player")) {
						p.sendMessage(line.replace("&", "§"));
					}
				}

			}
		}
		else if (command.getName().equalsIgnoreCase("golfgive")) {
			if (args.length >= 2) {
				if (Bukkit.getPlayer(args[0]) != null) {
					if (Golf.config.isConfigurationSection(args[1])) {
						if (Utils.getItem(Golf.config.getConfigurationSection(args[1])) != null ) {
							Bukkit.getPlayer(args[0]).getInventory().addItem(Utils.getItem(Golf.config.getConfigurationSection(args[1])));
						} else {
							if (args.length == 3 && Utils.getItem(Golf.config.getConfigurationSection(args[1] + "." + args[2])) != null) {
								Bukkit.getPlayer(args[0]).getInventory().addItem(Utils.getItem(Golf.config.getConfigurationSection(args[1] + "." + args[2])));
							} else sender.sendMessage("§cItem not found !");
						}
						
					} else sender.sendMessage("§cCategory not found !");
				} else {
					sender.sendMessage("§cThe player was not found !");
				}
			} 
		}
		return false;

	}

	public static void giveNewBall(Player p, boolean removePrevious) {

		ArrayList<String> balls = new ArrayList<>();
		for (String section : config.getConfigurationSection("balls").getKeys(false)) {
			if (p.hasPermission(config.getString("balls." + section + ".permission"))) {
				balls.add(section);
			}

		}
		if (removePrevious) {
			for (ItemStack item : p.getInventory().getContents()) {
				if (item != null) {
					if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
						for (String ball : balls) {
							if (config.getString("balls." + ball + ".name").replace("&", "§")
									.equals(item.getItemMeta().getDisplayName())) {
								p.getInventory().remove(item);
							}
						}
					}
				}
			}
		}

		Random r = new Random();
		ConfigurationSection randomSection = config
				.getConfigurationSection("balls." + balls.get(r.nextInt(balls.size())));
		p.getInventory().addItem(Utils.getItem(randomSection));
	}

	public static GolfArea findGolfArea(Player p) {
		Location loc = p.getLocation();
		ApplicableRegionSet set = WorldGuard.getInstance().getPlatform().getRegionContainer()
				.get(new BukkitWorld(loc.getWorld()))
				.getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
		for (ProtectedRegion region : set.getRegions()) {
			if (mySql.getArea(region.getId()) != null) {

				GolfArea area = mySql.getArea(region.getId());
				return area;
			}
		}
		return null;

	}

}
