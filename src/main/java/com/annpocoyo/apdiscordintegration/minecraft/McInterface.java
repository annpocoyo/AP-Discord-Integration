package com.annpocoyo.apdiscordintegration.minecraft;

/**
 * Abstraction layer for anything contacting Minecraft
 * Intended to be used to facilitate future multi version support!
 */
public interface McInterface {
    /** Begin to receive and send events. **/
    void start();
}
