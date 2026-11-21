package com.agrichain.events;

@FunctionalInterface
public interface AgriChainEventListener {
    void handleEvent(AgriChainEvent event);
}
