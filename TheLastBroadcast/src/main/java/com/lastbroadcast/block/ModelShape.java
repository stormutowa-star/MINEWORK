package com.lastbroadcast.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Collision/selection shapes authored in pixels for the NORTH-facing model (front toward -Z),
 * rotated to match the blockstate y-rotations (east = 90, south = 180, west = 270, clockwise).
 */
public final class ModelShape {
    private final Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);

    private ModelShape(double[][] boxes) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
            for (double[] b : boxes) {
                double[] r = rotate(b, d);
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape, Block.box(r[0], r[1], r[2], r[3], r[4], r[5]));
            }
            byFacing.put(d, shape.optimize());
        }
    }

    public static ModelShape of(double[]... boxes) {
        return new ModelShape(boxes);
    }

    public static ModelShape full() {
        return of(new double[]{0, 0, 0, 16, 16, 16});
    }

    public VoxelShape get(Direction facing) {
        VoxelShape s = byFacing.get(facing.getAxis().isHorizontal() ? facing : Direction.NORTH);
        return s == null ? net.minecraft.world.phys.shapes.Shapes.block() : s;
    }

    private static double[] rotate(double[] b, Direction d) {
        double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        int turns = switch (d) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        for (int i = 0; i < turns; i++) {
            // 90 degrees clockwise seen from above: (x, z) -> (16 - z, x)
            double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
            x0 = nx0; x1 = nx1; z0 = nz0; z1 = nz1;
        }
        return new double[]{x0, y0, z0, x1, y1, z1};
    }

}
