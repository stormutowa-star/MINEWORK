package com.lastbroadcast.blockentity;

import com.lastbroadcast.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Filing cabinet / notice board holding evidence tokens (doc:..., tape:blank). Never runs out. */
public class DocumentHolderBlockEntity extends SyncedBlockEntity {
    private final List<String> contents = new ArrayList<>();

    public DocumentHolderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOCUMENT_HOLDER.get(), pos, state);
    }

    public List<String> getContents() {
        return contents;
    }

    public void setContents(List<String> tokens) {
        contents.clear();
        contents.addAll(tokens);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (String s : contents) list.add(StringTag.valueOf(s));
        tag.put("contents", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents.clear();
        ListTag list = tag.getList("contents", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) contents.add(list.getString(i));
    }
}
