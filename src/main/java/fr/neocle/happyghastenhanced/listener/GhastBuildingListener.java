package fr.neocle.happyghastenhanced.listener;

import fr.neocle.happyghastenhanced.HappyGhastEnhanced;
import fr.neocle.happyghastenhanced.util.Logger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class GhastBuildingListener implements Listener {

    private final HappyGhastEnhanced plugin;
    private final Logger logger;

    public GhastBuildingListener(HappyGhastEnhanced plugin) {
        this.plugin = plugin;
        this.logger = plugin.getCustomLogger();
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getConfigManager().isAllowBuildingOnGhast()) return;
        if (!player.isSneaking()) return;

        Entity entity = event.getRightClicked();
        if (!(entity instanceof HappyGhast)) return;

        GameMode gameMode = player.getGameMode();
        if (gameMode != GameMode.SURVIVAL && gameMode != GameMode.CREATIVE) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        Material material = item.getType();
        EquipmentSlot slot = EquipmentSlot.HAND;

        if (material.isAir() || !material.isBlock() || !material.isSolid()) {
            item = player.getInventory().getItemInOffHand();
            material = item.getType();
            if (material.isAir() || !material.isBlock() || !material.isSolid()) return;
            slot = EquipmentSlot.OFF_HAND;
        }

        Location clicked = entity.getLocation().add(event.getClickedPosition());

        BlockFace face = getDominantFace(event.getClickedPosition());
        Location target = clicked.getBlock().getRelative(face).getLocation();

        if (entity.getBoundingBox().overlaps(target.getBlock().getBoundingBox())) return;
        if (!target.getBlock().getType().isAir()) return;

        placeBlockWithEvent(player, target.getBlock(), face, item, slot);
        event.setCancelled(true);

        logger.debug("Player " + player.getName() + " placed block on HappyGhast at " +
                target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ());
    }

    private void placeBlockWithEvent(Player player, Block block, BlockFace face, ItemStack placedItem, EquipmentSlot slot) {
        BlockState replacedState = block.getState();
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(
                block,
                replacedState,
                block.getRelative(face),
                placedItem,
                player,
                true,
                slot
        );

        Bukkit.getPluginManager().callEvent(placeEvent);
        if (placeEvent.isCancelled()) return;

        if (player.getGameMode() != GameMode.CREATIVE) {
            placedItem.setAmount(placedItem.getAmount() - 1);
        }

        BlockData data = Bukkit.createBlockData(placedItem.getType());
        if (data instanceof Directional directional) {
            face = face.getOppositeFace();
            if (face != BlockFace.UP && face != BlockFace.DOWN) {
                directional.setFacing(face);
            }
        }
        block.setBlockData(data, true);
    }

    private Location adjustPlaceLocation(Location placeLocation, Entity entity) {
        Location entityLocation = entity.getLocation();
        double entityHeight = entity.getHeight();
        double entityWidth = entity.getWidth();

        double relativeX = placeLocation.getX() - entityLocation.getX();
        double relativeY = placeLocation.getY() - entityLocation.getY();
        double relativeZ = placeLocation.getZ() - entityLocation.getZ();

        double halfWidth = entityWidth / 2.0;
        double halfHeight = entityHeight / 2.0;

        Location adjustedLocation = placeLocation.clone();

        if (Math.abs(relativeX) > Math.abs(relativeZ)) {
            if (relativeX > 0) {
                adjustedLocation.setX(entityLocation.getX() + halfWidth + 1);
            } else {
                adjustedLocation.setX(entityLocation.getX() - halfWidth - 1);
            }
        } else {
            if (relativeZ > 0) {
                adjustedLocation.setZ(entityLocation.getZ() + halfWidth + 1);
            } else {
                adjustedLocation.setZ(entityLocation.getZ() - halfWidth - 1);
            }
        }

        if (relativeY > halfHeight) {
            adjustedLocation.setY(entityLocation.getY() + entityHeight + 1);
        } else if (relativeY < -halfHeight) {
            adjustedLocation.setY(entityLocation.getY() - 1);
        } else {
            if (Math.abs(relativeY) < 0.5) {
                adjustedLocation.setY(entityLocation.getY() + halfHeight);
            }
        }

        adjustedLocation.setX(Math.floor(adjustedLocation.getX()));
        adjustedLocation.setY(Math.floor(adjustedLocation.getY()));
        adjustedLocation.setZ(Math.floor(adjustedLocation.getZ()));

        return adjustedLocation;
    }

    private BlockFace getDominantFace(Vector direction) {
        BlockFace bestFace = BlockFace.NORTH;
        double bestDot = -Double.MAX_VALUE;

        Vector normalized = direction.clone().normalize();

        for (BlockFace face : BlockFace.values()) {
            if (!face.isCartesian()) continue;

            double dot = normalized.dot(face.getDirection());
            if (dot > bestDot) {
                bestDot = dot;
                bestFace = face;
            }
        }

        return bestFace;
    }
}