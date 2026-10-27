package com.servicehub.model;

import java.util.Set;

public enum RequestStatus {
    PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED;

    public static final Set<RequestStatus> ACTIVE = Set.of(PENDING, ACCEPTED, IN_PROGRESS);

    public boolean canTransitionTo(RequestStatus next) {
        return switch (this) {
            case PENDING -> next == ACCEPTED || next == CANCELLED;
            case ACCEPTED -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
