package com.shelk.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftSnowball;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.shelk.Golf;
import com.shelk.GolfArea;
import com.shelk.pins.Pins;
import com.shelk.utils.Utils;
import net.minecraft.core.Vector3f;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.projectile.EntitySnowball;

@SuppressWarnings("deprecation")
public class InteractListener implements Listener {

	public static ArrayList<Snowball> balls = new ArrayList<>();
	public static HashMap<String, Location> ballPreviousLoc = new HashMap<>();
	public static HashMap<String, String> ballSpawnName = new HashMap<>();

	@EventHandler
	public void onSwap(PlayerSwapHandItemsEvent e) {
		GolfArea area = Golf.findGolfArea(e.getPlayer());
		if (area != null)
			e.setCancelled(true);
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (e.getCurrentItem() != null) {
			GolfArea area = Golf.findGolfArea((Player) e.getWhoClicked());
			if (area != null)
				if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
					e.setCancelled(true);
				}
		}

	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent e) {
		GolfArea area = Golf.findGolfArea(e.getPlayer());
		if (area != null)
			e.setCancelled(true);
	}

	@EventHandler
	public void onDrop(PlayerPickupItemEvent e) {
		GolfArea area = Golf.findGolfArea(e.getPlayer());
		if (area != null)
			e.setCancelled(true);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND) {
			ItemStack item = e.getItem();
			Player p = e.getPlayer();
			// Pickup
			if (e.getAction().name().contains("RIGHT")) {
				GolfArea area = Golf.findGolfArea(p);
				if (area != null) {

					Snowball ball = findBall(p);
					if (ball != null) {
						Vector playerLookDir = p.getEyeLocation().getDirection();
						Location eyeLoc = p.getEyeLocation();
						World world = p.getWorld();

						if (eyeLoc.distance(ball.getLocation()) < 3.5) {
							for (Entity en : world.getNearbyEntities(
									eyeLoc.add(playerLookDir.multiply(ball.getLocation().distance(eyeLoc))), 0.1, 0.1,
									0.1)) {
								if (en instanceof Snowball && ((Snowball) en).getShooter().equals(p)) {
									// Pickup
									if (e.getPlayer().isSneaking()) {
										p.getInventory().addItem(ball.getItem());
										balls.remove(ball);
										ballPreviousLoc.remove(p.getName());
										ballSpawnName.remove(p.getName());
										ball.remove();
										if (InteractListener.getPins(p) != null) {
											for (Player player : Bukkit.getOnlinePlayers()) {
												for (EntityArmorStand standPacket : InteractListener.getPins(p)
														.getPackets()) {
													((CraftPlayer) player).getHandle().b.sendPacket(
															new PacketPlayOutEntityDestroy(standPacket.getId()));
												}
											}
											Pins.allPins.remove(InteractListener.getPins(p));

										}
										return;

									} else {
										if (ball.getVelocity().getX() < 0.01 && ball.getVelocity().getY() < 0.01
												&& ball.getVelocity().getZ() < 0.01)
											ball.teleport(ball.getLocation().add(0, -0.5, 0).getBlock().getLocation()
													.add(0.5, 1.3, 0.5));
									}

								}
							}
						}

					}

				}
			}
			if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				// Change ball spot - Bowling
				GolfArea area = Golf.findGolfArea(p);
				if (area != null && area.getAreaId().contains("bowling")) {
					if (isBallSpawn(e.getClickedBlock(), area)) {
						if (getPins(p) != null) {
							String ballSpawn = getBallSpawn(e.getClickedBlock(), area);
							String idPins = ballSpawn.replaceAll("[0-9]", "");
							if (idPins.equalsIgnoreCase(getPins(p).getName())) {
								Snowball previousBall = findBall(p);
								if (previousBall.getVelocity().getX() < 0.01 && previousBall.getVelocity().getY() < 0.01
										&& previousBall.getVelocity().getZ() < 0.01) {
									Location loc = e.getClickedBlock().getLocation();
									Snowball ball = (Snowball) loc.getWorld().spawnEntity(loc.add(0.5, 1.3, 0.5),
											EntityType.SNOWBALL);
									ball.setShooter(previousBall.getShooter());
									ball.setInvulnerable(true);
									ball.setGravity(false);
									ball.setVelocity(new Vector(0, 0, 0));
									ball.setCustomName(previousBall.getCustomName());
									if (!Golf.mySql.getHide(p.getUniqueId().toString()))
										ball.setCustomNameVisible(true);

									InteractListener.balls.add(ball);
									InteractListener.ballPreviousLoc.remove(p.getName());

									EntitySnowball entitysnowball = ((CraftSnowball) ball).getHandle();
									net.minecraft.world.item.ItemStack stack = CraftItemStack
											.asNMSCopy(previousBall.getItem());
									entitysnowball.setItem(stack);

									InteractListener.balls.remove(previousBall);
									previousBall.remove();
									return;
								}
							}
						}
					}
				}
			}

			// Whistle
			if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
				if (item.getItemMeta().getDisplayName()
						.equals(Golf.config.getString("whistle.name").replace("&", "§"))) {
					GolfArea area = Golf.findGolfArea(p);
					if (area != null) {
						// Does the player have a ball placed already
						if (findBall(p) != null) {
							Snowball previousBall = findBall(p);
							Location loc = InteractListener.ballPreviousLoc.get(p.getName());
							if (getPins(p) != null) {
								if (fallenStands.containsKey(p)) {

									for (EntityArmorStand stand : fallenStands.get(p)) {
										stand.setBasePlate(false);
										stand.setHeadPose(new Vector3f(0, 0, 0));

										for (Player player : Bukkit.getOnlinePlayers()) {
											PacketPlayOutEntityMetadata entityMetaPacket = new PacketPlayOutEntityMetadata(
													stand.getId(), stand.getDataWatcher(), true);
											((CraftPlayer) player).getHandle().b.sendPacket(entityMetaPacket);
										}
									}
									fallenStands.remove(p);

								} else
									p.sendMessage(Golf.config.getString("no-whistle").replace("&", "§"));
								return;
							}
							if (loc != null) {

								Snowball ball = (Snowball) loc.getWorld().spawnEntity(loc, EntityType.SNOWBALL);
								ball.setShooter(previousBall.getShooter());
								ball.setInvulnerable(true);
								ball.setGravity(false);
								ball.setVelocity(new Vector(0, 0, 0));
								ball.setCustomName(previousBall.getCustomName());
								if (!Golf.mySql.getHide(p.getUniqueId().toString()))
									ball.setCustomNameVisible(true);

								balls.add(ball);
								ballPreviousLoc.remove(p.getName());

								EntitySnowball entitysnowball = ((CraftSnowball) ball).getHandle();
								net.minecraft.world.item.ItemStack stack = CraftItemStack
										.asNMSCopy(previousBall.getItem());
								entitysnowball.setItem(stack);

								balls.remove(previousBall);
								previousBall.remove();
								p.sendMessage(Golf.config.getString("ball-recalled").replace("&", "§"));

							} else
								p.sendMessage(Golf.config.getString("no-previous-loc").replace("&", "§"));
						} else
							p.sendMessage(Golf.config.getString("no-ball").replace("&", "§"));
					}

				}
			}

			if (e.getAction().name().contains("RIGHT")) {

				// Put ball
				if (e.getClickedBlock() != null) {
					// Now finding the golf area
					GolfArea area = Golf.findGolfArea(p);
					if (area != null) {
						// Is it a ball spawn block ?
						if (isBallSpawn(e.getClickedBlock(), area)) {
							e.setCancelled(true);
							// Is the slot empty
							if (isSlotEmpty(e.getClickedBlock().getLocation())) {
								// Does the player have a ball placed already
								if (findBall(p) == null) {

									// If bowling, check if there is space and spawn pins
									if (area.getAreaId().contains("bowling")) {
										String idBall = getBallSpawn(e.getClickedBlock(), area);
										idBall = idBall.replaceAll("[0-9]", "");

										if (!hasPins(idBall)) {
											Pins pins = new Pins(p, area.getCauldrons().get(idBall), idBall);
											pins.spawn();

										} else {
											p.sendMessage(Golf.config.getString("already-playing").replace("&", "§"));
											return;
										}

									}
									// Place the snowball

									Snowball ball = (Snowball) p.getWorld().spawnEntity(
											e.getClickedBlock().getLocation().add(0.5, 1.3, 0.5), EntityType.SNOWBALL);
									ball.setGravity(false);
									ball.setShooter(p);
									ball.setInvulnerable(true);
									ball.setCustomName("Par 0");
									if (!Golf.mySql.getHide(p.getUniqueId().toString()))
										ball.setCustomNameVisible(true);
									ball.setVelocity(new Vector(0, 0, 0));

									balls.add(ball);
									ballSpawnName.put(p.getName(), getBallSpawn(e.getClickedBlock(), area));

									Location ballInitialPos = ball.getLocation().clone();
									new BukkitRunnable() {
										public void run() {
											if (!ball.isDead() && ballInitialPos.distance(ball.getLocation()) < 1) {
												balls.remove(ball);
												p.getInventory().addItem(ball.getItem());
												ballPreviousLoc.remove(p.getName());
												ballSpawnName.remove(p.getName());
												ball.remove();
												if (InteractListener.getPins(p) != null) {
													for (Player player : Bukkit.getOnlinePlayers()) {
														for (EntityArmorStand standPacket : InteractListener.getPins(p)
																.getPackets()) {
															((CraftPlayer) player).getHandle().b
																	.sendPacket(new PacketPlayOutEntityDestroy(
																			standPacket.getId()));
														}
													}
													Pins.allPins.remove(InteractListener.getPins(p));
												}

											}

										}
									}.runTaskLater(Golf.pl, 20 * 180);

								} else
									p.sendMessage(Golf.config.getString("already-placed-ball").replace("&", "§"));

							} else
								p.sendMessage(Golf.config.getString("already-in-use").replace("&", "§"));

						}
					}

				}
			}

			// Hit ball
			if (e.getAction().name().contains("LEFT")) {
				if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
					GolfArea area = Golf.findGolfArea(p);
					if (area != null) {

						Snowball ball = findBall(p);
						if (ball != null) {

							String configSection = "clubs";
							if (area.getAreaId().contains("mini"))
								configSection = "clubs-mini";
							if (area.getAreaId().contains("bowling"))
								configSection = "clubs-bowling";
							for (String section : Golf.config.getConfigurationSection(configSection).getKeys(false)) {
								// Found the club
								if (Golf.config.getString(configSection + "." + section + ".name").replace("&", "§")
										.equals(item.getItemMeta().getDisplayName())) {

									Vector playerLookDir = p.getEyeLocation().getDirection();
									Location eyeLoc = p.getEyeLocation();
									World world = p.getWorld();
									if (eyeLoc.distance(ball.getLocation()) < 3.5) {
										for (Entity en : world.getNearbyEntities(
												eyeLoc.add(playerLookDir.multiply(ball.getLocation().distance(eyeLoc))),
												0.1, 0.1, 0.1)) {
											if (en instanceof Snowball && ((Snowball) en).getShooter().equals(p)) {
												if (en.getVelocity().getX() < 0.01 && en.getVelocity().getY() < 0.01
														&& en.getVelocity().getZ() < 0.01) {
													ConfigurationSection clubSection = Golf.config
															.getConfigurationSection(configSection + "." + section);
													e.setCancelled(true);
													if (!shooting.containsKey(p)) {
														ball.setVelocity(new Vector(0, 0, 0));
														startShoot(p, ball, clubSection);
													} else {
														double minForce = clubSection.getDouble("minforce");
														double maxForce = clubSection.getDouble("maxforce");
														double minHigh = clubSection.getDouble("minhigh");
														double maxHigh = clubSection.getDouble("maxhigh");
														double force = minForce + ((maxForce - minForce) * p.getExp());
														double high = minHigh + ((maxHigh - minHigh) * p.getExp());
														shoot(p, ball, force, high);

													}
												}
											}
										}
									}

								}
							}
						}
					}

				}

			}

		}

	}

	public boolean isSlotEmpty(Location loc) {
		for (Entity e : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
			if (e instanceof Snowball && balls.contains(e)) {
				return false;
			}
		}
		return true;
	}

	public static Snowball findBall(Player p) {
		for (Snowball ball : balls) {
			if (ball.getShooter().equals(p)) {
				return ball;
			}
		}
		return null;
	}

	HashMap<Player, BukkitTask> shooting = new HashMap<>();
	HashMap<Player, Float> saveExp = new HashMap<>();

	public void startShoot(Player p, Snowball ball, ConfigurationSection club) {

		saveExp.put(p, p.getExp());
		p.setExp(0);
		BukkitTask runnable = new BukkitRunnable() {

			int positive = 1;
			double increase = (double) 1 / club.getDouble("chargetime");

			public void run() {
				if (p.getExp() < 1 && p.getExp() + (increase * positive) >= 1) {
					positive = -1;
					p.setExp(1);
					return;

				} else if (p.getExp() > 0 && p.getExp() + (increase * positive) <= 0) {
					positive = 1;
					p.setExp(0);
					return;
				} else {
					p.setExp(p.getExp() + ((float) increase * positive));
				}

			}
		}.runTaskTimer(Golf.pl, 0, 1);

		shooting.put(p, runnable);
	}

	// HashMap<Player, ArrayList<EntityArmorStand>> bowling

	public void shoot(Player p, Snowball ball, double force, double high) {
		shooting.get(p).cancel();
		shooting.remove(p);
		p.setExp(saveExp.get(p));
		saveExp.remove(p);
		ballPreviousLoc.put(((Player) ball.getShooter()).getName(), ball.getLocation().clone());
		// Shoot the ball

		int i = Integer.parseInt(ball.getCustomName().replace("Par ", ""));
		ball.setCustomName("Par " + (i + 1));

		if (!Golf.mySql.getHide(p.getUniqueId().toString())) {

			ball.setCustomNameVisible(true);
		}

		ball.setGravity(true);
		Vector dir = p.getEyeLocation().getDirection();
		dir.setY(0).normalize();

		dir.multiply(force);
		dir.setY(high);

		ball.setVelocity(dir);

		fallenStands.remove(p);
		// Start loop for pins

		for (Pins pins : Pins.allPins) {
			if (pins.getPlayer().getUniqueId().equals(p.getUniqueId())) {
				fallenStands.remove(p);
				new BukkitRunnable() {

					@SuppressWarnings("unchecked")
					public void run() {
						Snowball bowlingBall = findBall(p);
						if (bowlingBall != null && !bowlingBall.isDead()) {

							// Hit pins ?
							for (EntityArmorStand pin : (ArrayList<EntityArmorStand>) pins.getPackets().clone()) {
								Location standLoc = new Location(p.getWorld(), pin.locX(), pin.locY() + 1.5, pin.locZ(),
										pin.getBukkitYaw(), 0f);

								if (standLoc.distance(bowlingBall.getLocation()) < Golf.config
										.getDouble("detectRange")) {
									if (!pin.hasBasePlate()) {
										Random r = new Random();
										double randomX = r.nextInt(60) - 30;
										double randomZ = r.nextInt(60) - 30;

										fall(pin,
												bowlingBall.getVelocity()
														.add(new Vector(randomX / 1000, 0, randomZ / 1000)),
												pins, p, this.getTaskId());

									}
								}
							}

						} else
							this.cancel();

					}
				}.runTaskTimer(Golf.pl, 0, 2);

			}
		}

	}

	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		if (shooting.containsKey(e.getPlayer()))
			if (e.getFrom().distance(e.getTo()) > 0)
				e.setCancelled(true);
	}

	@EventHandler
	public void onSneak(PlayerToggleSneakEvent e) {
		if (shooting.containsKey(e.getPlayer())) {
			shooting.get(e.getPlayer()).cancel();
			shooting.remove(e.getPlayer());
			e.getPlayer().setExp(saveExp.get(e.getPlayer()));
			saveExp.remove(e.getPlayer());
		}
	}

	@EventHandler
	public void onSlot(PlayerItemHeldEvent e) {
		if (shooting.containsKey(e.getPlayer()))
			e.setCancelled(true);
	}

	public static boolean isBallSpawn(Block block, GolfArea area) {
		for (Location ballSpawn : area.getBalls().values()) {
			if (ballSpawn.getBlock().getLocation().equals(block.getLocation())) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasPins(String name) {
		for (Pins pins : Pins.allPins) {
			if (pins.getName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public static Pins getPins(Player p) {
		for (Pins pins : Pins.allPins) {
			if (pins.getPlayer().equals(p)) {
				return pins;
			}
		}
		return null;
	}

	static HashMap<Player, ArrayList<EntityArmorStand>> fallenStands = new HashMap<>();

	public static void fall(EntityArmorStand armorStand, Vector velocity, Pins pins, Player p, int taskId) {

		armorStand.setBasePlate(true);

		new BukkitRunnable() {
			int i = 0;

			public void run() {
				armorStand.setHeadPose(new Vector3f(i, Utils.getLookAtYaw(velocity) + 90, 0));

				for (Player player : Bukkit.getOnlinePlayers()) {
					PacketPlayOutEntityMetadata entityMetaPacket = new PacketPlayOutEntityMetadata(armorStand.getId(),
							armorStand.getDataWatcher(), true);
					((CraftPlayer) player).getHandle().b.sendPacket(entityMetaPacket);
				}
				i += 15;
				if (i > 90)
					this.cancel();
			}
		}.runTaskTimer(Golf.pl, 0, 1);

		Pins newPins = pins;

		Pins.allPins.remove(pins);
		ArrayList<EntityArmorStand> newPackets = newPins.getPackets();
		newPackets.add(armorStand);
		newPins.setPackets(newPackets);
		Pins.allPins.add(newPins);

		// Win

		boolean standing = false;
		for (EntityArmorStand pinPacket : newPins.getPackets()) {
			if (!pinPacket.hasBasePlate())
				standing = true;
		}

		if (!standing) {
			ProjectileListener.win(p, findBall(p), pins.getLocation());
			for (Player player : Bukkit.getOnlinePlayers()) {
				for (EntityArmorStand standPacket : pins.getPackets()) {
					((CraftPlayer) player).getHandle().b
							.sendPacket(new PacketPlayOutEntityDestroy(standPacket.getId()));
				}
			}
			Pins.allPins.remove(InteractListener.getPins(p));
			Bukkit.getScheduler().cancelTask(taskId);
		}

		// Check collisions X ticks later
		new BukkitRunnable() {
			@SuppressWarnings("unchecked")
			public void run() {
				Location standLoc = new Location(p.getWorld(), armorStand.locX(), armorStand.locY() + 1.5,
						armorStand.locZ());
				standLoc.add(velocity.normalize().setY(0).multiply(0.5));

				for (EntityArmorStand standAround : (ArrayList<EntityArmorStand>) pins.getPackets().clone()) {
					if (!standAround.hasBasePlate()) {
						Location standAroundLoc = new Location(p.getWorld(), standAround.locX(),
								standAround.locY() + 1.5, standAround.locZ());
						if (standAroundLoc.distance(standLoc) < 0.3) {
							Random r = new Random();
							double randomX = r.nextInt(60) - 30;
							double randomZ = r.nextInt(60) - 30;

							fall(standAround, velocity.add(new Vector(randomX / 1000, 0.0, randomZ / 1000)), pins, p,
									taskId);
						}
					}
				}

			}
		}.runTaskLater(Golf.pl, Golf.config.getInt("cooldownCollide"));

		ArrayList<EntityArmorStand> stands = new ArrayList<>();
		if (fallenStands.containsKey(p))
			stands = fallenStands.get(p);
		stands.add(armorStand);
		fallenStands.put(p, stands);

	}

	public static String getBallSpawn(Block block, GolfArea area) {
		for (Entry<String, Location> ballSpawn : area.getBalls().entrySet()) {
			if (ballSpawn.getValue().getBlock().getLocation().equals(block.getLocation())) {
				return ballSpawn.getKey();
			}
		}
		return null;
	}

}
