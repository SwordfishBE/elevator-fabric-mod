package be.locutus.elevator.handler;

import be.locutus.elevator.config.ElevatorConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all elevator logic each server tick.
 *
 * ELEVATOR:
 *   - Jump while standing on an elevator block → teleport up to the next elevator block
 *   - Sneak while standing on an elevator block → teleport down to the previous elevator block
 *   - The elevator block must be placed ON TOP of a redstone block to function.
 *   - Optionally hide the elevator block under a carpet — it still works!
 */
public class ElevatorHandler {

    /** Was the player on the ground last tick? */
    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();

    /** Was the player sneaking last tick? */
    private final Map<UUID, Boolean> wasSneaking = new HashMap<>();

    /** Player Y position last tick (for jump detection). */
    private final Map<UUID, Double> prevY = new HashMap<>();

    /** Tick at which the cooldown expires per player. */
    private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

    /** Cached elevator block reference (avoids registry lookup every tick). */
    private Block cachedElevatorBlock = null;
    private String cachedElevatorBlockId = null;

    // -------------------------------------------------------------------------
    // Main loop
    // -------------------------------------------------------------------------

    public void onServerTick(MinecraftServer server) {
        long currentTick = server.getTicks();
        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isSpectator()) continue;
                handlePlayer(player, world, currentTick);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Per-player handling
    // -------------------------------------------------------------------------

    private void handlePlayer(ServerPlayerEntity player, ServerWorld world, long currentTick) {
        UUID uuid = player.getUuid();
        ElevatorConfig config = ElevatorConfig.get();

        boolean onGround   = player.isOnGround();
        boolean sneaking   = player.isSneaking();
        boolean prevGround = wasOnGround.getOrDefault(uuid, true);
        boolean prevSneak  = wasSneaking.getOrDefault(uuid, false);
        double  lastY      = prevY.getOrDefault(uuid, player.getY());

        BlockPos blockUnder = player.getBlockPos().down();

        Block elevatorBlock = getElevatorBlock(config);
        if (world.getBlockState(blockUnder).getBlock() == elevatorBlock) {
            // Check that the elevator block is placed on a redstone block
            if (world.getBlockState(blockUnder.down()).getBlock() == Blocks.REDSTONE_BLOCK) {
                if (currentTick >= cooldownExpiry.getOrDefault(uuid, 0L)) {

                    // Jump detection: was on ground, now in air, Y has increased
                    boolean jumped = prevGround && !onGround && player.getY() > lastY + 0.05;

                    // Sneak detection: just started sneaking while on the ground
                    boolean startedSneaking = !prevSneak && sneaking && onGround;

                    if (jumped) {
                        BlockPos dest = findElevatorAbove(blockUnder, world, elevatorBlock, config);
                        if (dest != null) {
                            doTeleport(player, dest, world, currentTick, config);
                        }
                    } else if (startedSneaking) {
                        BlockPos dest = findElevatorBelow(blockUnder, world, elevatorBlock, config);
                        if (dest != null) {
                            doTeleport(player, dest, world, currentTick, config);
                        }
                    }
                }
            }
        }

        // Update state for next tick
        wasOnGround.put(uuid, onGround);
        wasSneaking.put(uuid, sneaking);
        prevY.put(uuid, player.getY());
    }

    // -------------------------------------------------------------------------
    // Elevator destination search
    // -------------------------------------------------------------------------

    private BlockPos findElevatorAbove(BlockPos current, ServerWorld world, Block elevatorBlock, ElevatorConfig config) {
        for (int i = 1; i <= config.maxElevatorHeight; i++) {
            BlockPos check = current.up(i);
            if (world.getBlockState(check).getBlock() == elevatorBlock) {
                if (world.getBlockState(check.down()).getBlock() == Blocks.REDSTONE_BLOCK) {
                    if (!config.safetyEnabled || isSafe(check, world)) {
                        return check;
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findElevatorBelow(BlockPos current, ServerWorld world, Block elevatorBlock, ElevatorConfig config) {
        for (int i = 1; i <= config.maxElevatorHeight; i++) {
            BlockPos check = current.down(i);
            if (world.getBlockState(check).getBlock() == elevatorBlock) {
                if (world.getBlockState(check.down()).getBlock() == Blocks.REDSTONE_BLOCK) {
                    if (!config.safetyEnabled || isSafe(check, world)) {
                        return check;
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafe(BlockPos elevatorPos, ServerWorld world) {
        BlockPos above1 = elevatorPos.up(1);
        BlockPos above2 = elevatorPos.up(2);
        return !world.getBlockState(above1).isSolidBlock(world, above1)
            && !world.getBlockState(above2).isSolidBlock(world, above2);
    }

    // -------------------------------------------------------------------------
    // Teleport
    // -------------------------------------------------------------------------

    public void doTeleport(ServerPlayerEntity player, BlockPos dest,
                            ServerWorld destWorld, long currentTick, ElevatorConfig config) {
        double x = dest.getX() + 0.5;
        double y = dest.getY() + 1.0;
        double z = dest.getZ() + 0.5;

        player.teleport(destWorld, x, y, z,
            java.util.Set.of(), player.getYaw(), player.getPitch(), true);

        player.setVelocity(0, 0, 0);

        cooldownExpiry.put(player.getUuid(), currentTick + config.cooldownTicks);
        wasOnGround.put(player.getUuid(), false);
        wasSneaking.put(player.getUuid(), player.isSneaking());
        prevY.put(player.getUuid(), y);

        if (config.particlesEnabled) {
            destWorld.spawnParticles(
                ParticleTypes.PORTAL,
                x, y + 0.5, z,
                25, 0.3, 0.6, 0.3, 0.05
            );
        }

        if (config.soundEnabled) {
            destWorld.playSound(
                null, x, y, z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                0.6f, 1.0f
            );
        }
    }

    // -------------------------------------------------------------------------
    // Cleanup on disconnect
    // -------------------------------------------------------------------------

    public void removePlayer(UUID uuid) {
        wasOnGround.remove(uuid);
        wasSneaking.remove(uuid);
        prevY.remove(uuid);
        cooldownExpiry.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Cached block lookup
    // -------------------------------------------------------------------------

    private Block getElevatorBlock(ElevatorConfig config) {
        if (!config.elevatorBlock.equals(cachedElevatorBlockId)) {
            cachedElevatorBlock = Registries.BLOCK.get(Identifier.of(config.elevatorBlock));
            cachedElevatorBlockId = config.elevatorBlock;
            if (cachedElevatorBlock == Blocks.AIR) {
                System.err.println("[Elevator] Invalid elevator block in config: " + config.elevatorBlock);
            }
        }
        return cachedElevatorBlock;
    }
}
