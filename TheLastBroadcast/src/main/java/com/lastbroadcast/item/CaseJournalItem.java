package com.lastbroadcast.item;

import com.lastbroadcast.ClientBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** FCC field case file #62-1147: evidence, comparisons, deductions, current lead. */
public class CaseJournalItem extends Item {
    public CaseJournalItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) ClientBridge.openJournal();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("lastbroadcast.item.journal.1").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("lastbroadcast.item.journal.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
