package com.holybuckets.enchanting.mixin.apotheosis;

import com.holybuckets.enchanting.client.EnchantingTierClient;
import com.holybuckets.enchanting.core.EnchantmentCalculator;
import com.holybuckets.enchanting.core.TableCalculator;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.TableStats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

/**
 * Hijacks apotheosis enchanting calculator
 */
@Mixin(value = ApothEnchantmentMenu.class, remap = false)
public abstract class MixinApothEnchantmentMenuSelect {

    @Shadow
    protected TableStats stats;

    @Shadow
    @Final
    protected Player player;

    @Unique
    private boolean hbs_enchanting$hadItem = false;


    @Inject(method = "slotsChanged", at = @At("HEAD"), remap = true)
    private void hbs_enchanting$trackInsert(Container container, CallbackInfo ci) {
        if (this.player.level().isClientSide) return;

        ItemStack stack = ((ApothEnchantmentMenu) (Object) this).getSlot(0).getItem();
        boolean hasItem = !stack.isEmpty();

        if (hasItem && !this.hbs_enchanting$hadItem) {
            EnchantmentCalculator.Quanta.onInsert(this.player.getUUID(), stack);
        }
        this.hbs_enchanting$hadItem = hasItem;
    }

    /** Tells the client which reroll page it is looking at, for the quanta popup. */
    @Inject(method = "slotsChanged", at = @At("TAIL"), remap = true)
    private void hbs_enchanting$sendPage(Container container, CallbackInfo ci) {
        if (this.player.level().isClientSide) return;

        UUID id = this.player.getUUID();
        ItemStack stack = ((ApothEnchantmentMenu) (Object) this).getSlot(0).getItem();
        EnchantmentCalculator.Quanta.Key key = EnchantmentCalculator.Quanta.keyOf(id, stack);
        SimpleStringMessage.createAndFire(this.player, EnchantingTierClient.PAGE_MESSAGE_ID,
            EnchantmentCalculator.Quanta.getPage(key) + "/" + EnchantmentCalculator.Quanta.getTotalPages(key)
                + "/" + EnchantmentCalculator.Quanta.getRemainingRerolls(key));

        SimpleStringMessage.createAndFire(this.player, EnchantingTierClient.LEDGER_MESSAGE_ID,
            TableCalculator.getLastLedgerJson());
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), remap = false)
    private void hbs_enchanting$markApplying(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        EnchantmentCalculator.markApplying(true);
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"), remap = false)
    private void hbs_enchanting$clearApplying(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        EnchantmentCalculator.markApplying(false);
    }

    @Inject(
        method = "getEnchantmentList(Lnet/minecraft/world/item/ItemStack;II)Ljava/util/List;",
        at = @At("RETURN"), cancellable = true, remap = false)
    private void hbs_enchanting$select(ItemStack stack, int enchantSlot, int level,
                                       CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        List<EnchantmentInstance> selected = EnchantmentCalculator.select(stack, this.player.getUUID(), enchantSlot, level,
            this.stats.quanta(), this.stats.arcana(), this.stats.rectification(),
            cir.getReturnValue());

        //A row with no option is not offered at all; zeroing the cost stops it being drawn
        if (selected.isEmpty()) {
            ((EnchantmentMenu) (Object) this).costs[enchantSlot] = 0;
        }

        cir.setReturnValue(selected);
    }
}
