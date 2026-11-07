package com.healthcare.entity;

import java.util.EnumSet;
import java.util.Set;

/** PENDING -> CONFIRMED -> COMPLETED, and PENDING/CONFIRMED -> CANCELLED. COMPLETED and CANCELLED are final. */
public enum AppointmentStatus {
    PENDING, CONFIRMED, COMPLETED, CANCELLED;

    /** True while the appointment occupies its time slot. */
    public boolean occupiesSlot() {
        return this == PENDING || this == CONFIRMED;
    }

    public Set<AppointmentStatus> allowedNext() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(AppointmentStatus.class);
        };
    }
}
