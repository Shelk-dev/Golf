package com.shelk.listeners;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.command.CommandException;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftSnowball;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import com.shelk.Golf;
import com.shelk.GolfArea;
import com.shelk.sql.MySQLAccess;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.minecraft.world.entity.projectile.EntitySnowball;

public class ProjectileListener implements Listener {

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		Entity ent = event.getEntity();
		if (ent instanceof Snowball && InteractListener.balls.contains(ent)) {
			event.setCancelled(true);

			Location loc = ent.getLocation();
			Vector vel = ent.getVelocity();
			World world = ent.getWorld();

			Snowball previousBall = (Snowball) ent;

			final GolfArea area = Golf.findGolfArea((Player) previousBall.getShooter());

			if (event.getHitEntity() != null) {
				if (event.getHitEntity().getType().name().contains("MINECART")) {
					previousBall.teleport(
							InteractListener.ballPreviousLoc.get(((Player) previousBall.getShooter()).getName()));
					previousBall.setGravity(false);
					previousBall.setVelocity(new Vector(0, 0, 0));
				}
				return;
			}

			if (event.getHitBlock() != null && event.getHitBlockFace() == BlockFace.UP) {

				// WIN
				if (event.getHitBlock().getType() == Material.CAULDRON) {
					if (isWin(previousBall, area, event.getHitBlock().getLocation())) {
						win(((Player) previousBall.getShooter()), previousBall, event.getHitBlock().getLocation());
						return;
					}
				}
			}

			Location spawnLoc = loc;

			if (event.getHitBlock().getType() == Material.SOUL_SAND || loc.getBlock().getType() == Material.WATER) {
				spawnLoc = InteractListener.ballPreviousLoc.get(((Player) previousBall.getShooter()).getName()).clone();
				InteractListener.ballPreviousLoc.remove(((Player) previousBall.getShooter()).getName());
			}

			final Snowball ball = (Snowball) world.spawnEntity(spawnLoc, EntityType.SNOWBALL);
			ball.setShooter(previousBall.getShooter());
			ball.setInvulnerable(true);
			ball.setCustomName(previousBall.getCustomName());
			if (!Golf.mySql.getHide(((Player) previousBall.getShooter()).getUniqueId().toString()))
				ball.setCustomNameVisible(true);

			InteractListener.balls.add(ball);
			Player p = (Player) ball.getShooter();
			EntitySnowball entitysnowball = ((CraftSnowball) ball).getHandle();
			net.minecraft.world.item.ItemStack stack = CraftItemStack.asNMSCopy(previousBall.getItem());
			entitysnowball.setItem(stack);

			InteractListener.balls.remove(ent);
			ent.remove();

			final ProtectedRegion region = WorldGuard.getInstance().getPlatform().getRegionContainer()
					.get(new BukkitWorld(world)).getRegion(area.getAreaId());
			Material mat = event.getHitBlock().getType();
			new BukkitRunnable() {
				public void run() {
					if (ball != null && !ball.isDead()) {
						if (!region.contains(BlockVector3.at(ball.getLocation().getX(), ball.getLocation().getY(),
								ball.getLocation().getZ()))) {
							Snowball previousBall = ball;

							Location loc = InteractListener.ballPreviousLoc.get(p.getName());
							Snowball ball = (Snowball) loc.getWorld().spawnEntity(loc, EntityType.SNOWBALL);
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
							net.minecraft.world.item.ItemStack stack = CraftItemStack.asNMSCopy(previousBall.getItem());
							entitysnowball.setItem(stack);

							InteractListener.balls.remove(previousBall);
							previousBall.remove();
							this.cancel();
						} else
							this.cancel();
					} else
						this.cancel();
				}
			}.runTaskTimer(Golf.pl, 0, 10);

			if (event.getHitBlock().getType() == Material.SOUL_SAND || loc.getBlock().getType() == Material.WATER) {
				ball.setVelocity(new Vector(0, 0, 0));
				ball.setGravity(false);
				return;
			}

			if (event.getHitBlockFace() == BlockFace.UP) {

				if (mat == Material.MAGENTA_GLAZED_TERRACOTTA) {
					Directional bmeta = (Directional) event.getHitBlock().getBlockData();
					if (bmeta.getFacing() == BlockFace.WEST) {
						vel.setX(vel.getX() + 0.1 * Golf.config.getDouble("terracota-strength"));
					} else if (bmeta.getFacing() == BlockFace.EAST) {
						vel.setX(vel.getX() - 0.1 * Golf.config.getDouble("terracota-strength"));
					} else if (bmeta.getFacing() == BlockFace.NORTH) {
						vel.setZ(vel.getZ() + 0.1 * Golf.config.getDouble("terracota-strength"));
					} else if (bmeta.getFacing() == BlockFace.SOUTH) {
						vel.setZ(vel.getZ() - 0.1 * Golf.config.getDouble("terracota-strength"));
					}

				}

				else if (mat == Material.SAND) {
					vel.multiply(0.90);
				} else if (mat == Material.GRASS_BLOCK || mat == Material.DIRT) {
					vel.multiply(0.94);

				} else if (mat == Material.ICE || mat == Material.PACKED_ICE || mat == Material.BLUE_ICE) {
					vel.multiply(1);

				} else if (mat == Material.HONEY_BLOCK) {
					vel.setY(0);
				} else
					vel.multiply(0.8);

				if (mat == Material.SLIME_BLOCK)
					vel.setY(-vel.getY() * 1.3);
				else
					vel.setY(-vel.getY() * 0.7F);

				if (vel.getY() < 0.1) {

					vel.setY(0);
					ball.teleport(ball.getLocation().add(0, 0.1, 0));
					ball.setGravity(false);
					new BukkitRunnable() {
						public void run() {
							if (ball != null && !ball.isDead()) {

								if (ball.getLocation().add(0, -0.5, 0).getBlock().getType() == Material.CAULDRON) {
									if (isWin(ball, area,
											ball.getLocation().add(0, -0.5, 0).getBlock().getLocation())) {
										win(((Player) ball.getShooter()), ball,
												ball.getLocation().add(0, -0.5, 0).getBlock().getLocation());
										this.cancel();
									}

								}
								if (ball.getLocation().add(0, -0.5, 0).getBlock().getType() == Material.BIG_DRIPLEAF) {
									ball.setGravity(true);
								}

								if (ball.getLocation().add(0, -0.5, 0).getBlock().getType() == Material.WATER || ball
										.getLocation().add(0, -0.5, 0).getBlock().getType() == Material.SOUL_SAND) {
									ball.teleport(InteractListener.ballPreviousLoc
											.get(((Player) ball.getShooter()).getName()).clone());
									InteractListener.ballPreviousLoc.remove(((Player) ball.getShooter()).getName());
									ball.setVelocity(new Vector(0, 0, 0));
									ball.setGravity(false);
									return;

								}

								if (ball.getLocation().add(0, -0.5, 0).getBlock()
										.getType() == Material.MAGENTA_GLAZED_TERRACOTTA) {
									Directional bmeta = (Directional) ball.getLocation().add(0, -0.5, 0).getBlock()
											.getBlockData();
									Vector vel = ball.getVelocity();
									if (bmeta.getFacing() == BlockFace.WEST) {
										vel.setX(vel.getX() + 0.1 * Golf.config.getDouble("terracota-strength"));
									} else if (bmeta.getFacing() == BlockFace.EAST) {
										vel.setX(vel.getX() - 0.1 * Golf.config.getDouble("terracota-strength"));
									} else if (bmeta.getFacing() == BlockFace.NORTH) {
										vel.setZ(vel.getZ() + 0.1 * Golf.config.getDouble("terracota-strength"));
									} else if (bmeta.getFacing() == BlockFace.SOUTH) {
										vel.setZ(vel.getZ() - 0.1 * Golf.config.getDouble("terracota-strength"));
									}
									ball.setVelocity(vel);

								}
								if (ball.getLocation().add(0, -0.5, 0).getBlock().getType() == Material.ICE
										|| ball.getLocation().add(0, -0.5, 0).getBlock()
												.getType() == Material.PACKED_ICE
										|| ball.getLocation().add(0, -0.5, 0).getBlock()
												.getType() == Material.BLUE_ICE) {
									ball.setVelocity(ball.getVelocity().clone().multiply(1));
								}
								if (ball.getLocation().add(0, -0.5, 0).getBlock().getType() == Material.SAND) {
									ball.setVelocity(ball.getVelocity().clone().multiply(0.90));
								}
								if (ball.getLocation().add(0, -0.5, 0).getBlock().getType() == Material.GRASS_BLOCK
										|| ball.getLocation().add(0, -0.5, 0).getBlock().getType() == Material.DIRT) {
									ball.setVelocity(ball.getVelocity().clone().multiply(0.94));
								}
								if (ball.getLocation().add(0, -0.5, 0).getBlock().getType() == Material.AIR) {
									ball.setGravity(true);
									this.cancel();
								} else
									ball.setVelocity(ball.getVelocity().clone().multiply(0.97));

								if (Math.abs(ball.getVelocity().getX()) < 0.01 && Math.abs(ball.getVelocity().getY()) < 0.01
										&& Math.abs(ball.getVelocity().getZ()) < 0.01) {
									if (InteractListener.getPins(p) != null) {
			
										// Ball is too slow, reset for next shoot
										Location loc = InteractListener.ballPreviousLoc.get(p.getName());
										Snowball newBall = (Snowball) loc.getWorld().spawnEntity(loc,
												EntityType.SNOWBALL);
										newBall.setShooter(p);
										newBall.setInvulnerable(true);
										newBall.setGravity(false);
										newBall.setVelocity(new Vector(0, 0, 0));
										newBall.setCustomName(ball.getCustomName());
										if (!Golf.mySql.getHide(p.getUniqueId().toString()))
											newBall.setCustomNameVisible(true);

										InteractListener.balls.add(newBall);
										InteractListener.ballPreviousLoc.remove(p.getName());

										EntitySnowball entitysnowball = ((CraftSnowball) newBall).getHandle();
										net.minecraft.world.item.ItemStack stack = CraftItemStack
												.asNMSCopy(ball.getItem());
										entitysnowball.setItem(stack);

										InteractListener.balls.remove(ball);
										ball.remove();

									}
								}

							} else
								this.cancel();
						}
					}.runTaskTimer(Golf.pl, 0, 1);

				}

			}

			switch (event.getHitBlockFace()) {
			
			case NORTH:
			case SOUTH:
				if (mat == Material.SLIME_BLOCK) {
					vel.setZ(Math.copySign(0.25D, -vel.getZ()));
					break;
				}
				if (mat == Material.HONEY_BLOCK) {
					ball.setVelocity(new Vector(0, 0, 0));
					ball.setGravity(false);

					vel.setZ(0);
					vel.setY(-Math.abs(vel.getY()));
					new BukkitRunnable() {

						public void run() {
							boolean honey = false;
							for (Block b : getNearbyBlocks(ball.getLocation(), 1)) {
								if (b.getLocation().distance(ball.getLocation()) < 1.35
										&& b.getType() == Material.HONEY_BLOCK) {
									honey = true;
								}
							}
							if (!honey) {
								ball.setGravity(true);
								this.cancel();
							}
						}
					}.runTaskTimer(Golf.pl, 0, 1);
					break;

				}
				vel.setZ(-vel.getZ());
				break;
			case EAST:
			case WEST:
				if (mat == Material.SLIME_BLOCK) {
					vel.setX(Math.copySign(0.25D, -vel.getX()));
					break;
				}
				if (mat == Material.HONEY_BLOCK) {
					ball.setVelocity(new Vector(0, 0, 0));
					ball.setGravity(false);

					vel.setX(0);
					vel.setY(-Math.abs(vel.getY()));
					new BukkitRunnable() {

						public void run() {
							boolean honey = false;
							for (Block b : getNearbyBlocks(ball.getLocation(), 1)) {
								if (b.getLocation().distance(ball.getLocation()) < 1.35
										&& b.getType() == Material.HONEY_BLOCK) {
									honey = true;
								}
							}
							if (!honey) {
								ball.setGravity(true);
								this.cancel();
							}
						}
					}.runTaskTimer(Golf.pl, 0, 1);

					break;
				}
				vel.setX(-vel.getX());
				break;
			case DOWN:
				if (mat == Material.SLIME_BLOCK) {
					vel.setY(Math.copySign(0.25D, -vel.getY()));
					break;
				}
				if (mat == Material.HONEY_BLOCK) {
					ball.setVelocity(new Vector(0, 0, 0));
					ball.setGravity(false);

					vel.setY(0);
					new BukkitRunnable() {

						public void run() {
							boolean honey = false;
							for (Block b : getNearbyBlocks(ball.getLocation(), 1)) {
								if (b.getLocation().distance(ball.getLocation()) < 1.35
										&& b.getType() == Material.HONEY_BLOCK) {
									honey = true;
								}
							}
							if (!honey) {
								ball.setGravity(true);
								this.cancel();
							}
						}
					}.runTaskTimer(Golf.pl, 0, 1);
					break;
					
				}
				vel.setY(-vel.getY());
				break;
			default:
				break;

			}
			ball.setVelocity(vel);

		}

	}

	public boolean isWin(Snowball ball, GolfArea area, Location cauldron) {
		String cauldronName = null;
		for (Entry<String, Location> cauldronPos : area.getCauldrons().entrySet()) {
			if (cauldronPos.getValue().distance(cauldron) < 1)
				cauldronName = cauldronPos.getKey();
		}
		return InteractListener.ballSpawnName.get(((Player) ball.getShooter()).getName())
				.equalsIgnoreCase(cauldronName);

	}

	@SuppressWarnings("deprecation")
	public static void win(Player p, Snowball ball, Location cauldron) {

		InteractListener.balls.remove(ball);
		InteractListener.ballPreviousLoc.remove(p.getName());

		Random r = new Random();

		for (int i = 0; i < 3; i++) {
			FireworkEffect fx = FireworkEffect.builder()
					.withColor(Color.fromRGB(r.nextInt(255), r.nextInt(255), r.nextInt(255)))
					.with(FireworkEffect.Type.BALL_LARGE).withFlicker().build();
			Firework fw = (Firework) p.getWorld().spawnEntity(ball.getLocation().add(0, 2, 0), EntityType.FIREWORK);
			FireworkMeta fwm = fw.getFireworkMeta();

			fwm.setPower(1);
			fwm.addEffect(fx);

			fw.setFireworkMeta(fwm);
		}

		int attempts = Integer.parseInt(ball.getCustomName().replace("Par ", ""));
		ball.remove();

		GolfArea area = Golf.findGolfArea(p);
		String cauldronName = null;
		for (Entry<String, Location> cauldronPos : area.getCauldrons().entrySet()) {
			if (cauldronPos.getValue().distance(cauldron) < 1)
				cauldronName = cauldronPos.getKey();
		}
		p.sendMessage(((Golf.config.getString("win-message").replace("&", "§")).replace("%shots%", attempts + ""))
				.replace("%hole%", cauldronName));

		if (Parties.getApi().getPartyPlayer(p.getUniqueId()).getPartyId() != null) {
			Party party = Parties.getApi().getParty(Parties.getApi().getPartyPlayer(p.getUniqueId()).getPartyId());
			if (party != null && party.getOnlineMembers().size() > 0) {
				for (PartyPlayer player : party.getOnlineMembers()) {
					if (!player.getPlayerUUID().equals(p.getUniqueId())) {
						if (Bukkit.getPlayer(player.getPlayerUUID()) != null) {
							Bukkit.getPlayer(player.getPlayerUUID()).sendMessage(
									(((Golf.config.getString("win-message-party-members").replace("&", "§"))
											.replace("%player%", p.getName())).replace("%hole%", cauldronName))
													.replace("%shots%", attempts + ""));
						}
					}
				}
			}

		}
		int previousScore = Golf.mySql.getScore(p, cauldronName);
		if (previousScore == 0) {
			p.sendTitle(Golf.config.getString("new-personal-best-title").replace("&", "§"), null);

			if (Golf.mySql.getScores(p.getUniqueId().toString()) != null
					&& Golf.mySql.getScores(p.getUniqueId().toString()).equals("novalue")) {
				Golf.mySql.addScores(p, cauldronName + "=" + attempts);
			} else {
				Golf.mySql.updateScore(p, cauldronName + "=" + attempts);
			}

		} else if (previousScore != 0 && attempts < previousScore) {
			p.sendTitle(Golf.config.getString("new-personal-best-title").replace("&", "§"), null);
			Golf.mySql.updateScore(p, cauldronName + "=" + attempts);

		}

		// Update date, rewards
		if (Golf.mySql.getDate(p, cauldronName) != null) {
			try {
				if (ChronoUnit.DAYS.between(
						MySQLAccess.DATE_FORMAT.parse(Golf.mySql.getDate(p, cauldronName)).toInstant(),
						Instant.now()) >= 1) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
							Golf.config.getString("reward-24h").replace("%player%", p.getName()));
					Golf.mySql.updateDate(p, cauldronName + "=" + MySQLAccess.DATE_FORMAT.format(new Date()));
				}
			} catch (CommandException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		} else {
			Golf.mySql.updateDate(p, cauldronName + "=" + MySQLAccess.DATE_FORMAT.format(new Date()));
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					Golf.config.getString("reward-win").replace("%player%", p.getName()));
		}

	}

	public static List<Block> getNearbyBlocks(Location location, int radius) {
		List<Block> blocks = new ArrayList<Block>();
		for (int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
			for (int y = location.getBlockY() - radius; y <= location.getBlockY() + radius; y++) {
				for (int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
					blocks.add(location.getWorld().getBlockAt(x, y, z));
				}
			}
		}
		return blocks;
	}

}
