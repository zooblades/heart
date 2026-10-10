package com.heartbound.command;

import com.heartbound.date.DateManager;
import com.heartbound.date.DateRules.DateType;
import com.heartbound.interaction.InteractionHandler;
import com.heartbound.relationship.RelationshipData;
import com.heartbound.relationship.RelationshipStage;
import com.heartbound.relationship.RomanceableMobs;
import com.heartbound.talk.PartnerTalk;
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
import net.minecraft.world.entity.Mob;

/**
 * Debug commands (operators only):
 * <pre>{@code
 * /heartbound date <mob> walk|place|home   (starts a date with your partner now, for testing)
 * /heartbound affinity get <mob>
 * /heartbound affinity set <mob> <value>
 * /heartbound affinity add <mob> <amount>
 * }</pre>
 *
 * The affinity is always between the targeted mob and the player who runs the command.
 */
public final class HeartboundCommands {

    private HeartboundCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        int max = RelationshipStage.MAX_AFFINITY;
        dispatcher.register(Commands.literal("heartbound")
                .then(Commands.literal("breakup").executes(HeartboundCommands::breakUp))
                .then(Commands.literal("reply")
                        .then(Commands.argument("token", IntegerArgumentType.integer())
                                .then(Commands.argument("option", IntegerArgumentType.integer(0, 2))
                                        .executes(HeartboundCommands::reply))))
                .then(Commands.literal("date").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("mob", EntityArgument.entity())
                                .then(Commands.literal("walk")
                                        .executes(ctx -> startDate(ctx, DateType.WALK)))
                                .then(Commands.literal("place")
                                        .executes(ctx -> startDate(ctx, DateType.PLACE)))
                                .then(Commands.literal("home")
                                        .executes(ctx -> startDate(ctx, DateType.HOME)))))
                .then(Commands.literal("affinity").requires(source -> source.hasPermission(2))
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

    /** For testing: starts a date with your partner right now, without waiting for an invitation. */
    private static int startDate(CommandContext<CommandSourceStack> ctx, DateType type) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Entity mob = resolveMob(ctx);
        RelationshipData data = RelationshipData.get(ctx.getSource().getServer());
        if (!(mob instanceof Mob partner) || !data.isPartner(mob.getUUID(), player.getUUID())) {
            throw error("Only your partner can go on a date with you.");
        }
        if (!DateManager.start(player.serverLevel(), player, partner, type, data)) {
            throw error("This date cannot start: no home point is set, no place was found, or a date is already going.");
        }
        return 1;
    }

    private static int breakUp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        return InteractionHandler.breakUp(player) ? 1 : 0;
    }

    private static int reply(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartnerTalk.reply(player, IntegerArgumentType.getInteger(ctx, "token"), IntegerArgumentType.getInteger(ctx, "option"));
        return 1;
    }

    private static Entity resolveMob(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity mob = EntityArgument.getEntity(ctx, "mob");
        if (!RomanceableMobs.isSupportedType(mob)) {
            throw error("This mob is not supported yet (villager, wolf, cat, fox, piglin).");
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
