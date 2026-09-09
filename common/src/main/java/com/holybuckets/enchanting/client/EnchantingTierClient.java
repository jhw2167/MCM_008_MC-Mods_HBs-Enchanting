package com.holybuckets.enchanting.client;

import com.holybuckets.enchanting.LoggerProject;
import com.holybuckets.enchanting.config.model.EnchantingTierCaps;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;

/**
 * Client side cache of the tier settings for the enchanting table currently open.
 * <p>
 * The client menu is built with ContainerLevelAccess.NULL and never learns the block position, so
 * the server sends the tier's caps as JSON when the table is opened.
 */
public class EnchantingTierClient {

    public static final String MESSAGE_ID = "hbs_enchanting_tier";
    private static final String CLASS_ID = "015";

    private static EnchantingTierCaps caps = EnchantingTierCaps.getDefault(EnchantingTierCaps.TIER_NORMAL);

    private EnchantingTierClient() {}

    public static void init(ClientEventRegistrar registrar) {
        registrar.registerOnSimpleMessage(MESSAGE_ID, EnchantingTierClient::onTierMessage);
    }

    private static void onTierMessage(SimpleMessageEvent event) {
        try {
            caps = EnchantingTierCaps.fromJson(event.getContent());
        } catch (RuntimeException e) {
            LoggerProject.logError(CLASS_ID + "001",
                "Could not read enchanting tier payload: " + event.getContent());
        }
    }

    public static EnchantingTierCaps getCaps() {
        return caps;
    }
}
