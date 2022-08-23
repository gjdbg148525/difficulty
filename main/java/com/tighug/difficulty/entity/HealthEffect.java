package com.tighug.difficulty.entity;

public class HealthEffect {
    private byte count = 0;
    private final float amplifier;

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