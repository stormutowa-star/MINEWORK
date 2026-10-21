package com.interactioncraft.mod.action;

/** Matematica pura del piloto automatico (sin Minecraft, para poder probarla aparte). */
final class FlightMath {
    private FlightMath() {
    }

    static float wrap(float deg) {
        float d = deg % 360.0f;
        if (d >= 180.0f) {
            d -= 360.0f;
        }
        if (d < -180.0f) {
            d += 360.0f;
        }
        return d;
    }

    /** Rumbo (grados, convencion de Minecraft: 0 = +Z, 90 = -X) hacia un desplazamiento horizontal. */
    static float yawToward(double dx, double dz) {
        return (float) (Math.atan2(-dx, dz) * 57.29577951308232);
    }

    static float turn(float current, float wanted, float max) {
        float diff = wrap(wanted - current);
        if (diff > max) {
            diff = max;
        }
        if (diff < -max) {
            diff = -max;
        }
        return wrap(current + diff);
    }

    static double fx(float yaw, double speed) {
        return -Math.sin(Math.toRadians(yaw)) * speed;
    }

    static double fz(float yaw, double speed) {
        return Math.cos(Math.toRadians(yaw)) * speed;
    }

    static double ramp(double current, double wanted, double step) {
        if (current < wanted) {
            return Math.min(wanted, current + step);
        }
        return Math.max(wanted, current - step);
    }

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Punto de la circunferencia (centro, radio) un poco por delante de donde esta ahora el vehiculo. */
    static double[] circleAim(double cx, double cz, double radius, double px, double pz, double fallbackYaw, double lead) {
        double dx = px - cx;
        double dz = pz - cz;
        double phi;
        if (dx * dx + dz * dz < 1.0) {
            phi = Math.atan2(fz((float) fallbackYaw, 1.0), fx((float) fallbackYaw, 1.0));
        } else {
            phi = Math.atan2(dz, dx);
        }
        double a = phi + lead;
        return new double[]{cx + Math.cos(a) * radius, cz + Math.sin(a) * radius};
    }
}
