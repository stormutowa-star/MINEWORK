package com.lastbroadcast.item;

import com.lastbroadcast.ClientBridge;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.content.DocumentDef;
import com.lastbroadcast.registry.ModItems;
import com.lastbroadcast.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/** A paper document (letter, log, notice). Use to read. */
public class DocumentItem extends Item {
    public DocumentItem(Properties props) {
        super(props);
    }

    public static ItemStack create(String docId) {
        ItemStack stack = new ItemStack(ModItems.DOCUMENT.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("doc", docId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static String docId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString("doc");
    }

    @Override
    public Component getName(ItemStack stack) {
        DocumentDef d = ContentLibrary.DOCUMENTS.get(docId(stack));
        return d != null ? Component.literal(d.title) : super.getName(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String id = docId(stack);
        if (level.isClientSide) {
            if (ContentLibrary.DOCUMENTS.containsKey(id)) ClientBridge.openDocument(id);
        } else {
            level.playSound(null, player.blockPosition(), ModSounds.PAPER.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.1f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        DocumentDef d = ContentLibrary.DOCUMENTS.get(docId(stack));
        if (d != null && d.header != null && !d.header.isEmpty()) lines.add(Component.literal(d.header).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Use to read").withStyle(ChatFormatting.DARK_GRAY));
    }
}
