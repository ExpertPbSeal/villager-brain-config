package joshie.vbc.mixins;

import joshie.vbc.behaviours.ThrottledOneShot;
import joshie.vbc.config.ConfigManager;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.VillageBoundRandomStroll;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(VillageBoundRandomStroll.class)
public class VillageBoundRandomStrollMixin {

    @Inject(method = "create(F)Lnet/minecraft/world/entity/ai/behavior/OneShot;", at = @At("RETURN"), cancellable = true)
    private static void vbc$throttleStroll(float speed, CallbackInfoReturnable<OneShot<PathfinderMob>> cir) {
        vbc$throttle(cir);
    }

    @Inject(method = "create(FII)Lnet/minecraft/world/entity/ai/behavior/OneShot;", at = @At("RETURN"), cancellable = true)
    private static void vbc$throttleStrollSized(float speed, int maxXZ, int maxY,
                                                CallbackInfoReturnable<OneShot<PathfinderMob>> cir) {
        vbc$throttle(cir);
    }

    private static void vbc$throttle(CallbackInfoReturnable<OneShot<PathfinderMob>> cir) {
        int interval = ConfigManager.getIdleWalkTargetInterval();
        OneShot<PathfinderMob> original = cir.getReturnValue();
        if (interval <= 0 || original == null) {
            return;
        }
        cir.setReturnValue(new ThrottledOneShot<PathfinderMob>(original,
                mob -> mob instanceof Villager villager && ThrottledOneShot.isIdleAdult(villager), interval));
    }
}
