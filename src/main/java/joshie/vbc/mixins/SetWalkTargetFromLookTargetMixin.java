package joshie.vbc.mixins;

import joshie.vbc.behaviours.ThrottledOneShot;
import joshie.vbc.config.ConfigManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Optionally applies the same idle walk target interval to the look target behaviour.
 * <p>
 * Disabled by default: this behaviour is what makes a villager walk over to a player or a cat, so
 * throttling it changes how lively a village feels.
 */
@Mixin(SetWalkTargetFromLookTarget.class)
public class SetWalkTargetFromLookTargetMixin {

    @Inject(method = "create(FI)Lnet/minecraft/world/entity/ai/behavior/OneShot;", at = @At("RETURN"), cancellable = true)
    private static void vbc$throttleLookTarget(float speed, int closeEnough,
                                               CallbackInfoReturnable<OneShot<LivingEntity>> cir) {
        vbc$throttle(cir);
    }

    @Inject(method = "create(Ljava/util/function/Predicate;Ljava/util/function/Function;I)Lnet/minecraft/world/entity/ai/behavior/OneShot;",
            at = @At("RETURN"), cancellable = true)
    private static void vbc$throttleLookTargetCustom(Predicate<LivingEntity> canStroll, Function<LivingEntity, Float> speed,
                                                     int closeEnough, CallbackInfoReturnable<OneShot<LivingEntity>> cir) {
        vbc$throttle(cir);
    }

    private static void vbc$throttle(CallbackInfoReturnable<OneShot<LivingEntity>> cir) {
        int interval = ConfigManager.getIdleWalkTargetInterval();
        OneShot<LivingEntity> original = cir.getReturnValue();
        if (!ConfigManager.idleIntervalCoversLookTarget() || interval <= 0 || original == null) {
            return;
        }
        cir.setReturnValue(new ThrottledOneShot<LivingEntity>(original,
                entity -> entity instanceof Villager villager && ThrottledOneShot.isIdleAdult(villager), interval));
    }
}
