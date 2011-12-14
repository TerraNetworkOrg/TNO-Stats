package com.nidefawl.Stats;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.craftbukkit.entity.CraftItem;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nidefawl.Stats.datasource.PlayerStat;
import com.nidefawl.Stats.event.StatsMobDeathByPlayerEvent;
import com.nidefawl.Stats.event.StatsPlayerDamagedPlayerEvent;
import com.nidefawl.Stats.event.StatsPlayerDeathByEntityEvent;
import com.nidefawl.Stats.event.StatsPlayerDeathByOtherEvent;
import com.nidefawl.Stats.event.StatsPlayerDeathByPlayerEvent;

/**
 * Handles all events fired in relation to entities
 */
public class StatsEntityListener extends EntityListener {
	private Stats plugin;
	HashMap<Entity, ArrayList<Entity>> entsKilledByEnt = new HashMap<Entity, ArrayList<Entity>>();
	HashMap<Entity, String> otherDeathCauses = new HashMap<Entity, String>();

	private Entity getEntFromEnt(Entity entity, boolean setNull) {
		Entity res = null;
		ArrayList<Entity> removeThat = new ArrayList<Entity>();
		for (Entity ee : entsKilledByEnt.keySet()) { // cycle through Ents that
														// dealt damage
			
			if (ee == null)
				continue;

			ArrayList<Entity> ents = entsKilledByEnt.get(ee);
			if (ents.size() == 0) {
				removeThat.add(ee);
				continue;
			}
			for (Entity ent : ents) { // cycle through the ents that were
										// damaged by ee
				if (ent == null)
					continue;
				if (ent.equals(entity)) {
					res = ee; // return ee, he killed that ent
					break;
				}
			}
			if(res!=null) {
				if (setNull) {
					entsKilledByEnt.get(res).remove(entity);
					if (entsKilledByEnt.get(res).size() == 0)
						removeThat.add(res);
				}
				break;
			}
		}
		for (Entity e : removeThat) {
			entsKilledByEnt.remove(e);
		}
		return res;
	}

	public void UnloadPlayer(String player) {
		ArrayList<Entity> removeThat = new ArrayList<Entity>();
		Entity playerEnt = null;
		for (Entity ee : entsKilledByEnt.keySet()) { // cycle through Ents that
														// dealt damage
			if (ee == null)
				continue;

			ArrayList<Entity> ents = entsKilledByEnt.get(ee);
			if (ents.size() == 0) {
				removeThat.add(ee);
				continue;
			}
			if (ee instanceof Player) {
				if (((Player) ee).getName().equals(player)) {
					playerEnt = ee;
				}
			}
			Entity Remove = null;
			for (Entity ent : ents) { // cycle through the ents that were
										// damaged by ee
				if (ent == null)
					continue;
				if (ent instanceof Player) {
					if (((Player) ent).getName().equals(player)) {
						Remove = ent;
						break;
					}

				}
			}
			if (Remove != null)
				ents.remove(Remove);
		}
		for (Entity e : removeThat) {
			entsKilledByEnt.remove(e);
		}
		if (playerEnt != null)
			entsKilledByEnt.remove(playerEnt);
		otherDeathCauses.remove(player);
	}

	private void saveEntToEnt(Entity damager, Entity entity) {
		if (!entsKilledByEnt.keySet().contains(damager)) {
			ArrayList<Entity> ents = new ArrayList<Entity>();
			ents.add(entity);
			entsKilledByEnt.put(damager, ents);
		} else {
			ArrayList<Entity> ents = entsKilledByEnt.get(damager);
			for (Entity ent : ents) {
				if (ent.equals(entity))
					return;
			}
			ents.add(entity);
			entsKilledByEnt.put(damager, ents);
		}
	}
	public String getNiceDamageString(String cause) {
		if(cause.equals("fire")) 
			return "burned to death";
		if(cause.equals("fall")) 
			return "died by falling down a cliff";
		if(cause.equals("drowning")) 
			return "died by drowning";
		if(cause.equals("entityexplosion")||cause.equals("explosion")) 
			return "was hit by an explosion";
		if(cause.equals("lava")) 
			return "had a bath in lava";
		if(cause.equals("suffocation")) 
			return "suffocated";
		if(cause.equals("entityattack"))
			return "was hunt down by some creature";
		if(cause.equals("unknown"))
			return "was killed by Herobrine";
		return "was killed by "+cause.toLowerCase();
	}
	private void checkOtherDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			String cause = "unknown";
			switch (event.getCause()) {
			case FIRE_TICK:
			case FIRE:
				cause = "fire";
				break;
			case FALL:
				cause = "fall";
				break;
			case DROWNING:
				cause = "drowning";
				break;
			case BLOCK_EXPLOSION:
				cause = "explosion";
				break;
			case LAVA:
				cause = "lava";
				break;
			case SUFFOCATION:
				cause = "suffocation";
				break;
			case ENTITY_ATTACK:
				cause = "entityattack";
				break;
			case ENTITY_EXPLOSION:
				cause = "entityexplosion";
				break;
			case CONTACT:
				if (event instanceof EntityDamageByBlockEvent) {
					cause = ((EntityDamageByBlockEvent) event).getDamager().getType().toString();
					if (cause != null)
						break;
				}
			case CUSTOM:
			default:
				cause = "unknown";
			}
			plugin.updateStat((Player) event.getEntity(), "damagetaken", cause, event.getDamage(),false);
			plugin.updateStat((Player) event.getEntity(), "damagetaken", "total", event.getDamage(),false);
			if (event.getDamage() >= ((Player) event.getEntity()).getHealth() && plugin.getPlayerStat(((Player) event.getEntity()).getName())!=null) {
				otherDeathCauses.put((Player) event.getEntity(), cause);
			}

		}

	}
	/*public void sendEntList(CommandSender sender) {
		int LoadedEnts = 0;
		int nullEnts = 0;
		for (Entity e : entsKilledByEnt.keySet()) {
			if (e == null) { 
				nullEnts++;
				continue;
			}
			String entName = "(unknown)";
			if (e instanceof Player)
				entName = ((Player) e).getName();
			else
				entName = StatsEntityListener.EntToString(e);
			String entsList = "";
			for (Entity ee : entsKilledByEnt.get(e)) {
				if (ee instanceof Player)
					entsList += ((Player) ee).getName() + ", ";
				else
					entsList += StatsEntityListener.EntToString(ee) + " (" + ee.getEntityId() + ","+((LivingEntity)ee).getHealth()+"), ";

			}
			if (entsList.length() > 2)
				entsList.substring(0, entsList.length() - 2);
			sender.sendMessage("killed entitys for: " + entName + "(" + e.getEntityId() + "): " + entsList);
			LoadedEnts++;
			LoadedEnts += entsKilledByEnt.get(e).size();
		}
		sender.sendMessage(StatsSettings.premessage + ChatColor.WHITE + "Total chached ents: " + LoadedEnts + ", Nulls: " + nullEnts);

	}*/

	private boolean checkEntDamage(Entity entity, Entity damager, int amount) {
		if (!(damager instanceof Player) && !(entity instanceof Player)) {
			return true;
		}
		String typeName = null;
		if ((damager instanceof Player) && (entity instanceof Player)) {
			typeName = "Player";
			if(((LivingEntity) entity).getHealth()>0 ) {
				StatsPlayerDamagedPlayerEvent damageevent = new StatsPlayerDamagedPlayerEvent((Player)damager,(Player)entity,amount);
				plugin.getServer().getPluginManager().callEvent(damageevent);
				if(damageevent.isCancelled()) {
					return false;
				}
				plugin.updateStat((Player) damager, "damagedealt", typeName, amount,false);
				plugin.updateStat((Player) entity, "damagetaken", typeName, amount,false);
				if (amount >= ((LivingEntity) entity).getHealth() && plugin.getPlayerStat(((Player) damager).getName())!=null) {
					if (getEntFromEnt(entity, false) == null)
						saveEntToEnt(damager, entity);
				}
			}
			return true;
		}
		if (damager instanceof Player) {
			typeName = EntToString(entity);
			if (!(entity instanceof LivingEntity)) {
				plugin.updateStat((Player) damager, "damagedealt", typeName, amount,false);
				plugin.updateStat((Player) damager, "damagedealt", "total", amount,false);
				return true;
			} else {
				if (((LivingEntity) entity).getHealth() > 0) {
					plugin.updateStat((Player) damager, "damagedealt", typeName, amount,false);
					plugin.updateStat((Player) damager, "damagedealt", "total", amount,false);
					if (amount >= ((LivingEntity) entity).getHealth() && plugin.getPlayerStat(((Player) damager).getName())!=null) {
						if (getEntFromEnt(entity, false) == null)
							saveEntToEnt(damager, entity);
					}
				}
			}
		}
		if (entity instanceof Player) {
			typeName = EntToString(damager);
			if (((LivingEntity) entity).getHealth() > 0) {
				plugin.updateStat((Player) entity, "damagetaken", typeName, amount,false);
				plugin.updateStat((Player) entity, "damagetaken", "total", amount,false);
				if (amount >= ((Player) entity).getHealth() && plugin.getPlayerStat(((Player) entity).getName())!=null) {
					if (getEntFromEnt(entity, false) == null)
						saveEntToEnt(damager, entity);
				}
			}
		}
		return true;
	}

	public StatsEntityListener(Stats plugin) {
		this.plugin = plugin;
	}

    @SuppressWarnings("unused")
	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		Entity e = getEntFromEnt(event.getEntity(), true);
		if (event.getEntity() instanceof Player) {

			Player p = (Player) event.getEntity();
			PlayerStat ps = plugin.getPlayerStat(p.getName());
			if (ps == null)
				return;
			plugin.updateStat(p, "deaths", "total", 1,false);
			String otherReason = otherDeathCauses.get(p);
			otherReason = otherReason != null ? otherReason : "unknown";
			if (StatsSettings.deathNotifying) {
				String name = p.getDisplayName();
				String message = name + " &4died";
				if (e instanceof Player) {
					plugin.updateStat(((Player) e), "kills", "total", 1,false);
					plugin.updateStat(((Player) e), "kills", "player", 1,false);
					String name2 = ((Player) e).getName();
					message = name + " &4was killed by &8" + name2;
				} else if (e instanceof LivingEntity) {
					message = name + " &4was killed by &8" + EntToString(e);
				} else if (otherReason != null) {
					message = name + " &4"+getNiceDamageString(otherReason);
				} else {
					message = name + " &4died";
				}
				Messaging.worldbroadcast(event.getEntity().getWorld(), message);	
			}
			if (e instanceof LivingEntity) {
				plugin.updateStat(p, "deaths", EntToString(e), 1,false);
				if(e instanceof Player) {
					StatsPlayerDeathByPlayerEvent ev = new StatsPlayerDeathByPlayerEvent(event,(Player)event.getEntity(),(Player)e);
					plugin.getServer().getPluginManager().callEvent(ev);
				} else  {
					StatsPlayerDeathByEntityEvent ev = new StatsPlayerDeathByEntityEvent(event,(Player)event.getEntity(),e);
					plugin.getServer().getPluginManager().callEvent(ev);
				}
			} else if (otherReason != null) {
				plugin.updateStat(p, "deaths", otherReason, 1,false);
				StatsPlayerDeathByOtherEvent ev = new StatsPlayerDeathByOtherEvent(event,(Player)event.getEntity(),otherReason);
				plugin.getServer().getPluginManager().callEvent(ev);
			}
			otherDeathCauses.remove(p);

		} else if (event.getEntity() instanceof LivingEntity) {
			if (e instanceof Player) {
				plugin.updateStat((Player) e, "kills", "total", 1,false);
				plugin.updateStat((Player) e, "kills", EntToString(event.getEntity()), 1,false);
				StatsMobDeathByPlayerEvent ev = new StatsMobDeathByPlayerEvent(event, (Player)e,event.getEntity());
				plugin.getServer().getPluginManager().callEvent(ev);
			}
			entsKilledByEnt.remove(e);
		}
		entsKilledByEnt.remove(event.getEntity());
	}



    @Override
	public void onEntityCombust(EntityCombustEvent event) {

	}

    @Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled())
			return;
		if(event.getCause() == DamageCause.PROJECTILE) {
			EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
			if(!checkEntDamage(event.getEntity(), ((Projectile)e.getDamager()).getShooter(), event.getDamage())) {
				event.setCancelled(true);
			} 
		} else if(event instanceof EntityDamageByEntityEvent) {
			if(!checkEntDamage(((EntityDamageByEntityEvent)event).getEntity(), ((EntityDamageByEntityEvent)event).getDamager(), event.getDamage()))  {
				event.setCancelled(true);
			}
		} else if(event instanceof EntityDamageByBlockEvent) {
			checkOtherDamage((EntityDamageByBlockEvent)event);
		} else {
			checkOtherDamage(event);
		}
	}

    @Override
	public void onEntityExplode(EntityExplodeEvent event) {
	}

	static final public String EntToString(Entity ent) {
		if (ent == null)
			return "(null)";
		if (ent instanceof Chicken) {
			return "Chicken";
		} else if (ent instanceof Cow) {
			return "Cow";
		} else if (ent instanceof Wolf) {
			return "Wolf";
		} else if (ent instanceof CaveSpider) {
			return "CaveSpider";
		} else if (ent instanceof Spider) {
			return "Spider";
		} else if (ent instanceof Fish) {
			return "Fish";
		} else if (ent instanceof Pig) {
			return "Pig";
		} else if (ent instanceof Sheep) {
			return "Sheep";
		} else if (ent instanceof Arrow) {
			return "Arrow";
		} else if (ent instanceof Creeper) {
			return "Creeper";
		} else if (ent instanceof PigZombie) {
			return "PigZombie";
		} else if (ent instanceof Skeleton) {
			return "Skeleton";
		} else if (ent instanceof Egg) {
			return "Egg";
		} else if (ent instanceof Enderman) {
			return "Enderman";
		} else if (ent instanceof EnderDragon) {
			return "EnderDragon";
		} else if (ent instanceof Giant) {
			return "Giant";
		} else if (ent instanceof FallingSand) {
			return "FallingSand";
		} else if (ent instanceof Fireball) {
			return "Fireball";
		} else if (ent instanceof Ghast) {
			return "Ghast";
		} else if (ent instanceof Item) {
			return "Item";
		} else if (ent instanceof CraftItem) {
			return "ItemDrop";
		} else if (ent instanceof Painting) {
			return "Painting";
		} else if (ent instanceof Player) {
			return "Player";
		} else if (ent instanceof Snowball) {
			return "Snowball";
		} else if (ent instanceof Snowman) {
			return "Snowman";
		} else if (ent instanceof Zombie) {
			return "Zombie";
		} else if (ent instanceof Squid) {
			return "Squid";
		} else if (ent instanceof MushroomCow) {
			return "MushroomCow";
		} else if (ent instanceof Blaze) {
			return "Blaze";
		} else if (ent instanceof Enderman) {
			return "Enderman";
		} else if (ent instanceof Silverfish) {
			return "Silverfish";
		} else if (ent instanceof WaterMob) {
			return "WaterMob";
		} else if (ent instanceof Slime) {
			return "Slime";
		} else if (ent instanceof Animals) {
			return "Animals";
		} else if (ent instanceof AnimalTamer) {
			return "AnimalTamer";
		} else if (ent instanceof HumanEntity) {
			return "HumanEntity";
		} else if (ent instanceof Flying) {
			return "Flying";
		} else if (ent instanceof LivingEntity) {
			return "LivingEntity";
		} else if (ent instanceof TNTPrimed) {
			return "TNTPrimed";
		} else if (ent instanceof PoweredMinecart) {
			return "PoweredMinecart";
		} else if (ent instanceof StorageMinecart) {
			return "StorageMinecart";
		} else if (ent instanceof Minecart) {
			return "Minecart";
		} else if (ent instanceof Boat) {
			return "Boat";
		} else if (ent instanceof Vehicle) {
			return "Vehicle";
		} else if (ent instanceof Entity) {
			return "Entity";
		} else if (ent instanceof Explosive) {
			return "Explosive";
		} else if (ent instanceof LightningStrike) {
			return "LightningStrike";
		} else if (ent instanceof NPC) {
			return "NPC";
		} else if (ent instanceof Projectile) {
			return "Projectile";
		} else if (ent instanceof SmallFireball) {
			return "SmallFireball";
		} else if (ent instanceof Tameable) {
			return "Tameable";
		} else if (ent instanceof ThrownPotion) {
			return "ThrownPotion";
		} else if (ent instanceof Villager) {
			return "Villager";
		} else if (ent instanceof Weather) {
			return "Weather";
		} else if (ent instanceof Monster) {
			return "Monster";
		} else if (ent instanceof Creature) {
			return "Creature";
		} else {
		return "(unknown)";
		}
	}
}