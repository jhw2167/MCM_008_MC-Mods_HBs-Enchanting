package com.holybuckets.enchanting.mixin;

import com.holybuckets.enchanting.block.ModBlocks;
import com.holybuckets.enchanting.client.EnchantingTierClient;
import com.holybuckets.enchanting.config.json.EnchantingTableJsonConfig;
import com.holybuckets.enchanting.config.model.EnchantingTierCaps;
import com.holybuckets.enchanting.config.ModConfig;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sends the opened table's tier settings to the client so the screen can draw the cap markers.
 * The menu itself is left alone so Apotheosis keeps ownership of it.
 */
@Mixin(EnchantmentTableBlock.class)
public class MixinEnchantmentTableBlock {

    @Inject(method = "use", at = @At("RETURN"))
    private void hbs_enchanting$sendTierCaps(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit,
                                             CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) return;

        EnchantingTierCaps caps = ModConfig.getInstance().getTierCaps(ModBlocks.getTableTier(state));
        SimpleStringMessage.createAndFire(player, EnchantingTierClient.MESSAGE_ID,
            new EnchantingTableJsonConfig(caps).toJson());
    }
}
