package com.lastbroadcast.item;

import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.CaseData;
import com.lastbroadcast.story.Flags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Portable field-strength meter. Held: live needle on the HUD (client). Used: takes a logged
 * reading with a bearing toward the strongest radiator — the overdrive amplifier.
 */
public class FieldMeterItem extends Item {
    public FieldMeterItem(Properties props) {
        super(props);
    }

    /** Field strength 0..1 at a position, given the overdrive source position. Shared by HUD and readings. */
    public static float strength(Vec3 at, BlockPos source, boolean overdrive) {
        if (!overdrive || source.equals(BlockPos.ZERO)) return 0.04f;
        double d = Math.sqrt(at.distanceToSqr(Vec3.atCenterOf(source)));
        return (float) Math.max(0.04, Math.min(1.0, 1.15 / (1.0 + d * d / 90.0)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            CaseData data = CaseData.get(sp.server);
            boolean overdrive = data.stationBuilt && !data.state.has(Flags.CASE_SOLVED);
            float s = strength(player.position(), data.consolePos, overdrive);
            int dbuv = Math.round(40 + s * 80);
            String bearing = "none";
            if (overdrive) {
                Vec3 dir = Vec3.atCenterOf(data.consolePos).subtract(player.position());
                double ang = Math.toDegrees(Math.atan2(dir.x, -dir.z));
                String[] names = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};
                bearing = names[(int) Math.floorMod(Math.round(ang / 45.0), 8)];
            }
            level.playSound(null, player.blockPosition(), ModSounds.METER_CLICK.get(), SoundSource.PLAYERS, 0.7f, 0.8f + s * 0.6f);
            String text = overdrive
                    ? String.format("Field strength %d dBuV at 175.25 Mc - strongest bearing: %s%s", dbuv, bearing, s > 0.9f ? " (pinned - you are on top of it)" : "")
                    : "Field strength 44 dBuV - normal background. The carrier is quiet.";
            player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GREEN), true);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Halcyon Instruments Model 498 field-strength meter").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Hold to watch the needle; use to log a reading").withStyle(ChatFormatting.DARK_GRAY));
    }
}
