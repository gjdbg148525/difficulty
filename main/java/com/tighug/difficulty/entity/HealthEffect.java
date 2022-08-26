package com.tighug.difficulty.entity;

public class HealthEffect {
    public static final HealthEffect EMPTY = new HealthEffect(0) {
        @Override
        public float getAmplifier() {
            return 0;
        }

        @Override
        public boolean isEffective() {
            return false;
        }
    };
    protected byte count = 0;
    protected final float amplifier;

    public HealthEffect(float amplifier1) {
        amplifier = amplifier1;
    }

    public float getAmplifier() {
        if (++count <= 10) return amplifier;
        return 0;
    }

    public boolean isEffective() {
        return count <= 10;
    }
}