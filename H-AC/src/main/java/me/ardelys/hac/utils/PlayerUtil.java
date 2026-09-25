package me.ardelys.hac.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;
import java.util.Set;

public final class PlayerUtil {

    private static final Set<Material> ICE_BLOCKS = EnumSet.of(
            Material.ICE,
            Material.PACKED_ICE,
            Material.BLUE_ICE,
            Material.FROSTED_ICE
    );

    private static final Set<Material> CLIMBABLE_BLOCKS = EnumSet.of(
            Material.LADDER,
            Material.VINE,
            Material.SCAFFOLDING,
            Material.WEEPING_VINES,
            Material.WEEPING_VINES_PLANT,
            Material.TWISTING_VINES,
            Material.TWISTING_VINES_PLANT
    );

    private static final Set<Material> LIQUID_BLOCKS = EnumSet.of(
            Material.WATER,
            Material.LAVA,
            Material.BUBBLE_COLUMN
    );

    private static final Set<Material> SOLID_SURFACE_ON_LIQUID = EnumSet.of(
            Material.LILY_PAD,
            Material.WHITE_CARPET,
            Material.ORANGE_CARPET,
            Material.MAGENTA_CARPET,
            Material.LIGHT_BLUE_CARPET,
            Material.YELLOW_CARPET,
            Material.LIME_CARPET,
            Material.PINK_CARPET,
            Material.GRAY_CARPET,
            Material.LIGHT_GRAY_CARPET,
            Material.CYAN_CARPET,
            Material.PURPLE_CARPET,
            Material.BLUE_CARPET,
            Material.BROWN_CARPET,
            Material.GREEN_CARPET,
            Material.RED_CARPET,
            Material.BLACK_CARPET,
            Material.MOSS_CARPET
    );

    private static final Set<Material> PISTON_BLOCKS = EnumSet.of(
            Material.PISTON,
            Material.STICKY_PISTON,
            Material.PISTON_HEAD,
            Material.MOVING_PISTON
    );

    private PlayerUtil() {
    }

    public record EnvironmentScan(
            boolean onIce,
            boolean onSlime,
            boolean onHoney,
            boolean onClimbable,
            boolean inWeb,
            boolean inLiquid,
            boolean onLiquid,
            boolean hasSolidGround,
            boolean onSoul,
            boolean onPiston
    ) {}

    public static EnvironmentScan scanEnvironment(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return new EnvironmentScan(false, false, false, false, false, false, false, false, false, false);
        }

        int centerChunkX = loc.getBlockX() >> 4;
        int centerChunkZ = loc.getBlockZ() >> 4;
        if (!loc.getWorld().isChunkLoaded(centerChunkX, centerChunkZ)) {
            return new EnvironmentScan(false, false, false, false, false, false, false, true, false, false);
        }

        int minX = (int) Math.floor(loc.getX() - 0.3);
        int maxX = (int) Math.floor(loc.getX() + 0.3);
        int minY = (int) Math.floor(loc.getY() - 0.5);
        int maxY = (int) Math.floor(loc.getY() + 0.8);
        int minZ = (int) Math.floor(loc.getZ() - 0.3);
        int maxZ = (int) Math.floor(loc.getZ() + 0.3);

        boolean sameChunk = ((minX >> 4) == centerChunkX)
                && ((maxX >> 4) == centerChunkX)
                && ((minZ >> 4) == centerChunkZ)
                && ((maxZ >> 4) == centerChunkZ);

        boolean onIce = false;
        boolean onSlime = false;
        boolean onHoney = false;
        boolean onClimbable = false;
        boolean inWeb = false;
        boolean inLiquid = false;
        boolean onLiquid = false;
        boolean carpetOrPad = false;
        boolean hasSolidGround = false;
        boolean onSoul = false;
        boolean onPiston = false;

        double locY = loc.getY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!sameChunk) {
                    int cx = x >> 4;
                    int cz = z >> 4;
                    if (!loc.getWorld().isChunkLoaded(cx, cz)) {
                        continue;
                    }
                }
                for (int y = minY; y <= maxY; y++) {
                    Block block = loc.getWorld().getBlockAt(x, y, z);
                    Material mat = block.getType();

                    if (mat.isAir()) {
                        continue;
                    }

                    if (ICE_BLOCKS.contains(mat) && y <= Math.floor(locY + 0.1)) {
                        onIce = true;
                    }
                    if (mat == Material.SLIME_BLOCK && y <= Math.floor(locY + 0.1)) {
                        onSlime = true;
                    }
                    if (mat == Material.HONEY_BLOCK && y <= Math.floor(locY + 0.1)) {
                        onHoney = true;
                    }
                    if (CLIMBABLE_BLOCKS.contains(mat)) {
                        onClimbable = true;
                    }
                    if (mat == Material.COBWEB) {
                        inWeb = true;
                    }
                    if (LIQUID_BLOCKS.contains(mat)) {
                        if (y >= Math.floor(locY)) {
                            inLiquid = true;
                        }
                        if (y <= Math.floor(locY + 0.1)) {
                            onLiquid = true;
                        }
                    }
                    if (SOLID_SURFACE_ON_LIQUID.contains(mat) && y <= Math.floor(locY + 0.3)) {
                        carpetOrPad = true;
                    }
                    if (mat.isSolid() && y <= Math.floor(locY + 0.05)) {
                        hasSolidGround = true;
                    }
                    if ((mat == Material.SOUL_SAND || mat == Material.SOUL_SOIL) && y <= Math.floor(locY + 0.1)) {
                        onSoul = true;
                    }
                    if (PISTON_BLOCKS.contains(mat)) {
                        onPiston = true;
                    }
                }
            }
        }

        if (carpetOrPad) {
            onLiquid = false;
            hasSolidGround = true;
        }

        return new EnvironmentScan(onIce, onSlime, onHoney, onClimbable, inWeb, inLiquid, onLiquid, hasSolidGround, onSoul, onPiston);
    }

    public static int getPing(Player player) {
        if (player == null) return 0;
        try {
            return Math.max(0, player.getPing());
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getPotionLevel(Player player, PotionEffectType type) {
        if (player == null || type == null) return 0;
        PotionEffect effect = player.getPotionEffect(type);
        return effect != null ? effect.getAmplifier() + 1 : 0;
    }

    public static boolean hasFrostWalker(Player player) {
        if (player == null) return false;
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || boots.getType().isAir()) return false;
        return boots.getEnchantmentLevel(Enchantment.FROST_WALKER) > 0;
    }

    public static boolean hasSoulSpeed(Player player) {
        if (player == null) return false;
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || boots.getType().isAir()) return false;
        return boots.getEnchantmentLevel(Enchantment.SOUL_SPEED) > 0;
    }

    public static double getStepHeight(Player player) {
        if (player == null) return 0.6;
        try {
            AttributeInstance inst = player.getAttribute(Attribute.STEP_HEIGHT);
            if (inst != null) {
                return inst.getValue();
            }
        } catch (Throwable ignored) {
        }
        return 0.6;
    }

    public static double getMovementSpeedAttribute(Player player) {
        if (player == null) return 0.1;
        try {
            AttributeInstance inst = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (inst != null) {
                return inst.getValue();
            }
        } catch (Throwable ignored) {
        }
        return 0.1;
    }

    public static double getKnockbackResistance(Player player) {
        if (player == null) return 0.0;
        try {
            AttributeInstance inst = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (inst != null) {
                return inst.getValue();
            }
        } catch (Throwable ignored) {
        }
        return 0.0;
    }

    public static boolean isOnIce(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return checkSurrounding(loc, ICE_BLOCKS, 0.3, 0.5);
    }

    public static boolean isOnSlime(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return checkSurrounding(loc, EnumSet.of(Material.SLIME_BLOCK), 0.3, 0.5);
    }

    public static boolean isOnHoney(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return checkSurrounding(loc, EnumSet.of(Material.HONEY_BLOCK), 0.3, 0.5);
    }

    public static boolean isOnClimbable(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return checkSurrounding(loc, CLIMBABLE_BLOCKS, 0.3, 0.8);
    }

    public static boolean isInWeb(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return checkSurrounding(loc, EnumSet.of(Material.COBWEB), 0.3, 0.8);
    }

    public static boolean isInLiquid(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return checkSurrounding(loc, LIQUID_BLOCKS, 0.3, 0.8);
    }

    public static boolean isOnLiquid(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (checkSurrounding(loc, SOLID_SURFACE_ON_LIQUID, 0.3, 0.3)) {
            return false;
        }
        return checkSurrounding(new Location(loc.getWorld(), loc.getX(), loc.getY() - 0.1, loc.getZ()), LIQUID_BLOCKS, 0.3, 0.2);
    }

    public static boolean isMovementSlowingItem(ItemStack item) {
        if (item == null) return false;
        Material mat = item.getType();
        if (mat.isEdible()) return true;
        return mat == Material.POTION
                || mat == Material.SPLASH_POTION
                || mat == Material.BOW
                || mat == Material.CROSSBOW
                || mat == Material.SHIELD
                || mat == Material.GOAT_HORN
                || mat == Material.MILK_BUCKET
                || mat == Material.HONEY_BOTTLE;
    }

    public static boolean checkSurrounding(Location loc, Set<Material> materials, double expandXZ, double expandY) {
        if (loc == null || loc.getWorld() == null || materials == null || materials.isEmpty()) return false;
        int centerChunkX = loc.getBlockX() >> 4;
        int centerChunkZ = loc.getBlockZ() >> 4;
        if (!loc.getWorld().isChunkLoaded(centerChunkX, centerChunkZ)) return false;

        int minX = (int) Math.floor(loc.getX() - expandXZ);
        int maxX = (int) Math.floor(loc.getX() + expandXZ);
        int minY = (int) Math.floor(loc.getY() - expandY);
        int maxY = (int) Math.floor(loc.getY() + expandY);
        int minZ = (int) Math.floor(loc.getZ() - expandXZ);
        int maxZ = (int) Math.floor(loc.getZ() + expandXZ);

        boolean sameChunk = ((minX >> 4) == centerChunkX)
                && ((maxX >> 4) == centerChunkX)
                && ((minZ >> 4) == centerChunkZ)
                && ((maxZ >> 4) == centerChunkZ);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!sameChunk) {
                    int cx = x >> 4;
                    int cz = z >> 4;
                    if (!loc.getWorld().isChunkLoaded(cx, cz)) {
                        continue;
                    }
                }
                for (int y = minY; y <= maxY; y++) {
                    Block block = loc.getWorld().getBlockAt(x, y, z);
                    if (materials.contains(block.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
