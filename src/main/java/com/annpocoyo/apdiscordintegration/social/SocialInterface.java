package com.annpocoyo.apdiscordintegration.social;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Abstraction layer for anything communicating with social platforms
 * Allows potential support of platforms other than Discord.
 */
public interface SocialInterface {
    /** Begin to send and receive events. */
    void start();

    // Events
    /** Event called when ready to receive events and able to start sending events */
    ReadyEvent getReadyEvent();
    class ReadyEvent {
        CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<>();
        @FunctionalInterface
        interface Listener {
            void onEvent();
        }

        /** Call Event */
        void invoke() {
            listeners.forEach(Listener::onEvent);
        }

        /**
         * Register listener
         * @param listener Listener to call on event
         * @return {@code true} if a new listener was added, {@code false} if the listener was already registered!
         */
        boolean register(Listener listener) {
            return listeners.add(listener);
        }
    }
}
