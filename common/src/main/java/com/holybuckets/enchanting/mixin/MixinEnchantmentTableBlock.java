package com.holybuckets.enchanting.mixin;

import com.holybuckets.enchanting.menu.EnchantingTableMenuProvider;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes the vanilla enchanting table to the HB enchantment menu so it picks up the wider
 * bookshelf search radius, the per block power caps, and the optional essence slot.
 */
@Mixin(EnchantmentTableBlock.class)
public class MixinEnchantmentTableBlock {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void hbs_enchanting$openModMenu(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, BlockHitResult hit,
                                            CallbackInfoReturnable<InteractionResult> cir) {
        if(true) return;
        if (level.isClientSide) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof EnchantmentTableBlockEntity table)) return;

        Balm.getNetworking().openMenu(player,
            new EnchantingTableMenuProvider(level, pos, table.getDisplayName(), false));
        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
