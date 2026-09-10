package com.holybuckets.enchanting.mixin;

import com.holybuckets.enchanting.block.ModBlocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AbstractContainerMenu.class)
public class MixinAbstractContainerMenu {

    @Inject(method = "stillValid(Lnet/minecraft/world/inventory/ContainerLevelAccess;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/Block;)Z",
        at = @At("HEAD"), cancellable = true)
    private static void hbs_enchanting$allowModTables(ContainerLevelAccess access, Player player, Block targetBlock,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (targetBlock != Blocks.ENCHANTING_TABLE) return;
        if (ModBlocks.copperEnchantingTable == null) return;

        boolean valid = access.evaluate((level, pos) -> {
            if (!level.getBlockState(pos).is(ModBlocks.copperEnchantingTable)) return false;
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }, false);

        if (valid) cir.setReturnValue(true);
    }
}
