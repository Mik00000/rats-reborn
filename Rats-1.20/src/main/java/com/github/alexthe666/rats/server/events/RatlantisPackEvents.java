package com.github.alexthe666.rats.server.events;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.server.message.ChangeRatlantisStatusPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = "rats")
public class RatlantisPackEvents {

    static {
        RatsMod.RATLANTIS_DATAPACK_ENABLED = true;
        RatConfig.ratlantisEnabledByDefault = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        RatsMod.RATLANTIS_DATAPACK_ENABLED = true;
        RatConfig.ratlantisEnabledByDefault = true;
    }

    @SubscribeEvent
    public static void checkDatapackOnLoad(LevelEvent.Load event) {
        RatsMod.RATLANTIS_DATAPACK_ENABLED = true;
        RatConfig.ratlantisEnabledByDefault = true;
        LevelAccessor level = event.getLevel();
        if (level instanceof Level && ((Level) level).dimension().equals(Level.OVERWORLD)) {
            MinecraftServer server = level.getServer();
            if (server != null) {
                RatsNetworkHandler.CHANNEL.send(RatsNetworkHandler.allTarget(), new ChangeRatlantisStatusPacket(true));
                RatsMod.LOGGER.info("Ratlantis content is permanently unlocked for this world.");
            }
        }
    }

    @SubscribeEvent
    public static void checkDatapackOnSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        RatsMod.RATLANTIS_DATAPACK_ENABLED = true;
        RatConfig.ratlantisEnabledByDefault = true;
        if (player != null) {
            RatsNetworkHandler.CHANNEL.send(RatsNetworkHandler.playerTarget(() -> player), new ChangeRatlantisStatusPacket(true));
        } else if (event.getPlayerList() != null) {
            event.getPlayerList().getPlayers().forEach(p -> {
                RatsNetworkHandler.CHANNEL.send(RatsNetworkHandler.playerTarget(() -> p), new ChangeRatlantisStatusPacket(true));
            });
        }
    }
}
