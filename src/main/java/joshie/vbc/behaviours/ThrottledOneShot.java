package joshie.vbc.behaviours;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

/**
 * Wraps a vanilla {@link OneShot} and enforces a minimum interval between two successful triggers,
 * but only for the entities the predicate selects.
 * <p>
 * The predicate means "this entity is throttled", not "this entity is blocked". Entities outside the
 * predicate run the wrapped behaviour without any interval, which keeps panic, raid and play
 * packages untouched while the same factory is reused by several villager packages.
 */
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

    @Override
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

    @Override
    public String debugString() {
        return "throttled[" + this.delegate.debugString() + "]";
    }

    /**
     * True for an adult villager that is currently running the idle package, which is the only case
     * the idle stroll interval should apply to.
     */
    public static boolean isIdleAdult(Villager villager) {
        return !villager.isBaby()
                && villager.getBrain().getActiveNonCoreActivity()
                        .orElse(null) == net.minecraft.world.entity.schedule.Activity.IDLE;
    }
}
