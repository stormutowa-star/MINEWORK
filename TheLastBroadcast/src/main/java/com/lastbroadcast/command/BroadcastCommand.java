package com.lastbroadcast.command;

import com.lastbroadcast.story.CaseData;
import com.lastbroadcast.story.StoryDirector;
import com.lastbroadcast.world.StationBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Operator utilities: rebuild the station, inspect or adjust case progress. */
public final class BroadcastCommand {
    private BroadcastCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("lastbroadcast").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status").executes(ctx -> {
                    CaseData data = CaseData.get(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("Station built: " + data.stationBuilt + " at " + data.origin.toShortString()
                            + "\nFlags: " + data.state.flags + "\nEvidence: " + data.state.evidence + "\nDeductions: " + data.state.deductions), false);
                    return 1;
                }))
                .then(Commands.literal("flag").then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                    String f = StringArgumentType.getString(ctx, "name");
                    StoryDirector.setFlag(ctx.getSource().getServer(), f);
                    ctx.getSource().sendSuccess(() -> Component.literal("Set flag " + f), true);
                    return 1;
                })))
                .then(Commands.literal("evidence").then(Commands.argument("id", StringArgumentType.greedyString()).executes(ctx -> {
                    String e = StringArgumentType.getString(ctx, "id");
                    StoryDirector.logEvidence(ctx.getSource().getServer(), e);
                    ctx.getSource().sendSuccess(() -> Component.literal("Logged " + e), true);
                    return 1;
                })))
                .then(Commands.literal("reset").executes(ctx -> {
                    CaseData data = CaseData.get(ctx.getSource().getServer());
                    data.state.flags.clear();
                    data.state.evidence.clear();
                    data.state.deductions.clear();
                    data.state.ch7.set(0, java.util.List.of(), java.util.List.of());
                    data.state.ch7b.set(0, java.util.List.of(), java.util.List.of());
                    data.endingStartedAt = -1;
                    data.endingScreenSent = false;
                    StoryDirector.sync(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("Case file reset. Rebuild the station to restore machines and staff."), true);
                    return 1;
                }))
                .then(Commands.literal("station").executes(ctx -> {
                    BlockPos p = BlockPos.containing(ctx.getSource().getPosition());
                    StationBuilder.build(ctx.getSource().getLevel(), p.offset(-5, -1, -31));
                    ctx.getSource().sendSuccess(() -> Component.literal("WKVR station built north of you."), true);
                    return 1;
                })));
    }
}
