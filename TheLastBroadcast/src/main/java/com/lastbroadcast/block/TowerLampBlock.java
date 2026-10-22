package com.lastbroadcast.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/** Aircraft-warning beacon at the top of the Skyhook. */
public class TowerLampBlock extends LampBlock {
    public TowerLampBlock(Properties props, ModelShape shape) {
        super(props, shape);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(4) == 0) {
            level.addParticle(new DustParticleOptions(new Vector3f(1f, 0.15f, 0.1f), 1.2f),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
        }
    }
}
