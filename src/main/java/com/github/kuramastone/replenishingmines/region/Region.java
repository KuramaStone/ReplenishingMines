package com.github.kuramastone.replenishingmines.region;

import com.github.kuramastone.ReplenishingMines;
import com.github.kuramastone.utils.BrushableBlockEntityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrushableBlock;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.*;

public class Region {

    // constructor values
    private final ServerWorld world;
    private final BlockPos low;
    private final BlockPos high;
    private String brushableBlockLootTable;
    private int regenSpeedInTicks;
    private boolean regenInstantly;

    // class values
    // Storage for the blockstate per pos
    private RegionData data;

    private int currentRegenTimerIndex = 0; // regen timer
    private double currentBlockIndex = 0; // The current index of block placement
    private boolean temporarilyInstant = false;

    public Region(ServerWorld world, BlockPos low, BlockPos high, String brushableBlockLootTableID, int regenSpeedInTicks, boolean regenInstantly) {
        this(world, low, high, brushableBlockLootTableID, regenSpeedInTicks, regenInstantly, null);
    }

    public Region(ServerWorld world, BlockPos low, BlockPos high, String brushableBlockLootTableID, int regenSpeedInTicks, boolean regenInstantly, RegionData data) {
        this.world = world;
        this.low = new BlockPos(Math.min(low.getX(), high.getX()), Math.min(low.getY(), high.getY()), Math.min(low.getZ(), high.getZ()));
        this.high = new BlockPos(Math.max(low.getX(), high.getX()), Math.max(low.getY(), high.getY()), Math.max(low.getZ(), high.getZ()));
        this.brushableBlockLootTable = brushableBlockLootTableID;
        this.regenSpeedInTicks = regenSpeedInTicks;
        this.regenInstantly = regenInstantly;
        this.data = data;
    }

    public void regenTick() {
        if(regenSpeedInTicks <= 0)
            return;

        if (regenInstantly && !temporarilyInstant) {
            if (currentRegenTimerIndex >= regenSpeedInTicks) {
                currentBlockIndex = 0;
                temporarilyInstant = true;
            }
            else {
                currentRegenTimerIndex = Math.min(currentRegenTimerIndex + 1, regenSpeedInTicks);
                return;
            }
        }

        double blocksToRegen;
        if (temporarilyInstant) {
            // if set to regen instantly, use max speed after time has ended
            blocksToRegen = ReplenishingMines.getApi().getConfigOptions().instantRegenBlocksPerTick;
        }
        else {
            // if not regen instantly, then regen gradually every tick
            blocksToRegen = getRegenSpeed();
        }

        if(blocksToRegen <= 0)
            return;

        List<ServerPlayerEntity> nearbyPlayers = world.getPlayers(p -> this.contains(world, p.getBlockPos()));

        boolean regenComplete = regenerateBlocks(nearbyPlayers, blocksToRegen);
        // after regen index is at the max, restart
        if (regenComplete) {
            restartRegenIndex();
            return;
        }


        currentRegenTimerIndex = Math.min(currentRegenTimerIndex + 1, regenSpeedInTicks);
    }

    private void restartRegenIndex() {
        currentRegenTimerIndex = 0;
        currentBlockIndex = 0;
        temporarilyInstant = false;
    }

    private double getRegenSpeed() {
        return (double) data.size() / regenSpeedInTicks;
    }

    /**
     * Checks if this position is within the bounds of this Region. Inclusive of both the high and low corner.
     */
    public boolean contains(ServerWorld world, BlockPos pos) {
        if (world != null && world.equals(this.world)) {

            if (pos.getX() >= low.getX() && pos.getX() <= high.getX()) {
                if (pos.getY() >= low.getY() && pos.getY() <= high.getY()) {
                    if (pos.getZ() >= low.getZ() && pos.getZ() <= high.getZ()) {
                        return true;
                    }
                }
            }

        }

        return false;
    }

    /**
     * Saves the current blockstate data as {@link RegionData}
     */
    public void saveData() {

        List<Pair<BlockPos, BlockState>> map = new ArrayList<>();
        for (int y = low.getY(); y <= high.getY(); y++) {
            for (int x = low.getX(); x <= high.getX(); x++) {
                for (int z = low.getZ(); z <= high.getZ(); z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(blockPos);
                    state = processState(state);
                    BlockPos offset = blockPos.mutableCopy().subtract(low);
                    map.add(Pair.of(offset, state));
                }
            }
        }

        this.data = new RegionData(map);
    }

    /**
     * Make any required changes to this blockstate
     *
     * @param state
     * @return
     */
    private BlockState processState(BlockState state) {
        return state;
    }

    /**
     * Regenerate this many blocks
     *
     * @return True if all blocks have been regenerated
     */
    public boolean regenerateBlocks(List<ServerPlayerEntity> nearby, double blocksToRegen) {
        double oldValue = currentBlockIndex;
        double newValue = currentBlockIndex + blocksToRegen;
        if (Math.floor(oldValue) == Math.floor(newValue)) {
            // hasnt incremented up, keep waiting for that increment
            currentBlockIndex += blocksToRegen;
            return false;
        }

        int start = (int) oldValue % data.size();
        int end = (int) Math.min(newValue, data.size());

        // place blocks, but never place more than the max.
        for (int i = start; i < end; i++) {
            Pair<BlockPos, BlockState> pair = data.getStatesByPosition().get(i);
            regenerate(nearby, pair);
            currentRegenTimerIndex = (currentRegenTimerIndex + 1) % data.size();
        }
        currentBlockIndex = newValue;

        if (currentBlockIndex >= data.size()) {
            restartRegenIndex();
            return true;
        }

        return false;
    }

    private void regenerate(List<ServerPlayerEntity> nearby, Pair<BlockPos, BlockState> pair) {
        BlockPos offset = pair.getKey();
        BlockPos positionInWorld = low.mutableCopy().add(offset);
        BlockState state = pair.getRight();

        int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
        world.setBlockState(positionInWorld, state, flags, 0);

        List<ServerPlayerEntity> inBlock = nearby.stream().filter(p -> p.getBlockPos().equals(positionInWorld)).toList();
        for (ServerPlayerEntity serverPlayerEntity : inBlock) {
            serverPlayerEntity.teleport(world,
                    positionInWorld.getX(), positionInWorld.getY() + 1, positionInWorld.getZ(),
                    serverPlayerEntity.getYaw(), serverPlayerEntity.getPitch());
        }

        // if the block is BrushableBlock, then manually set the loot tables
        if (state.getBlock() instanceof BrushableBlock) {
            BrushableBlockEntity blockEntity = (BrushableBlockEntity) world.getBlockEntity(positionInWorld);
            Objects.requireNonNull(blockEntity, "BrushableBlockEntity is null.");
            long seed = world.getSeed() + positionInWorld.getX() * 74355L - positionInWorld.getY() * 412541L + positionInWorld.getZ() * 6235L;

            if (brushableBlockLootTable != null) {
                ItemStack loot = ReplenishingMines.getApi().getLootManager().getLoot(brushableBlockLootTable).getLoot(seed);
                BrushableBlockEntityUtils.setItem(blockEntity, loot);
            }
            else {
                BrushableBlockEntityUtils.setSeed(blockEntity, seed);
            }

        }

    }

    public BlockPos getLow() {
        return low;
    }

    public ServerWorld getWorld() {
        return world;
    }

    public BlockPos getHigh() {
        return high;
    }

    public String getBrushableBlockLootTable() {
        return brushableBlockLootTable;
    }

    public int getRegenSpeedInTicks() {
        return regenSpeedInTicks;
    }

    public boolean shouldRegenInstantly() {
        return regenInstantly;
    }

    public boolean isRegenInstantly() {
        return regenInstantly;
    }

    public void setRegenInstantly(boolean regenInstantly) {
        this.regenInstantly = regenInstantly;
    }

    public void setRegenSpeedInTicks(int regenSpeedInTicks) {
        this.regenSpeedInTicks = regenSpeedInTicks;
    }

    public void setBrushableBlockLootTable(String brushableBlockLootTable) {
        this.brushableBlockLootTable = brushableBlockLootTable;
    }

    public RegionData getData() {
        return data;
    }

    /**
     * Set the regen timer to the end to cause it to instantly regenerate
     */
    public void regenImmediately() {
        this.temporarilyInstant = true;
    }
}
