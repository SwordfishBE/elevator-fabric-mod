package net.elevator.handler;

import net.elevator.ElevatorMod;
import net.elevator.config.ElevatorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    private Set<Block> cachedElevatorBlocks = Set.of();
    private String cachedElevatorBlocksKey = "";
    private SimpleParticleType cachedParticle = null;
    private String cachedParticleId = null;
    private SoundEvent cachedSound = null;
    private String cachedSoundId = null;

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

        Set<Block> elevatorBlocks = getElevatorBlocks(config);
        Block currentElevatorBlock = world.getBlockState(blockUnder).getBlock();
        if (elevatorBlocks.contains(currentElevatorBlock)) {
            if (world.getBlockState(blockUnder.below()).getBlock() == Blocks.REDSTONE_BLOCK) {
                if (currentTick >= cooldownExpiry.getOrDefault(uuid, 0L)) {
                    boolean jumped = prevGround && !onGround && player.getY() > lastY + 0.05;
                    boolean startedSneaking = !prevSneak && sneaking && onGround;

                    if (jumped) {
                        BlockPos dest = findElevatorAbove(blockUnder, world, currentElevatorBlock, config);
                        if (dest != null) {
                            doTeleport(player, dest, world, currentTick, config);
                        }
                    } else if (startedSneaking) {
                        BlockPos dest = findElevatorBelow(blockUnder, world, currentElevatorBlock, config);
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
            SimpleParticleType particleType = getParticleType(config);
            if (particleType != null) {
                destWorld.sendParticles(
                    particleType,
                    x, y + 0.5, z,
                    25, 0.3, 0.6, 0.3, 0.05
                );
            }
        }

        if (config.soundEnabled) {
            SoundEvent soundEvent = getSoundEvent(config);
            if (soundEvent != null) {
                destWorld.playSound(
                    null, x, y, z,
                    soundEvent,
                    SoundSource.PLAYERS,
                    0.6f, 1.0f
                );
            }
        }
    }

    public void removePlayer(UUID uuid) {
        wasOnGround.remove(uuid);
        wasSneaking.remove(uuid);
        prevY.remove(uuid);
        cooldownExpiry.remove(uuid);
    }

    private Set<Block> getElevatorBlocks(ElevatorConfig config) {
        String cacheKey = String.join("|", config.elevatorBlocks);
        if (!cacheKey.equals(cachedElevatorBlocksKey)) {
            Set<Block> resolvedBlocks = new HashSet<>();
            for (String blockId : config.elevatorBlocks) {
                try {
                    Block resolvedBlock = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId));
                    if (resolvedBlock == Blocks.AIR) {
                        ElevatorMod.LOGGER.warn("{} Invalid elevator block in config: {}", ElevatorMod.logPrefix(), blockId);
                    } else {
                        resolvedBlocks.add(resolvedBlock);
                    }
                } catch (Exception e) {
                    ElevatorMod.LOGGER.warn("{} Invalid elevator block in config: {}", ElevatorMod.logPrefix(), blockId);
                }
            }
            cachedElevatorBlocks = Set.copyOf(resolvedBlocks);
            cachedElevatorBlocksKey = cacheKey;
        }
        return cachedElevatorBlocks;
    }

    private SimpleParticleType getParticleType(ElevatorConfig config) {
        if (!config.particleType.equals(cachedParticleId)) {
            cachedParticle = resolveSimpleParticle(config.particleType);
            cachedParticleId = config.particleType;
        }
        return cachedParticle;
    }

    private SoundEvent getSoundEvent(ElevatorConfig config) {
        if (!config.soundEvent.equals(cachedSoundId)) {
            cachedSound = resolveSoundEvent(config.soundEvent);
            cachedSoundId = config.soundEvent;
        }
        return cachedSound;
    }

    private SimpleParticleType resolveSimpleParticle(String particleId) {
        try {
            Object particle = BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(particleId));
            if (particle instanceof SimpleParticleType simpleParticle) {
                return simpleParticle;
            }
        } catch (Exception ignored) {
        }

        ElevatorMod.LOGGER.warn("{} Invalid or unsupported particle in config: {}", ElevatorMod.logPrefix(), particleId);
        return null;
    }

    private SoundEvent resolveSoundEvent(String soundId) {
        try {
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse(soundId));
            if (soundEvent != null) {
                return soundEvent;
            }
        } catch (Exception ignored) {
        }

        ElevatorMod.LOGGER.warn("{} Invalid sound event in config: {}", ElevatorMod.logPrefix(), soundId);
        return null;
    }
}
