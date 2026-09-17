package com.miradev.and.animations;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.ClientUtils;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.animations.GunAnimations;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.common.FireMode;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.ReloadType;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.attachment.IAttachment;
import ttv.migami.jeg.util.GunEnchantmentHelper;

public final class ANDGunAnimations {
    private static String lastGunId = "";

    public static <T extends GeoAnimatable> AnimationController<GeoAnimatable> animationController(AnimatedGunItem animatable) {
        boolean[] inShootCycle = { false };
        boolean[] shootAnimAiming = { false };

        return new AnimationController<>(animatable, "Controller", 0, state -> {
            Player player = ClientUtils.getClientPlayer();
            ItemStack gunStack = player.getMainHandItem();

            if (gunStack.getOrCreateTag().contains("GunId")) {
                String currentId = gunStack.getOrCreateTag().getString("GunId");
                if (!currentId.equals(lastGunId)) {
                    lastGunId = currentId;
                    state.getController().forceAnimationReset();
                }
            }

            state.setControllerSpeed(1F);

            if (!state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE).firstPerson()) return state.setAndContinue(GunAnimations.IDLE);

            if (gunStack.getItem() != animatable)
                return state.setAndContinue(GunAnimations.IDLE);

            if (!(gunStack.getItem() instanceof AnimatedGunItem animatedGunItem))
                return state.setAndContinue(GunAnimations.IDLE);

            if (state.getController().getCurrentAnimation() == null)
                return state.setAndContinue(GunAnimations.IDLE);

            if (gunStack.getTag() != null) {
                CompoundTag tag = gunStack.getTag();

                boolean shootTagActive = ModSyncedDataKeys.SHOOTING.getValue(player) &&
                        animatedGunItem.getModifiedGun(gunStack).getGeneral().getFireMode().equals(FireMode.RELEASE_FIRE);

                if (shootTagActive) {
                    inShootCycle[0] = false;
                    return state.setAndContinue(GunAnimations.HOLD_FIRE);
                }

                boolean shootFlagActive = tag.getBoolean("IsShooting");
                boolean shootAnimPlaying = (GunAnimations.isAnimationPlaying(state.getController(), "shoot") ||
                        GunAnimations.isAnimationPlaying(state.getController(), "aim_shoot")) &&
                        !state.getController().getAnimationState().equals(AnimationController.State.PAUSED);

                boolean newShot = shootFlagActive && !inShootCycle[0];

                if (shootFlagActive || shootAnimPlaying) {
                    if (newShot) {
                        shootAnimAiming[0] = tag.getBoolean("IsAiming");
                        inShootCycle[0] = true;

                        state.getController().stop();
                        state.getController().forceAnimationReset();
                    }

                    state.setControllerSpeed(GunEnchantmentHelper.getReloadAnimationSpeed(gunStack));
                    return shootAnimAiming[0]
                            ? state.setAndContinue(GunAnimations.AIM_SHOOT)
                            : state.setAndContinue(GunAnimations.SHOOT);
                } else {
                    inShootCycle[0] = false;

                    if (tag.getBoolean("IsMeleeing") ||
                            (GunAnimations.isAnimationPlaying(state.getController(), "melee") ||
                                    GunAnimations.isAnimationPlaying(state.getController(), "bayonet")) &&
                                    !state.getController().getAnimationState().equals(AnimationController.State.PAUSED)) {

                        if ((Gun.getAttachment(IAttachment.Type.BARREL, player.getMainHandItem()).getItem() instanceof SwordItem)) {
                            return state.setAndContinue(GunAnimations.BAYONET);
                        } else {
                            return state.setAndContinue(GunAnimations.MELEE);
                        }
                    }

                    boolean lacksAmmo;
                    if (animatedGunItem.getModifiedGun(gunStack).getReloads().getReloadType() == ReloadType.SINGLE_ITEM) {
                        lacksAmmo = Gun.findAmmo(player, animatedGunItem.getModifiedGun(gunStack).getReloads().getReloadItem()).stack().isEmpty();
                    } else {
                        lacksAmmo = Gun.findAmmo(player, animatedGunItem.getModifiedGun(gunStack).getProjectile().getItem()).stack().isEmpty();
                    }

                    if (!lacksAmmo) {
                        if ((!ModSyncedDataKeys.RELOADING.getValue(player) &&
                                GunAnimations.isAnimationPlaying(state.getController(), "reload_loop")) ||
                                GunAnimations.isAnimationPlaying(state.getController(), "reload_stop")) {
                            state.setControllerSpeed(GunEnchantmentHelper.getReloadAnimationSpeed(gunStack));
                            return state.setAndContinue(GunAnimations.RELOAD_STOP);
                        }
                        if ((ModSyncedDataKeys.RELOADING.getValue(player) ||
                                ((GunAnimations.isAnimationPlaying(state.getController(), "reload") ||
                                        GunAnimations.isAnimationPlaying(state.getController(), "reload_alt") ||
                                        GunAnimations.isAnimationPlaying(state.getController(), "reload_start")) &&
                                        !state.getController().hasAnimationFinished()))) {
                            if (animatedGunItem.getModifiedGun(gunStack).getReloads().getReloadType().equals(ReloadType.MANUAL)) {
                                state.setControllerSpeed(GunEnchantmentHelper.getReloadAnimationSpeed(gunStack));
                                return state.setAndContinue(GunAnimations.RELOAD_START);
                            }
                            state.setControllerSpeed(GunEnchantmentHelper.getReloadAnimationSpeed(gunStack));
                            if (gunStack.is(ModItems.INFANTRY_RIFLE.get())) {
                                if (Gun.getAttachment(IAttachment.Type.MAGAZINE, gunStack).getItem() == ModItems.EXTENDED_MAG.get() ||
                                        Gun.getAttachment(IAttachment.Type.MAGAZINE, gunStack).getItem() == ModItems.DRUM_MAG.get()) {
                                    return state.setAndContinue(GunAnimations.RELOAD_ALT);
                                }
                            }
                            return state.setAndContinue(GunAnimations.RELOAD);
                        }
                    }

                    if (tag.getBoolean("IsInspecting") ||
                            (GunAnimations.isAnimationPlaying(state.getController(), "inspect") &&
                                    !state.getController().hasAnimationFinished())) {
                        if (tag.getBoolean("IsAiming")) {
                            return state.setAndContinue(GunAnimations.IDLE);
                        }
                        return state.setAndContinue(GunAnimations.INSPECT);
                    }

                    if (Config.COMMON.gameplay.drawAnimation.get() && (tag.getBoolean("IsDrawing") ||
                            (GunAnimations.isAnimationPlaying(state.getController(), "draw") &&
                                    !state.getController().hasAnimationFinished() && !AimingHandler.get().isAiming()))) {
                        state.setControllerSpeed(GunEnchantmentHelper.getReloadAnimationSpeed(gunStack));
                        return state.setAndContinue(GunAnimations.DRAW);
                    } else {
                        if (tag.getBoolean("IsRunning") && !tag.getBoolean("IsAiming")) {
                            if (!(Gun.getAttachment(IAttachment.Type.BARREL, player.getMainHandItem()).getItem() instanceof SwordItem)) {
                                return state.setAndContinue(GunAnimations.SPRINT);
                            }
                        }
                    }
                }
            }

            if (GunAnimations.isAnimationPlaying(state.getController(), "reload") && !state.getController().hasAnimationFinished())
                return state.setAndContinue(GunAnimations.RELOAD);
            state.setAndContinue(GunAnimations.IDLE);

            return PlayState.CONTINUE;
        });
    }
}