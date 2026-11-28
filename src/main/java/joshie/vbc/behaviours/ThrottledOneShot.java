package joshie.vbc.behaviours;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ThrottledOneShot<E extends LivingEntity> extends OneShot<E> {

    private final OneShot<E> delegate;
    private final Predicate<E> throttled;
    private final int interval;
    private boolean hasStarted;
    private long lastStart;

    public ThrottledOneShot(OneShot<E> delegate, Predicate<E> throttled, int interval) {
        this.delegate = delegate;
        this.throttled = throttled;
        this.interval = interval;
    }

    public boolean trigger(ServerLevel level, E entity, long gameTime) {
        if (this.interval > 0 && this.throttled.test(entity)
                && this.hasStarted && gameTime - this.lastStart < this.interval) {
            return false;
        }
        if (!this.delegate.trigger(level, entity, gameTime)) {
            return false;
        }
        this.hasStarted = true;
        this.lastStart = gameTime;
        return true;
    }

    public String debugString() {
        return "throttled[" + this.delegate.debugString() + "]";
    }

    public static boolean isIdleAdult(Villager villager) {
        return !villager.isBaby()
                && villager.getBrain().getActiveNonCoreActivity()
                        .orElse(null) == net.minecraft.world.entity.schedule.Activity.IDLE;
    }
}
