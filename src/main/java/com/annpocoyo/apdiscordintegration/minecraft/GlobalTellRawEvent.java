package com.annpocoyo.apdiscordintegration.minecraft;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalTellRawEvent {
    private static final Set<GlobalTellRaw> listeners = ConcurrentHashMap.newKeySet();

    public static void invoke(Component message, CommandSourceStack commandSourceStack) {
        for (GlobalTellRaw listener : listeners)
            listener.onGlobalTellRaw(message, commandSourceStack);
    }

    public static void register(GlobalTellRaw listener) {
        listeners.add(listener);
    }

    @FunctionalInterface
    public interface GlobalTellRaw {
        /**
         * Called on a {@code /tellraw} command that targets {@code @a}.
         * @param message Resolved message sent
         * @param commandSourceStack Command Source Stack associated with command
         */
        void onGlobalTellRaw(Component message, CommandSourceStack commandSourceStack);
    }
}
