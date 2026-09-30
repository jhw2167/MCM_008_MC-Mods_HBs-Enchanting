package com.holybuckets.enchanting.networking;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

public class Codecs {


    //BlockStatsSync
    public static final FriendlyByteBuf encodeBlockStatsSync(BlockStatsSyncMessage object, FriendlyByteBuf buf) {
        buf.writeBoolean(object.replace);
        buf.writeUtf(object.json, 32000);
        return buf;
    }

    public static final BlockStatsSyncMessage decodeBlockStatsSync(FriendlyByteBuf buf) {
        boolean replace = buf.readBoolean();
        return new BlockStatsSyncMessage(replace, buf.readUtf(32000));
    }

}
