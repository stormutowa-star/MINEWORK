package com.lastbroadcast.item;

import com.lastbroadcast.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** Quarter-inch magnetic tape on a 7" reel. Blank, or holding a recorded programme id. */
public class TapeItem extends Item {
    public TapeItem(Properties props) {
        super(props);
    }

    public static ItemStack recorded(String broadcastId, String label) {
        ItemStack stack = new ItemStack(ModItems.MAGNETIC_TAPE.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("broadcast", broadcastId);
        tag.putString("label", label);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        return stack;
    }

    public static String broadcastId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        return tag.contains("broadcast") ? tag.getString("broadcast") : null;
    }

    public static String label(ItemStack stack) {
        String live = com.lastbroadcast.content.ContentLibrary.tapeLabel(broadcastId(stack));
        if (live != null && !live.isEmpty()) return live;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString("label");
    }

    public static boolean isBlank(ItemStack stack) {
        return broadcastId(stack) == null;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(isBlank(stack) ? "item.lastbroadcast.magnetic_tape" : "item.lastbroadcast.magnetic_tape.recorded");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        if (isBlank(stack)) {
            lines.add(Component.translatable("lastbroadcast.item.tape.blank_1").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("lastbroadcast.item.tape.blank_2").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lines.add(Component.translatable("lastbroadcast.item.tape.label", label(stack)).withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("lastbroadcast.item.tape.play").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
