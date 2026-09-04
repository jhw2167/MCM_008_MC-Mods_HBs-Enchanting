package com.holybuckets.enchanting.menu;

import com.holybuckets.enchanting.block.ModBlocks;
import com.holybuckets.enchanting.config.ModConfig;
import com.holybuckets.enchanting.core.EnchantingPowerCalculator;
import com.holybuckets.foundation.item.ModItems;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * Enchanting menu for both HB enchanting tables.
 * <p>
 * Differences from vanilla: enchanting power is gathered from a large configurable radius with per
 * block type caps, the lapis slot is an optional Enchanted Essence slot, and the copper variant
 * has no secondary slot at all and a hard power cap.
 */
public class ModEnchantmentMenu extends EnchantmentMenu {

    public static final int ESSENCE_SLOT = 1;
    /** Index of the enchantment seed data slot registered by the vanilla menu. */
    private static final int SEED_DATA_SLOT = 3;
    private static final int HIDDEN_SLOT_X = -2000;

    private final ContainerLevelAccess tableAccess;
    private final Container enchantContainer;
    private final RandomSource random = RandomSource.create();
    private final boolean copper;

    private int seed;

    /** Client side constructor. */
    public ModEnchantmentMenu(int containerId, Inventory playerInventory, boolean copper) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, copper);
    }

    public ModEnchantmentMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, boolean copper) {
        super(containerId, playerInventory, access);
        this.tableAccess = access;
        this.copper = copper;
        this.enchantContainer = this.slots.get(0).container;
        this.seed = playerInventory.player.getEnchantmentSeed();
        this.setData(SEED_DATA_SLOT, this.seed);
        this.slots.set(ESSENCE_SLOT, buildSecondarySlot());
    }

    public boolean isCopper() {
        return copper;
    }

    @Override
    public MenuType<?> getType() {
        return ModMenus.enchantmentMenu.get();
    }

    /**
     * The secondary item is no longer required, so the vanilla screen is told the player always
     * has enough of it; otherwise it renders and gates every row as unaffordable.
     */
    @Override
    public int getGoldCount() {
        return 64;
    }

    private Slot buildSecondarySlot() {
        int x = copper ? HIDDEN_SLOT_X : 35;
        int y = copper ? HIDDEN_SLOT_X : 47;
        return new Slot(this.enchantContainer, ESSENCE_SLOT, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !copper && isEssence(stack);
            }

            @Override
            public boolean isActive() {
                return !copper;
            }
        };
    }

    public static boolean isEssence(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.enchantedEssence.get());
    }

    private int tableMaxPower() {
        return copper
            ? ModConfig.getInstance().getCopperTableMaxPower()
            : ModConfig.getInstance().getStandardTableMaxPower();
    }

    /**
     * Vanilla checks the block at the access position is literally minecraft:enchanting_table,
     * which closes the copper table the instant it opens. Accept either table instead.
     */
    @Override
    public boolean stillValid(Player player) {
        return this.tableAccess.evaluate((level, pos) -> {
            if (!isEnchantingTable(level.getBlockState(pos))) return false;
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }, true);
    }

    private static boolean isEnchantingTable(BlockState state) {
        return state.is(Blocks.ENCHANTING_TABLE) || state.is(ModBlocks.copperEnchantingTable);
    }

    @Override
    public void slotsChanged(Container container) {
        if (container != this.enchantContainer) return;

        ItemStack stack = container.getItem(0);
        if (stack.isEmpty() || !stack.isEnchantable()) {
            for (int i = 0; i < 3; i++) {
                this.costs[i] = 0;
                this.enchantClue[i] = -1;
                this.levelClue[i] = -1;
            }
            return;
        }

        this.tableAccess.execute((level, pos) -> {
            int power = EnchantingPowerCalculator.getPower(level, pos, tableMaxPower());

            this.random.setSeed(this.seed);
            for (int i = 0; i < 3; i++) {
                this.costs[i] = EnchantmentHelper.getEnchantmentCost(this.random, i, power, stack);
                this.enchantClue[i] = -1;
                this.levelClue[i] = -1;
                if (this.costs[i] < i + 1) this.costs[i] = 0;
            }

            for (int i = 0; i < 3; i++) {
                if (this.costs[i] <= 0) continue;
                List<EnchantmentInstance> options = getEnchantmentList(stack, i, this.costs[i]);
                if (options == null || options.isEmpty()) continue;
                EnchantmentInstance chosen = options.get(this.random.nextInt(options.size()));
                this.enchantClue[i] = BuiltInRegistries.ENCHANTMENT.getId(chosen.enchantment);
                this.levelClue[i] = chosen.level;
            }

            this.broadcastChanges();
        });
    }

    private List<EnchantmentInstance> getEnchantmentList(ItemStack stack, int slot, int cost) {
        this.random.setSeed(this.seed + slot);
        List<EnchantmentInstance> options = EnchantmentHelper.selectEnchantment(this.random, stack, cost, false);
        if (stack.is(Items.BOOK) && options.size() > 1) {
            options.remove(this.random.nextInt(options.size()));
        }
        return options;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= this.costs.length) return false;

        ItemStack input = this.enchantContainer.getItem(0);
        int levelCost = id + 1;

        if (this.costs[id] <= 0 || input.isEmpty()) return false;
        boolean creative = player.getAbilities().instabuild;
        if (!creative && (player.experienceLevel < levelCost || player.experienceLevel < this.costs[id])) return false;

        this.tableAccess.execute((level, pos) -> {
            ItemStack result = input;
            List<EnchantmentInstance> options = getEnchantmentList(input, id, this.costs[id]);
            if (options.isEmpty()) return;

            player.onEnchantmentPerformed(input, levelCost);
            boolean isBook = input.is(Items.BOOK);
            if (isBook) {
                result = new ItemStack(Items.ENCHANTED_BOOK);
                CompoundTag tag = input.getTag();
                if (tag != null) result.setTag(tag.copy());
                this.enchantContainer.setItem(0, result);
            }

            for (EnchantmentInstance instance : options) {
                if (isBook) {
                    EnchantedBookItem.addEnchantment(result, instance);
                } else {
                    result.enchant(instance.enchantment, instance.level);
                }
            }

            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, result, levelCost);
            }

            this.enchantContainer.setChanged();
            this.seed = player.getEnchantmentSeed();
            this.setData(SEED_DATA_SLOT, this.seed);
            this.slotsChanged(this.enchantContainer);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS,
                1.0f, level.random.nextFloat() * 0.1f + 0.9f);
        });

        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return moved;

        ItemStack stack = slot.getItem();
        moved = stack.copy();

        if (index == 0) {
            if (!this.moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (index == ESSENCE_SLOT) {
            if (!this.moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (isEssence(stack) && !copper) {
            if (!this.moveItemStackTo(stack, ESSENCE_SLOT, ESSENCE_SLOT + 1, true)) return ItemStack.EMPTY;
        } else {
            if (this.slots.get(0).hasItem() || !this.slots.get(0).mayPlace(stack)) return ItemStack.EMPTY;
            ItemStack single = stack.copy();
            single.setCount(1);
            stack.shrink(1);
            this.slots.get(0).set(single);
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == moved.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return moved;
    }
}
