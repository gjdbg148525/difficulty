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
    private final byte maxCount;
    private final float amplifier;
    private byte count = 0;

    public HealthEffect(float amplifier1) {
        this(amplifier1, (byte) 10);
    }

    public HealthEffect(float amplifier1, byte b) {
        amplifier = amplifier1;
        maxCount = b;
    }

    public float getAmplifier() {
        if (isEffective()) {
            ++count;
            return amplifier;
        }
        return 0;
    }

    public boolean isEffective() {
        return remainderCount() > 0;
    }

    public byte remainderCount() {
        return (byte) Math.max(0, maxCount - count);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        else if (obj instanceof HealthEffect) {
            HealthEffect healthEffect = (HealthEffect) obj;
            if (this.isEffective() && healthEffect.isEffective()) {
                return amplifier == healthEffect.amplifier && remainderCount() == healthEffect.remainderCount();
            }
            return !(this.isEffective() || healthEffect.isEffective());
        }
        return false;
    }

}