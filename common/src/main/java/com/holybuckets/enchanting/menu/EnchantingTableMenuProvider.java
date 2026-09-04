package com.holybuckets.enchanting.menu;

import net.blay09.mods.balm.api.menu.BalmMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;

/**
 * Opens the shared HB enchantment menu for either table variant.
 */
public class EnchantingTableMenuProvider implements BalmMenuProvider {

    private final Level level;
    private final BlockPos pos;
    private final Component title;
    private final boolean copper;

    public EnchantingTableMenuProvider(Level level, BlockPos pos, Component title, boolean copper) {
        this.level = level;
        this.pos = pos;
        this.title = title;
        this.copper = copper;
    }

    @Override
    public Component getDisplayName() {
        return title;
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new ModEnchantmentMenu(syncId, playerInventory, ContainerLevelAccess.create(level, pos), copper);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
        buf.writeBoolean(copper);
    }
}
