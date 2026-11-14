package com.miradev.and.command;

import com.miradev.and.Reference;
import com.miradev.and.common.ResidueProfile;
import com.miradev.and.entity.ResidueEntity;
import com.miradev.and.registry.ResidueRegistry;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ResidueCommands {
    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("spawnresidue")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(ResidueRegistry.ids(), b))
                        .executes(c -> spawn(c.getSource(), ResourceLocationArgument.getId(c, "id")))));
        d.register(Commands.literal("igniteresidue")
                .requires(s -> s.hasPermission(2))
                .executes(c -> ignite(c.getSource())));
    }

    private static int spawn(CommandSourceStack src, ResourceLocation id) {
        ServerPlayer player;
        try { player = src.getPlayerOrException(); }
        catch (Exception e) { src.sendFailure(Component.literal("Must be run by a player.")); return 0; }

        ResidueProfile type = ResidueRegistry.get(id);
        if (type == null) { src.sendFailure(Component.literal("No residue '" + id + "' - check the extra json.")); return 0; }

        ResidueEntity r = ResidueEntity.tryCreate(player.level(), player.getX(), player.getY(), player.getZ(), type, player);
        if (r == null) { src.sendFailure(Component.literal("No ground in range, or one is already here.")); return 0; }

        player.level().addFreshEntity(r);
        src.sendSuccess(() -> Component.literal("Spawned residue '" + id + "'."), false);
        return 1;
    }

    private static int ignite(CommandSourceStack src) {
        try {
            ResidueEntity.igniteOwned(src.getPlayerOrException(), null);
            src.sendSuccess(() -> Component.literal("Ignited your residues."), false);
            return 1;
        } catch (Exception e) { src.sendFailure(Component.literal("Must be run by a player.")); return 0; }
    }
}
