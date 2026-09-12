package com.holybuckets.enchanting.mixin.apotheosis.client;

import com.holybuckets.enchanting.client.BlockStatsTooltip;
import dev.shadowsoffire.apotheosis.ench.EnchModuleClient;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EnchModuleClient.class, remap = false)
public class MixinEnchModuleClient {

    @Inject(method = "tooltips", at = @At("RETURN"), remap = false)
    private void hbs_enchanting$blockStats(ItemTooltipEvent event, CallbackInfo ci) {
        BlockStatsTooltip.append(event.getItemStack(), event.getToolTip());
    }
}
