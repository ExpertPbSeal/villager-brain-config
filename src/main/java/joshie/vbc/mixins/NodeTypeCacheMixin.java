package joshie.vbc.mixins;

import joshie.vbc.config.VillagerGlobalTickCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(WalkNodeEvaluator.class)
public class NodeTypeCacheMixin {

    private long vbc$signature() {
        WalkNodeEvaluator evaluator = (WalkNodeEvaluator) (Object) this;
        return VillagerGlobalTickCache.nodeTypeSignature(evaluator.mob,
                evaluator.canOpenDoors(), evaluator.canPassDoors());
    }

    @Inject(method = "getCachedPathType", at = @At("HEAD"), cancellable = true)
    private void vbc$readCachedPathType(int x, int y, int z, CallbackInfoReturnable<PathType> cir) {
        PathType cached = VillagerGlobalTickCache.getCachedNodeType(vbc$signature(), BlockPos.asLong(x, y, z));
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "getCachedPathType", at = @At("RETURN"))
    private void vbc$writeCachedPathType(int x, int y, int z, CallbackInfoReturnable<PathType> cir) {
        VillagerGlobalTickCache.cacheNodeType(vbc$signature(), BlockPos.asLong(x, y, z), cir.getReturnValue());
    }
}