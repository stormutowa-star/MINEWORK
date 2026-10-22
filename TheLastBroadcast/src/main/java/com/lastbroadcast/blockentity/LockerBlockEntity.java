package com.lastbroadcast.blockentity;

import com.lastbroadcast.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** Penrose's engineering locker with a four-wheel combination lock. */
public class LockerBlockEntity extends SyncedBlockEntity {
    public static final String COMBINATION = "1025";
    private boolean open = false;

    public LockerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOCKER.get(), pos, state);
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("open", open);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        open = tag.getBoolean("open");
    }
}
