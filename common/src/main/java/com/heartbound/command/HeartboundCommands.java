package com.heartbound.command;

import com.heartbound.relationship.RelationshipData;
import com.heartbound.relationship.RelationshipStage;
import com.heartbound.relationship.RomanceableMobs;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Debug commands (operators only):
 * /heartbound affinity get <mob>
 * /heartbound affinity set <mob> <value>
 * /heartbound affinity add <mob> <amount>
 *
 * The affinity is always between the targeted mob and the player who runs the command.
 */
public final class HeartboundCommands {

    private HeartboundCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        int max = RelationshipStage.MAX_AFFINITY;
        dispatcher.register(Commands.literal("heartbound")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("affinity")
                        .then(Commands.literal("get")
                                .then(Commands.argument("mob", EntityArgument.entity())
                                        .executes(HeartboundCommands::get)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("mob", EntityArgument.entity())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, max))
                                                .executes(HeartboundCommands::set))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("mob", EntityArgument.entity())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-max, max))
                                                .executes(HeartboundCommands::add))))));
    }

    private static Entity resolveMob(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity mob = EntityArgument.getEntity(ctx, "mob");
        if (!RomanceableMobs.isSupportedType(mob)) {
            throw error("This mob is not supported yet (villager, wolf, cat, fox).");
        }
        if (!RomanceableMobs.isAdult(mob)) {
            throw error("Only adult mobs can have relationships.");
        }
        return mob;
    }

    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Entity mob = resolveMob(ctx);
        int value = RelationshipData.get(ctx.getSource().getServer()).get(mob.getUUID(), player.getUUID());
        report(ctx, mob, value);
        return value;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Entity mob = resolveMob(ctx);
        int requested = IntegerArgumentType.getInteger(ctx, "value");
        int value = RelationshipData.get(ctx.getSource().getServer()).set(mob.getUUID(), player.getUUID(), requested);
        report(ctx, mob, value);
        return value;
    }

    private static int add(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Entity mob = resolveMob(ctx);
        int delta = IntegerArgumentType.getInteger(ctx, "amount");
        int value = RelationshipData.get(ctx.getSource().getServer()).add(mob.getUUID(), player.getUUID(), delta);
        report(ctx, mob, value);
        return value;
    }

    private static void report(CommandContext<CommandSourceStack> ctx, Entity mob, int value) {
        RelationshipStage stage = RelationshipStage.forAffinity(value);
        ctx.getSource().sendSuccess(() -> Component.literal(
                mob.getName().getString() + ": affinity " + value + "/" + RelationshipStage.MAX_AFFINITY
                        + " (" + stage.name() + ")"), false);
    }
}
