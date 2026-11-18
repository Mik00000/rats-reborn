package com.github.alexthe666.rats.compat.jade;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class RatsJadePlugin implements IWailaPlugin {
   private static final RatHouseProvider RAT_HOUSE_PROVIDER = new RatHouseProvider();

   @Override
   public void register(IWailaCommonRegistration registration) {
      try {
         registration.registerBlockDataProvider(RAT_HOUSE_PROVIDER, RatHouseBlockEntity.class);
      } catch (Throwable t) {
         RatsMod.LOGGER.error("Failed to register Jade server data", t);
      }
   }

   @Override
   public void registerClient(IWailaClientRegistration registration) {
      try {
         registration.registerBlockComponent(RAT_HOUSE_PROVIDER, RatHouseBlock.class);
      } catch (Throwable t) {
         RatsMod.LOGGER.error("Failed to register Jade client component", t);
      }
   }

   private static class RatHouseProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
      private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("rats", "rat_house");

      @Override
      public ResourceLocation getUid() {
         return UID;
      }

      @Override
      public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
         try {
            if (accessor.getBlockEntity() instanceof RatHouseBlockEntity house) {
               tag.putInt("RatHouseRatCount", house.getStoredRatCount());
               String name = house.getStoredRatName();
               if (name != null) {
                  tag.putString("RatHouseRatName", name);
               }
               tag.putInt("RatHouseRespawnTicks", house.getPendingRespawnTicks());
            }
         } catch (Throwable t) {
            RatsMod.LOGGER.error("Failed to append Jade server data for rat house", t);
         }
      }

      @Override
      public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
         try {
            CompoundTag tag = accessor.getServerData();
            int count = tag.getInt("RatHouseRatCount");
            String name = tag.getString("RatHouseRatName");
            int respawn = tag.getInt("RatHouseRespawnTicks");

            if (!tag.contains("RatHouseRatCount") && accessor.getBlockEntity() instanceof RatHouseBlockEntity house) {
               count = house.getStoredRatCount();
               name = house.getStoredRatName();
               respawn = house.getPendingRespawnTicks();
            }

            if (respawn > 0) {
               int seconds = Math.max(1, (respawn + 19) / 20);
               tooltip.add(Component.translatable("jade.rats.rat_house.respawn", seconds));
            } else if (count <= 0) {
               tooltip.add(Component.translatable("jade.rats.rat_house.empty"));
            } else if (name == null || name.isBlank()) {
               tooltip.add(Component.translatable("jade.rats.rat_house.occupied", count, 1));
            } else {
               tooltip.add(Component.translatable("jade.rats.rat_house.rat", name));
            }
         } catch (Throwable t) {
            RatsMod.LOGGER.error("Failed to append Jade tooltip for rat house at {}", accessor.getPosition(), t);
         }
      }
   }
}
