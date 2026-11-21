package com.agrichain.events;

public class AgriChainEvent {
    private final String eventType;
    private final String message;

    public AgriChainEvent(String eventType, String message) {
        this.eventType = eventType;
        this.message = message;
    }

    public String getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return eventType + " - " + message;
    }
}
