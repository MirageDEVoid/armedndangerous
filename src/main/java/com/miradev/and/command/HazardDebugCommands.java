package com.miradev.and.command;

import com.miradev.and.Reference;
import com.miradev.and.common.HazardZoneManager;
import com.miradev.and.common.HazardZoneType;
import com.miradev.and.entity.HazardZoneEntity;
import com.miradev.and.registry.HazardZoneRegistry;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class HazardDebugCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnhazard")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("id", ResourceLocationArgument.id())
                        .executes(ctx -> spawnHazard(ctx.getSource(), ResourceLocationArgument.getId(ctx, "id")))
                )
        );

        dispatcher.register(Commands.literal("ignitehazard")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> igniteHazard(ctx.getSource()))
        );
    }

    private static int spawnHazard(CommandSourceStack source, ResourceLocation id) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Must be run by a player."));
            return 0;
        }

        HazardZoneType type = HazardZoneRegistry.get(id);
        if (type == null) {
            source.sendFailure(Component.literal("No HazardZoneType found for '" + id + "' — check extra json."));
            return 0;
        }

        HazardZoneEntity zone = HazardZoneEntity.tryCreate(
                player.level(), player.getX(), player.getY(), player.getZ(), type, player
        );

        if (zone == null) {
            source.sendFailure(Component.literal("No solid ground found within range, spawn cancelled."));
            return 0;
        }

        player.level().addFreshEntity(zone);
        HazardZoneManager.register(zone);

        source.sendSuccess(() -> Component.literal("Spawned hazard zone '" + id + "' at your feet."), false);
        return 1;
    }

    private static int igniteHazard(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Must be run by a player."));
            return 0;
        }

        HazardZoneManager.igniteOwned(player, null);
        source.sendSuccess(() -> Component.literal("Ignited all owned hazard zones."), false);
        return 1;
    }
}