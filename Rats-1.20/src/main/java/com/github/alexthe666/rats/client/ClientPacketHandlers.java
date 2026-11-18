package com.github.alexthe666.rats.client;

import com.github.alexthe666.rats.client.gui.CheeseStaffScreen;
import com.github.alexthe666.rats.client.gui.PatrolStaffScreen;
import com.github.alexthe666.rats.client.gui.RadiusStaffScreen;
import com.github.alexthe666.rats.client.gui.RatScreen;
import com.github.alexthe666.rats.client.util.RatRecordSoundInstance;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.inventory.RatMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

public class ClientPacketHandlers {
   public static void openRatScreen(RatMenu menu, Inventory inventory, TamedRat rat) {
      Minecraft.getInstance().setScreen(RatScreen.forRat(menu, inventory, rat));
   }

   public static void openStaffScreen(int staffToOpen, TamedRat rat, BlockPos pos, int dirOrd) {
      switch (staffToOpen) {
         case 1:
            Minecraft.getInstance().setScreen(new RadiusStaffScreen(rat, pos));
            break;
         case 2:
            Minecraft.getInstance().setScreen(new PatrolStaffScreen(rat, pos));
            break;
         default:
            Minecraft.getInstance().setScreen(new CheeseStaffScreen(rat, pos, Direction.values()[dirOrd]));
      }
   }

   public static void playRatMusic(TamedRat rat, Item record) {
      Minecraft.getInstance().getSoundManager().queueTickingSound(new RatRecordSoundInstance(rat, record));
      Minecraft.getInstance().gui.setNowPlaying(record.getDescription());
   }
}
