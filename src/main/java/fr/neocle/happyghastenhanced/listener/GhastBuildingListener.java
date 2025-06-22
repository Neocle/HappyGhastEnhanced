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
        if (!(entity instanceof HappyGhast ghast)) return;

        GameMode mode = player.getGameMode();
        if (mode != GameMode.SURVIVAL && mode != GameMode.CREATIVE) return;

        ItemStack item = getPlaceableItem(player);
        if (item == null) return;

        EquipmentSlot slot = player.getInventory().getItemInMainHand().equals(item) ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;

        Location base = ghast.getLocation().add(event.getClickedPosition());
        BlockFace face = getDominantFace(event.getClickedPosition());
        Block target = base.getBlock().getRelative(face);

        if (ghast.getBoundingBox().overlaps(target.getBoundingBox())) return;
        if (!target.getType().isAir()) return;

        placeBlock(player, target, face, item, slot);
        event.setCancelled(true);

        logger.debug("Player " + player.getName() + " placed block on HappyGhast at " +
                target.getX() + ", " + target.getY() + ", " + target.getZ());
    }

    private ItemStack getPlaceableItem(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isValidBlockItem(main)) return main;

        ItemStack off = player.getInventory().getItemInOffHand();
        if (isValidBlockItem(off)) return off;

        return null;
    }

    private boolean isValidBlockItem(ItemStack item) {
        Material material = item.getType();
        return !material.isAir() && material.isBlock() && material.isSolid();
    }

    private void placeBlock(Player player, Block block, BlockFace face, ItemStack item, EquipmentSlot slot) {
        BlockState oldState = block.getState();
        Block relative = block.getRelative(face);
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(block, oldState, relative, item, player, true, slot);

        Bukkit.getPluginManager().callEvent(placeEvent);
        if (placeEvent.isCancelled()) return;

        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }

        BlockData data = Bukkit.createBlockData(item.getType());
        if (data instanceof Directional directional) {
            BlockFace opposite = face.getOppositeFace();
            if (opposite != BlockFace.UP && opposite != BlockFace.DOWN) {
                directional.setFacing(opposite);
            }
        }

        block.setBlockData(data, true);
    }

    private BlockFace getDominantFace(Vector direction) {
        Vector norm = direction.clone().normalize();
        BlockFace dominant = BlockFace.NORTH;
        double best = -Double.MAX_VALUE;

        for (BlockFace face : BlockFace.values()) {
            if (!face.isCartesian()) continue;

            double dot = norm.dot(face.getDirection());
            if (dot > best) {
                best = dot;
                dominant = face;
            }
        }

        return dominant;
    }
}
