package com.mik.rats_squirrel.debug;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.mik.rats_squirrel.ai.RatSquirrelHarvestGoal;
import com.mik.rats_squirrel.config.SquirrelConfig;
import com.mik.rats_squirrel.deposit.DepositGate;
import com.mik.rats_squirrel.deposit.DepositStatus;
import com.mik.rats_squirrel.tree.FailureBlacklist;
import com.mik.rats_squirrel.tree.LevelFruitIndex;
import com.mik.rats_squirrel.tree.TreeCluster;
import com.mik.rats_squirrel.tree.TrunkRoute;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Operator commands for inspecting and debugging the squirrel harvest state machine.
 */
@EventBusSubscriber(modid = "rats")
public final class SquirrelDebugCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_RAT =
            new SimpleCommandExceptionType(Component.literal("Target is not a tamed rat."));

    private SquirrelDebugCommand() {
    }

    /** Registers the {@code /rats_squirrel} command tree. */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("rats_squirrel")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("status")
                                .executes(SquirrelDebugCommand::status))
                        .then(Commands.literal("debug")
                                .then(Commands.argument("rat", EntityArgument.entity())
                                        .executes(SquirrelDebugCommand::debug)))
                        .then(Commands.literal("scan")
                                .then(Commands.argument("rat", EntityArgument.entity())
                                        .executes(SquirrelDebugCommand::scan)))
                        .then(Commands.literal("blacklist")
                                .then(Commands.literal("clear")
                                        .executes(SquirrelDebugCommand::clearBlacklist)
                                        .then(Commands.argument("rat", EntityArgument.entity())
                                                .executes(SquirrelDebugCommand::clearRatBlacklist))))
                        .then(Commands.literal("deposit")
                                .then(Commands.literal("check")
                                        .then(Commands.argument("rat", EntityArgument.entity())
                                                .executes(SquirrelDebugCommand::depositCheck))))
        );
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        LevelFruitIndex index = LevelFruitIndex.get(level);

        return reply(context, "Status: searchRadius=" + SquirrelConfig.searchRadius()
                + ", maxTreeHeight=" + SquirrelConfig.maxTreeHeight()
                + ", scanSectionsPerTick=" + SquirrelConfig.scanSectionsPerTick()
                + ", canopySearchNodeLimit=" + SquirrelConfig.canopyPathNodeLimit()
                + ", debugLogging=" + SquirrelConfig.debugLogging()
                + ", debugParticles=" + SquirrelConfig.debugParticles()
                + ", knownFruits=" + index.knownFruitsCount()
                + ", queuedSections=" + index.queuedSectionsCount());
    }

    private static int debug(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        TamedRat rat = getRat(context);
        RatSquirrelHarvestGoal goal = findGoal(rat);
        if (goal == null) {
            return reply(context, "No squirrel harvest goal attached to " + rat.getName().getString() + ".");
        }

        String taskInfo = "none";
        if (goal.task() != null) {
            taskInfo = (goal.task().cluster().isBush() ? "Bush" : "Tree")
                    + " fruits=" + goal.task().fruitQueue().size()
                    + " kind=" + goal.task().kind().id();
        }

        net.minecraft.world.item.ItemStack held = rat.getMainHandItem();
        String hand = held.isEmpty() ? "empty" : held.getCount() + "x" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem());
        return reply(context, "rat=" + rat.getName().getString()
                + " phase=" + goal.phase()
                + " ticksInPhase=" + goal.ticksInPhase()
                + " mode=" + goal.movement().mode()
                + " waypoint=" + goal.movement().waypointIndex() + "/" + goal.movement().waypoints().size()
                + " task=[" + taskInfo + "]"
                + " hand=" + hand
                + " deposit=" + (rat.getDepositPos().isPresent() ? DepositGate.check(rat, held) : "none")
                + " command=" + rat.getCommand()
                + " canWork=" + (rat.canWorkBySchedule() && rat.canWorkFromHunger())
                + " blacklistEntries=" + goal.blacklist().size());
    }

    private static int scan(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        TamedRat rat = getRat(context);
        ServerLevel level = context.getSource().getLevel();
        LevelFruitIndex index = LevelFruitIndex.get(level);
        int gameTime = (int) level.getGameTime();

        index.probeImmediate(rat.blockPosition(), gameTime);
        RatSquirrelHarvestGoal goal = findGoal(rat);
        FailureBlacklist blacklist = goal != null ? goal.blacklist() : new FailureBlacklist();

        List<TreeCluster> clusters = index.query(rat.blockPosition(), SquirrelConfig.searchRadius(), rat.getMainHandItem(), blacklist, gameTime);
        if (clusters.isEmpty()) {
            return reply(context, "Scan for " + rat.getName().getString() + ": 0 reachable clusters found.");
        }

        StringBuilder sb = new StringBuilder("Scan for " + rat.getName().getString() + " found " + clusters.size() + " cluster(s):\n");
        for (int i = 0; i < clusters.size(); i++) {
            TreeCluster c = clusters.get(i);
            sb.append(String.format("  #%d: id=%d, %s, fruits=%d",
                    i + 1, c.id(), c.isBush() ? "Bush" : "Tree", c.fruitCount()));
            if (!c.trunks().isEmpty()) {
                TrunkRoute route = c.trunks().getFirst();
                sb.append(String.format(", trunkHeight=%d, standPos=%s", route.height(), route.standPos().toShortString()));
            }
            sb.append("\n");
        }

        return reply(context, sb.toString().trim());
    }

    private static int clearBlacklist(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        int cleared = 0;
        for (Entity e : level.getAllEntities()) {
            if (e instanceof TamedRat rat) {
                RatSquirrelHarvestGoal goal = findGoal(rat);
                if (goal != null && goal.blacklist().size() > 0) {
                    goal.blacklist().clear();
                    cleared++;
                }
            }
        }
        return reply(context, "Cleared blacklist for " + cleared + " loaded rat(s).");
    }

    private static int clearRatBlacklist(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        TamedRat rat = getRat(context);
        RatSquirrelHarvestGoal goal = findGoal(rat);
        if (goal == null) {
            return reply(context, "No squirrel harvest goal attached to " + rat.getName().getString() + ".");
        }
        int count = goal.blacklist().size();
        goal.blacklist().clear();
        return reply(context, "Cleared " + count + " blacklist entries for " + rat.getName().getString() + ".");
    }

    private static int depositCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        TamedRat rat = getRat(context);
        DepositStatus status = DepositGate.check(rat, rat.getMainHandItem());
        String pos = rat.getDepositPos().map(gp -> gp.dimension().location() + " @ " + gp.pos().toShortString()).orElse("none");
        return reply(context, "Deposit for " + rat.getName().getString() + ": status=" + status + ", configuredPos=" + pos);
    }

    private static TamedRat getRat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "rat");
        if (!(entity instanceof TamedRat rat)) {
            throw ERROR_NOT_RAT.create();
        }
        return rat;
    }

    @Nullable
    private static RatSquirrelHarvestGoal findGoal(TamedRat rat) {
        for (var wrapped : rat.goalSelector.getAvailableGoals()) {
            Goal goal = wrapped.getGoal();
            if (goal instanceof RatSquirrelHarvestGoal squirrelGoal) {
                return squirrelGoal;
            }
        }
        return null;
    }

    private static int reply(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal("[Rats Squirrel] " + message), false);
        return 1;
    }
}
