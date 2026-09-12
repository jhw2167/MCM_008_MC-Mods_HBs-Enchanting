package com.holybuckets.enchanting.command;

//Project imports

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.CommandRegistry;
import com.holybuckets.enchanting.LoggerProject;
import com.holybuckets.enchanting.core.EnchantmentCalculator;
import com.holybuckets.enchanting.core.EnchantmentCalculator.Quanta;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandList {

    public static final String CLASS_ID = "033";
    private static final String PREFIX = "hbsEnchanting";

    public static void register() {
        //CommandRegistry.register(LocateClusters::noArgs);
        //CommandRegistry.register(LocateClusters::limitCount);
        //CommandRegistry.register(LocateClusters::limitCountSpecifyBlockType);
        CommandRegistry.register(TestEnchant::testEnchant);
    }

    //1. Locate Clusters
    private static class LocateClusters
    {
        // Register the base command with no arguments
        private static LiteralArgumentBuilder<CommandSourceStack> noArgs() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("locateClusters")
                    .executes(context -> execute(context.getSource(), -1, null)) // Default case (no args)
                );

        }

        // Register command with count argument
        private static LiteralArgumentBuilder<CommandSourceStack> limitCount() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("locateClusters")
                    .then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int count = IntegerArgumentType.getInteger(context, "count");
                            return execute(context.getSource(), count, null);
                        })
                    )
            );
        }

        // Register command with both count and blockType OR just blockType
        private static LiteralArgumentBuilder<CommandSourceStack> limitCountSpecifyBlockType() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("locateClusters")
                    .then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .then(Commands.argument("blockType", StringArgumentType.string())
                            .executes(context -> {
                                int count = IntegerArgumentType.getInteger(context, "count");
                                String blockType = StringArgumentType.getString(context, "blockType");
                                return execute(context.getSource(), count, blockType);
                            })
                        )
                    )
                    .then(Commands.argument("blockType", StringArgumentType.string())
                        .executes(context -> {
                            String blockType = StringArgumentType.getString(context, "blockType");
                            return execute(context.getSource(), -1, blockType);
                        })
                    )
            );
        }


        private static int execute(CommandSourceStack source, int count, String blockType)
        {

            LoggerProject.logDebug("010001", "Locate Clusters Command");
            return 0;
        }


    }
    //END COMMAND



    //2. Test Enchant
    private static class TestEnchant
    {
        private static LiteralArgumentBuilder<CommandSourceStack> testEnchant() {
            return Commands.literal(PREFIX)
                .then(Commands.literal("testEnchant")
                    .then(Commands.argument("eterna", FloatArgumentType.floatArg(0f))
                        .then(Commands.argument("quanta", FloatArgumentType.floatArg(0f))
                            .then(Commands.argument("arcana", FloatArgumentType.floatArg(0f))
                                .then(Commands.argument("total", IntegerArgumentType.integer(1))
                                    .executes(context -> execute(context.getSource(),
                                        FloatArgumentType.getFloat(context, "eterna"),
                                        FloatArgumentType.getFloat(context, "quanta"),
                                        FloatArgumentType.getFloat(context, "arcana"),
                                        IntegerArgumentType.getInteger(context, "total")))
                                )
                            )
                        )
                    )
                );
        }

        //Runs the table pipeline against the held item without opening a table
        private static int execute(CommandSourceStack source, float eterna, float quanta, float arcana, int total)
        {
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                source.sendFailure(Component.literal("testEnchant must be run by a player"));
                return 0;
            }

            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) {
                source.sendFailure(Component.literal("Hold the item to test in your main hand"));
                return 0;
            }

            float maxWeight = Quanta.getMaxCombinationsCount(quanta, eterna, arcana);
            Quanta.Key key = Quanta.keyOf(player.getUUID(), stack);

            LoggerProject.logInfo(CLASS_ID + "010", String.format(
                "testEnchant %s eterna=%.1f quanta=%.1f arcana=%.1f runs=%d maxCombinationWeight=%.2f rerolls=%d",
                stack.getItem(), eterna, quanta, arcana, total, maxWeight, Quanta.rerollsAtQuanta(quanta)));

            for (int run = 1; run <= total; run++)
            {
                RandomSource random = RandomSource.create(player.getUUID().hashCode() + run);
                int cost = EnchantmentCalculator.Eterna.getEnchantmentCost(random, 0, eterna, stack);
                List<EnchantmentInstance> valid = EnchantmentCalculator.Eterna.getValidEnchantments(random, stack, cost);

                Quanta.clear(key);
                Quanta.buildOptions(random, key, stack, quanta, eterna, arcana, valid);
                Quanta.Session session = Quanta.SESSIONS.get(key);

                LoggerProject.logInfo(CLASS_ID + "011", String.format(
                    "run %d/%d cost=%d rolled=%d options=%d pages=%d",
                    run, total, cost, valid.size(),
                    session == null ? 0 : session.pool.size(), Quanta.getTotalPages(key)));

                if (session == null) {
                    LoggerProject.logInfo(CLASS_ID + "012", "  no combinations available");
                    continue;
                }

                for (int i = 0; i < session.pool.size(); i++) {
                    Set<EnchantmentInstance> option = session.pool.get(i);
                    LoggerProject.logInfo(CLASS_ID + "013", String.format("  [%d] weight=%.2f/%.2f %s",
                        i, Quanta.weigh(option), maxWeight, names(option)));
                }

                List<EnchantmentInstance> chosen = Quanta.getCachedOptions(key, 0);
                List<EnchantmentInstance> applied = EnchantmentCalculator.Arcana.apply(random, stack, arcana, 0f, chosen);
                LoggerProject.logInfo(CLASS_ID + "014", "  applied " + names(applied));
            }

            Quanta.clear(key);
            source.sendSuccess(() -> Component.literal("testEnchant wrote " + total + " results to the log"), false);
            return 1;
        }

        private static String names(java.util.Collection<EnchantmentInstance> enchantments) {
            return enchantments.stream()
                .map(e -> BuiltInRegistries.ENCHANTMENT.getKey(e.enchantment).getPath() + " " + e.level)
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));
        }

    }
    //END COMMAND


}
//END CLASS COMMANDLIST
