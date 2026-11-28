package joshie.vbc.mixins;

import joshie.vbc.behaviours.VetoedOneShot;
import joshie.vbc.config.ConfigManager;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(StrollAroundPoi.class)
public class StrollAroundPoiMixin {

    @Inject(method = "create", at = @At("RETURN"), cancellable = true)
    private static void vbc$vetoJobSiteStroll(MemoryModuleType<GlobalPos> memory, float speed, int radius,
                                              CallbackInfoReturnable<OneShot<PathfinderMob>> cir) {
        if (memory != MemoryModuleType.JOB_SITE || !ConfigManager.isWorkStrollBanned()) {
            return;
        }
        OneShot<PathfinderMob> original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        cir.setReturnValue(new VetoedOneShot<PathfinderMob>(original, mob -> mob instanceof Villager));
    }
}
