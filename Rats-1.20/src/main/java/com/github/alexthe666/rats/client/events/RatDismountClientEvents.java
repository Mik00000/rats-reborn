package com.github.alexthe666.rats.client.events;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.message.DismountRatPacket;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = "rats", value = Dist.CLIENT)
public class RatDismountClientEvents {
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() && event.getEntity().isShiftKeyDown()) {
            Player player = event.getEntity();
            boolean hasRats = false;
            for (Entity passenger : player.getPassengers()) {
                if (passenger instanceof TamedRat rat) {
                    hasRats = true;
                    rat.stopRiding();
                    Vec3 dismountPos = rat.getDismountLocationForPassenger(player);
                    rat.setPos(dismountPos.x(), dismountPos.y(), dismountPos.z());
                    RatsNetworkHandler.CHANNEL.sendToServer(new DismountRatPacket(rat.getId()));
                }
            }
            if (!hasRats) {
                RatsNetworkHandler.CHANNEL.sendToServer(new DismountRatPacket(-1));
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getLevel().isClientSide() && event.getEntity().isShiftKeyDown()) {
            Player player = event.getEntity();
            if (player.getPassengers().isEmpty()) {
                RatsNetworkHandler.CHANNEL.sendToServer(new DismountRatPacket(-1));
            }
        }
    }
}
