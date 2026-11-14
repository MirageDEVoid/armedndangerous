package com.miradev.and.fragment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.entity.projectile.ProjectileEntity;
import ttv.migami.jeg.init.ModBlocks;
import ttv.migami.jeg.init.ModDamageTypes;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModTags;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// mirror targeting and damage rule of ProjectileEntity so fragment and bullet behave the same
// written against jeg as of 0.14.x, marked deviations

public final class FragmentRules {
    private FragmentRules() {}

    // ProjectileEntity.PROJECTILE_TARGETS
    public static final Predicate<Entity> TARGETS =
            e -> e != null && e.isPickable() && !e.isSpectator();

    // ProjectileEntity#getAdvantage
    public static ResourceLocation effectiveAdvantage(@Nullable ResourceLocation advantage) {
        if (advantage == null || !Config.COMMON.gameplay.gunAdvantage.get()) {
            return ModTags.Entities.NONE.location();
        }
        return advantage;
    }

    public static boolean isUndead(Entity e) {
        return e instanceof LivingEntity le
                && (le.getMobType().equals(MobType.UNDEAD) || e.getType().is(ModTags.Entities.UNDEAD));
    }

    // (DEVIATION) multiplier, jeg sets the target on fire inside this method, see advantageSideEffect
    public static float advantageMultiplier(Entity target, ResourceLocation advantage) {
        float m = 1F;
        if (advantage.equals(ModTags.Entities.NONE.location())) return m;

        ResourceLocation heavy = ModTags.Entities.HEAVY.location();
        ResourceLocation veryHeavy = ModTags.Entities.VERY_HEAVY.location();

        if (target.getType().is(ModTags.Entities.HEAVY)) {
            m = (advantage.equals(heavy) || advantage.equals(veryHeavy)) ? 1.25F : 0.50F;
        } else if (target.getType().is(ModTags.Entities.VERY_HEAVY)) {
            if (advantage.equals(veryHeavy)) m = 1.0F;
            else if (advantage.equals(heavy)) m = 0.5F;
            else m = 0.25F;
        }

        if (advantage.equals(ModTags.Entities.UNDEAD.location())) {   // overrides, like jeg
            m = (isUndead(target) || target.getType().is(ModTags.Entities.GHOST)) ? 1.25F : 0.75F;
        }
        return m;
    }

    // call once, only when damage is applied
    public static void advantageSideEffect(Entity target, ResourceLocation advantage) {
        if (advantage.equals(ModTags.Entities.UNDEAD.location())
                && (isUndead(target) || target.getType().is(ModTags.Entities.GHOST))) {
            target.setSecondsOnFire(2);
        }
    }

    // ProjectileEntity#ignoreEntity, null safe for a missing owner
    // call once per candidate entity per burst, cache the result
    // same type rule is random, rolling per ray makes failure a guarantee

    // (DEVIATION) Holy Shotgun check, Specter Round exception are gun specific, removed
    // (DEVIATION) owner is NOT ignored here, config dictates self damage

    public static boolean ignores(Entity target, @Nullable Entity owner, ResourceLocation advantage) {
        if (target instanceof LivingEntity le) {
            if (le.isDeadOrDying()) return true;
            if (le.hasEffect(ModEffects.BULLET_PROTECTION.get())) return true;
            if (owner instanceof Player && le.hasEffect(ModEffects.PLAYER_BULLET_PROTECTION.get())) return true;
        }

        if (target.getType().is(ModTags.Entities.GHOST)
                && !advantage.equals(ModTags.Entities.UNDEAD.location())) {
            return true;
        }

        if (owner != null && owner != target) {

            if (target.hasPassenger(owner)) return true;
            if (owner instanceof Player p && target.hasIndirectPassenger(p)) return true;

            boolean ownerPlayerOwned = owner.getTags().contains("PlayerOwned");
            boolean targetPlayerOwned = target.getTags().contains("PlayerOwned");

            if (owner.getTags().contains("GunnerPatroller") && target.getTags().contains("GunnerPatroller")) return true;
            if (owner instanceof Player && targetPlayerOwned) return true;
            if (ownerPlayerOwned && targetPlayerOwned) return true;
            if (ownerPlayerOwned && !(target instanceof Enemy) && !(target instanceof Player)) return true;

            return owner instanceof net.minecraft.world.entity.PathfinderMob mob
                    && mob.getType() == target.getType()
                    && target.level().random.nextFloat() < 0.9F;
        }
        return false;
    }

    // (DEVIATION) canHarmPlayer check, skipped for self hit, pvp off doesn't affect this
    public static boolean pvpAllows(Entity target, @Nullable Entity owner) {
        if (target == owner) return true;
        return !(target instanceof Player victim && owner instanceof Player attacker && !attacker.canHarmPlayer(victim));
    }

    // (DEVIATION) bullet dmg & enderman, SonicProjectileEntity exception doesn't apply to frag
    public static DamageSource damageSource(Level level, @Nullable Entity direct, @Nullable LivingEntity owner, Entity target) {
        if (target instanceof EnderMan) {
            return level.damageSources().mobProjectile(direct, owner);
        }
        return new ModDamageTypes.Sources.BulletDamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageTypes.BULLET),
                direct, owner);
    }

    // only pass through leaves, only when jeg ignoreLeaves config is on
    public static boolean passesThrough(BlockState state) {
        return Config.COMMON.gameplay.ignoreLeaves.get() && state.getBlock() instanceof LeavesBlock;
    }

    // collider raycast, read without entity collision context
    public static BlockHitResult clip(Level level, Vec3 from, Vec3 to) {
        return BlockGetter.traverseBlocks(from, to, level, (lvl, pos) -> {
            BlockState state = lvl.getBlockState(pos);
            if (passesThrough(state)) return null;
            VoxelShape shape = state.getCollisionShape(lvl, pos);
            return lvl.clipWithInteractionOverride(from, to, pos, shape, state);
        }, lvl -> BlockHitResult.miss(to,
                Direction.getNearest(from.x - to.x, from.y - to.y, from.z - to.z),
                BlockPos.containing(to)));
    }

    // fragile block, rolled per frag, the more frag hits the more likely it is to break, only for player owned frag
    // mobGriefing gamerule handle mob owned frag
    public static boolean tryBreakFragile(ServerLevel level, BlockPos pos, @Nullable Entity owner, double weight) {
        if (!(owner instanceof Player)) return false;
        if (!Config.COMMON.gameplay.griefing.enableGlassBreaking.get()) return false;

        BlockState state = level.getBlockState(pos);
        if (!state.is(ModTags.Blocks.FRAGILE)) return false;

        float destroySpeed = state.getDestroySpeed(level, pos);
        if (destroySpeed < 0) return false;

        double c = Math.min(1.0, Config.COMMON.gameplay.griefing.fragileBaseBreakChance.get() / (destroySpeed + 1));
        double chance = 1.0 - Math.pow(1.0 - c, weight);
        if (level.random.nextDouble() >= chance) return false;

        if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            Vec3 center = pos.getCenter();
            for (ItemStack drop : specialDrops(state, level.random)) {
                level.addFreshEntity(new ItemEntity(level, center.x, center.y, center.z, drop));
            }
        }

        boolean customDropsOnly = state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN);
        level.destroyBlock(pos, !customDropsOnly && Config.COMMON.gameplay.griefing.fragileBlockDrops.get());

        if (state.is(Blocks.ICE)) meltIce(level, pos);
        return true;
    }

    private static List<ItemStack> specialDrops(BlockState s, RandomSource r) {
        List<ItemStack> out = new ArrayList<>();
        if (s.is(Blocks.MELON)) {
            out.add(new ItemStack(Items.MELON_SLICE, r.nextInt(6) + 1));
            out.add(new ItemStack(Items.MELON_SEEDS, r.nextInt(2) + 1));
        }
        if (s.is(Blocks.CARVED_PUMPKIN) || s.is(Blocks.JACK_O_LANTERN)) {
            out.add(new ItemStack(Items.PUMPKIN_SEEDS, r.nextInt(6) + 1));
        }
        if (s.is(Blocks.PUMPKIN)) {
            out.add(new ItemStack(Items.PUMPKIN_SEEDS, r.nextInt(2) + 1));
            out.add(new ItemStack(Items.CARVED_PUMPKIN, 1));
        }
        if (s.is(Blocks.COCOA)) out.add(new ItemStack(Items.COCOA_BEANS, r.nextInt(3) + 1));
        if (s.is(Blocks.BEEHIVE) || s.is(Blocks.BEE_NEST)) out.add(new ItemStack(Items.HONEYCOMB, r.nextInt(3) + 1));
        if (s.is(ModBlocks.BOOHIVE.get()) || s.is(ModBlocks.BOO_NEST.get())) {
            out.add(new ItemStack(ModItems.ECTOPLASM.get(), r.nextInt(5) + 1));
        }
        return out;
    }

    private static void meltIce(ServerLevel level, BlockPos pos) {
        boolean water = level.getBlockState(pos.below()).is(Blocks.WATER);
        if (!water) {
            int ice = 0;
            for (Direction d : Direction.values()) {
                if (level.getBlockState(pos.relative(d)).is(Blocks.ICE)) ice++;
            }
            water = ice > 2;
        }
        // (DEVIATION) jeg flags 0(never do client sync), made this 3, notify and update client
        if (water) level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
    }
}