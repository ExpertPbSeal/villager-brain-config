package joshie.vbc.behaviours;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.OneShot;

import java.util.function.Predicate;

public final class VetoedOneShot<E extends LivingEntity> extends OneShot<E> {

    private final OneShot<E> delegate;
    private final Predicate<E> veto;

    public VetoedOneShot(OneShot<E> delegate, Predicate<E> veto) {
        this.delegate = delegate;
        this.veto = veto;
    }

    @Override
    public boolean trigger(ServerLevel level, E entity, long gameTime) {
        if (this.veto.test(entity)) {
            return false;
        }
        return this.delegate.trigger(level, entity, gameTime);
    }

    @Override
    public String debugString() {
        return "vetoed[" + this.delegate.debugString() + "]";
    }
}
