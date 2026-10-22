package com.lastbroadcast.client;

import com.lastbroadcast.LastBroadcast;
import com.lastbroadcast.blockentity.RecorderBlockEntity;
import com.lastbroadcast.blockentity.TelevisionBlockEntity;
import com.lastbroadcast.broadcast.Channels;
import com.lastbroadcast.client.hud.Hud;
import com.lastbroadcast.client.screen.*;
import com.lastbroadcast.entity.StaffNpc;
import com.lastbroadcast.item.TapeItem;
import com.lastbroadcast.network.NoticePacket;
import com.lastbroadcast.registry.ModItems;
import com.lastbroadcast.story.CaseData;
import com.lastbroadcast.story.Flags;
import com.lastbroadcast.story.StoryDirector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Development-only scripted walkthrough used for automated visual verification. Inactive unless
 * the JVM is started with -Dlastbroadcast.autotest=true. Drives a singleplayer session through
 * the station, opens every screen and saves screenshots, then quits.
 */
public final class DevAutotest {
    public static final boolean ENABLED = Boolean.getBoolean("lastbroadcast.autotest");
    private static int tick = 0;
    private static int step = 0;
    private static int waitUntil = 0;
    private static final List<Runnable> STEPS = new ArrayList<>();

    private DevAutotest() {}

    private static MinecraftServer server() {
        return Minecraft.getInstance().getSingleplayerServer();
    }

    private static BlockPos o() {
        return CaseData.get(server()).origin;
    }

    private static void cmd(String c) {
        MinecraftServer s = server();
        s.execute(() -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c));
    }

    private static void standLook(double x, double y, double z, double tx, double ty, double tz) {
        BlockPos o = o();
        Vec3 from = new Vec3(o.getX() + x, o.getY() + y, o.getZ() + z);
        Vec3 to = new Vec3(o.getX() + tx, o.getY() + ty, o.getZ() + tz);
        Vec3 eye = from.add(0, 1.62, 0);
        Vec3 d = to.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        cmd(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", from.x, from.y, from.z, yaw, pitch));
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "autotest_" + name + ".png", mc.getMainRenderTarget(), c -> LastBroadcast.LOG.info("[autotest] {}", c.getString()));
    }

    private static void onServer(java.util.function.Consumer<ServerLevel> r) {
        MinecraftServer s = server();
        s.execute(() -> r.accept(s.overworld()));
    }

    private static void build() {
        Minecraft mc = Minecraft.getInstance();
        // 1 exterior at dusk
        STEPS.add(() -> {
            cmd("gamerule doDaylightCycle false");
            cmd("time set 13200");
            cmd("gamerule doMobSpawning false");
            cmd("gamerule fallDamage false");
            standLook(5.5, 1, 40.5, 5.5, 5, 20);
        });
        STEPS.add(() -> shot("01_exterior"));
        STEPS.add(() -> standLook(10.5, 1, 25.5, 7.5, 1.6, 25.5));
        STEPS.add(() -> shot("02_lobby"));
        STEPS.add(() -> standLook(21.5, 1, 8.5, 19.5, 2.2, 1.5));
        STEPS.add(() -> shot("03_control_room"));
        STEPS.add(() -> onServer(l -> {
            StoryDirector.setFlag(l.getServer(), Flags.MET_ROSA);
            StoryDirector.setFlag(l.getServer(), Flags.SIGNAL_LOCKED);
            if (l.getBlockEntity(o().offset(21, 2, 1)) instanceof TelevisionBlockEntity tv) tv.setState(true, Channels.CH_7B);
        }));
        STEPS.add(() -> standLook(16.5, 1, 2.5, 17.5, 2.45, 1.4));
        STEPS.add(() -> shot("04_tv_closeup"));
        STEPS.add(() -> standLook(21.5, 1, 3.5, 21.5, 2.45, 1.4));
        STEPS.add(() -> shot("05_tv_7b_closeup"));
        STEPS.add(() -> mc.setScreen(new TelevisionScreen(o().offset(21, 2, 1))));
        STEPS.add(() -> shot("06_tv_screen"));
        STEPS.add(() -> mc.setScreen(new AnalyzerScreen(o().offset(24, 2, 4))));
        STEPS.add(() -> shot("07_analyzer"));
        STEPS.add(() -> {
            mc.setScreen(null);
            onServer(l -> {
                for (String f : new String[]{Flags.D1, Flags.D2, Flags.GOT_REEL, Flags.D3}) StoryDirector.setFlag(l.getServer(), f);
                for (String e : new String[]{"doc:assignment", "doc:complaint_log", "doc:program_log", "tape:p1_today_thursday", "tape:p3_report_thursday",
                        "tape:p4_last_broadcast", "doc:transmitter_log", "doc:procedure"}) StoryDirector.logEvidence(l.getServer(), e);
                if (l.getBlockEntity(o().offset(24, 2, 6)) instanceof RecorderBlockEntity deck) {
                    deck.eject();
                    deck.insert(TapeItem.recorded("p4_last_broadcast", "Special Bulletin"));
                }
            });
            standLook(22.5, 1, 6.5, 24.5, 2.5, 6.5);
        });
        STEPS.add(() -> shot("08_deck_in_world"));
        STEPS.add(() -> {
            RecorderScreen rs = new RecorderScreen(o().offset(24, 2, 6));
            mc.setScreen(rs);
            rs.devSeek(22.35);
        });
        STEPS.add(() -> shot("09_recorder_frame"));
        STEPS.add(() -> {
            RecorderScreen rs = new RecorderScreen(o().offset(24, 2, 6));
            mc.setScreen(rs);
            rs.devSeek(15.0);
        });
        STEPS.add(() -> shot("10_recorder_bulletin"));
        STEPS.add(() -> mc.setScreen(new JournalScreen()));
        STEPS.add(() -> shot("11_journal"));
        STEPS.add(() -> mc.setScreen(new DocumentScreen("transmitter_log", null)));
        STEPS.add(() -> shot("12_document"));
        STEPS.add(() -> mc.setScreen(new KeypadScreen(o().offset(27, 1, 12))));
        STEPS.add(() -> shot("13_keypad"));
        STEPS.add(() -> {
            onServer(l -> StoryDirector.setFlag(l.getServer(), Flags.PENROSE_CONFESSED));
            standLook(30.5, 1, 6.5, 30.5, 1.5, 4.5);
        });
        STEPS.add(() -> mc.setScreen(new ConsoleScreen(o().offset(30, 1, 4))));
        STEPS.add(() -> shot("14_console"));
        STEPS.add(() -> {
            mc.setScreen(null);
            standLook(27.5, 1, 10.5, 31, 1.5, 4.5);
        });
        STEPS.add(() -> shot("15_transmitter_hall"));
        STEPS.add(() -> standLook(21.5, 1, 22.5, 31.5, 2.0, 20.5));
        STEPS.add(() -> shot("16_studio"));
        STEPS.add(() -> {
            standLook(19.5, 1, 6.6, 19.5, 2.0, 4.5);
        });
        STEPS.add(() -> {
            for (var e : mc.level.entitiesForRendering()) {
                if (e instanceof StaffNpc n && "rosa".equals(n.getCharacter())) mc.setScreen(new DialogueScreen(n.getId(), "rosa"));
            }
        });
        STEPS.add(() -> shot("17_dialogue"));
        STEPS.add(() -> {
            mc.setScreen(null);
            standLook(30.5, 1, -11.5, 30.5, 22, -5);
        });
        STEPS.add(() -> shot("18_tower"));
        STEPS.add(() -> {
            onServer(l -> l.getServer().getPlayerList().getPlayers().get(0).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIELD_METER.get())));
            standLook(28.5, 1, 6.5, 30.5, 1.5, 4.5);
            Hud.notice(NoticePacket.DEDUCTION, "Ghosts Keep a Schedule");
        });
        STEPS.add(() -> shot("19_hud"));
        // ---- end-to-end story logic: every deduction, a failed shutdown, then the correct one
        STEPS.add(() -> {
            mc.setScreen(null);
            cmd("lastbroadcast reset");
        });
        STEPS.add(() -> onServer(l -> {
            var server = l.getServer();
            var p = server.getPlayerList().getPlayers().get(0);
            StoryDirector.setFlag(server, Flags.MET_ROSA);
            StoryDirector.setFlag(server, Flags.SIGNAL_LOCKED);
            String[][] pairs = {{"tape:p1_today_thursday", "doc:program_log"}, {"tape:p3_report_thursday", "doc:assignment"},
                    {"doc:transmitter_log", "doc:complaint_log"}, {"frame:console", "doc:procedure"}, {"tape:p2_lucky_dial", "tape:futura_ad_62"}};
            StoryDirector.compare(p, "doc:program_log", "doc:assignment");
            for (String[] pr : pairs) {
                StoryDirector.logEvidence(server, pr[0]);
                StoryDirector.logEvidence(server, pr[1]);
                StoryDirector.compare(p, pr[0], pr[1]);
            }
            com.lastbroadcast.story.DialogueLogic.apply(p, "penrose", "confess2", 0);
            LastBroadcast.LOG.info("[autotest] logic after deductions: flags={} deductions={}", CaseData.get(server).state.flags, CaseData.get(server).state.deductions);
            if (l.getBlockEntity(o().offset(30, 1, 4)) instanceof com.lastbroadcast.blockentity.ConsoleBlockEntity con) {
                con.action(p, com.lastbroadcast.blockentity.ConsoleBlockEntity.A_DRIVE_DOWN, 0);
                LastBroadcast.LOG.info("[autotest] logic: drive down without coolant -> fault={} ARC_FAULT_SEEN={}",
                        con.getFaultUntil() > l.getGameTime(), CaseData.get(server).state.has(Flags.ARC_FAULT_SEEN));
            }
        }));
        STEPS.add(() -> shot("20_arc_fault"));
        STEPS.add(() -> { });
        STEPS.add(() -> onServer(l -> {
            var server = l.getServer();
            var p = server.getPlayerList().getPlayers().get(0);
            if (l.getBlockEntity(o().offset(30, 1, 4)) instanceof com.lastbroadcast.blockentity.ConsoleBlockEntity con) {
                LastBroadcast.LOG.info("[autotest] logic: after relay reset drive={} valves={}", con.getDrive(), con.getValves());
                con.action(p, com.lastbroadcast.blockentity.ConsoleBlockEntity.A_VALVE, 4);
                for (int i = 0; i < 10; i++) con.action(p, com.lastbroadcast.blockentity.ConsoleBlockEntity.A_DRIVE_DOWN, 0);
                con.action(p, com.lastbroadcast.blockentity.ConsoleBlockEntity.A_PLATE, 0);
                con.action(p, com.lastbroadcast.blockentity.ConsoleBlockEntity.A_GROUND, 0);
                LastBroadcast.LOG.info("[autotest] logic: shutdown complete={} CASE_SOLVED={}", con.isShutDown(), CaseData.get(server).state.has(Flags.CASE_SOLVED));
            }
        }));
        STEPS.add(() -> standLook(20.5, 1, 7.5, 19.5, 2.2, 1.5));
        STEPS.add(() -> shot("21_signoff"));
        for (int i = 0; i < 18; i++) STEPS.add(() -> { });
        STEPS.add(() -> {
            LastBroadcast.LOG.info("[autotest] logic: ending screen open={}", mc.screen instanceof EndingScreen);
            if (!(mc.screen instanceof EndingScreen)) mc.setScreen(new EndingScreen());
        });
        STEPS.add(() -> shot("22_ending"));
        STEPS.add(() -> {
            LastBroadcast.LOG.info("[autotest] finished");
            mc.stop();
        });
    }

    public static void tick() {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || server() == null) return;
        tick++;
        if (STEPS.isEmpty()) {
            build();
            waitUntil = tick + 200;
        }
        if (tick < waitUntil || step >= STEPS.size()) return;
        try {
            STEPS.get(step).run();
        } catch (Exception e) {
            LastBroadcast.LOG.error("[autotest] step {} failed", step, e);
        }
        step++;
        waitUntil = tick + 50;
    }
}
