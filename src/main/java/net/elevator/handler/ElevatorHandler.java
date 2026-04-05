package net.elevator.handler;

import net.elevator.ElevatorMod;
import net.elevator.config.ElevatorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all elevator logic each server tick.
 *
 * ELEVATOR:
 *   - Jump while standing on an elevator block -> teleport up to the next elevator block
 *   - Sneak while standing on an elevator block -> teleport down to the previous elevator block
 *   - The elevator block must be placed ON TOP of a redstone block to function.
 *   - Optionally hide the elevator block under a carpet; it still works.
 */
public class ElevatorHandler {

    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();
    private final Map<UUID, Boolean> wasSneaking = new HashMap<>();
    private final Map<UUID, Double> prevY = new HashMap<>();
    private final Map<UUID, Long> cooldownExpiry = new HashMap<>();

    private Block cachedElevatorBlock = null;
    private String cachedElevatorBlockId = null;

    public void onServerTick(MinecraftServer server) {
        long currentTick = server.getTickCount();
        for (ServerLevel world : server.getAllLevels()) {
            for (ServerPlayer player : world.getPlayers(p -> true)) {
                if (player.isSpectator()) continue;
                handlePlayer(player, world, currentTick);
            }
        }
    }

    private void handlePlayer(ServerPlayer player, ServerLevel world, long currentTick) {
        UUID uuid = player.getUUID();
        ElevatorConfig config = ElevatorConfig.get();

        boolean onGround = player.onGround();
        boolean sneaking = player.isShiftKeyDown();
        boolean prevGround = wasOnGround.getOrDefault(uuid, true);
        boolean prevSneak = wasSneaking.getOrDefault(uuid, false);
        double lastY = prevY.getOrDefault(uuid, player.getY());

        BlockPos blockUnder = player.blockPosition().below();

        Block elevatorBlock = getElevatorBlock(config);
        if (world.getBlockState(blockUnder).getBlock() == elevatorBlock) {
            if (world.getBlockState(blockUnder.below()).getBlock() == Blocks.REDSTONE_BLOCK) {
                if (currentTick >= cooldownExpiry.getOrDefault(uuid, 0L)) {
                    boolean jumped = prevGround && !onGround && player.getY() > lastY + 0.05;
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

        wasOnGround.put(uuid, onGround);
        wasSneaking.put(uuid, sneaking);
        prevY.put(uuid, player.getY());
    }

    private BlockPos findElevatorAbove(BlockPos current, ServerLevel world, Block elevatorBlock, ElevatorConfig config) {
        for (int i = 1; i <= config.maxElevatorHeight; i++) {
            BlockPos check = current.above(i);
            if (world.getBlockState(check).getBlock() == elevatorBlock) {
                if (world.getBlockState(check.below()).getBlock() == Blocks.REDSTONE_BLOCK) {
                    if (!config.safetyEnabled || isSafe(check, world)) {
                        return check;
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findElevatorBelow(BlockPos current, ServerLevel world, Block elevatorBlock, ElevatorConfig config) {
        for (int i = 1; i <= config.maxElevatorHeight; i++) {
            BlockPos check = current.below(i);
            if (world.getBlockState(check).getBlock() == elevatorBlock) {
                if (world.getBlockState(check.below()).getBlock() == Blocks.REDSTONE_BLOCK) {
                    if (!config.safetyEnabled || isSafe(check, world)) {
                        return check;
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafe(BlockPos elevatorPos, ServerLevel world) {
        BlockPos above1 = elevatorPos.above(1);
        BlockPos above2 = elevatorPos.above(2);
        return world.isEmptyBlock(above1) && world.isEmptyBlock(above2);
    }

    public void doTeleport(ServerPlayer player, BlockPos dest,
                           ServerLevel destWorld, long currentTick, ElevatorConfig config) {
        double x = dest.getX() + 0.5;
        double y = dest.getY() + 1.0;
        double z = dest.getZ() + 0.5;

        player.teleportTo(destWorld, x, y, z,
            java.util.Set.of(), player.getYRot(), player.getXRot(), true);

        player.setDeltaMovement(0, 0, 0);

        cooldownExpiry.put(player.getUUID(), currentTick + config.cooldownTicks);
        wasOnGround.put(player.getUUID(), false);
        wasSneaking.put(player.getUUID(), player.isShiftKeyDown());
        prevY.put(player.getUUID(), y);

        if (config.particlesEnabled) {
            destWorld.sendParticles(
                ParticleTypes.PORTAL,
                x, y + 0.5, z,
                25, 0.3, 0.6, 0.3, 0.05
            );
        }

        if (config.soundEnabled) {
            destWorld.playSound(
                null, x, y, z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.6f, 1.0f
            );
        }
    }

    public void removePlayer(UUID uuid) {
        wasOnGround.remove(uuid);
        wasSneaking.remove(uuid);
        prevY.remove(uuid);
        cooldownExpiry.remove(uuid);
    }

    private Block getElevatorBlock(ElevatorConfig config) {
        if (!config.elevatorBlock.equals(cachedElevatorBlockId)) {
            try {
                cachedElevatorBlock = BuiltInRegistries.BLOCK.getValue(Identifier.parse(config.elevatorBlock));
            } catch (Exception e) {
                cachedElevatorBlock = Blocks.AIR;
            }
            cachedElevatorBlockId = config.elevatorBlock;
            if (cachedElevatorBlock == Blocks.AIR) {
                ElevatorMod.LOGGER.warn("{} Invalid elevator block in config: {}", ElevatorMod.logPrefix(), config.elevatorBlock);
            }
        }
        return cachedElevatorBlock;
    }
}
