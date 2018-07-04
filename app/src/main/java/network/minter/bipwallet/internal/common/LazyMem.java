package network.minter.bipwallet.internal.common;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;


/**
 * Useful suppliers.
 * <p>
 * <p>All methods return serializable suppliers as long as they're given serializable parameters.
 *
 * @author Laurence Gonsalves
 * @author Harry Heymann
 * @since 2.0
 */
public class LazyMem {

    private enum SupplierFunctionImpl implements SupplierFunction<Object> {
        INSTANCE;

        // Note: This makes T a "pass-through type"
        @Override
        public Object apply(Lazy<Object> input) {
            return input.get();
        }

        @Override
        public String toString() {
            return "LazyMem.supplierFunction()";
        }
    }

    private LazyMem() {
    }

    /**
     * Returns a new supplier which is the composition of the provided function and supplier. In
     * other words, the new supplier's value will be computed by retrieving the value from {@code
     * supplier}, and then applying {@code function} to that value. Note that the resulting supplier
     * will not call {@code supplier} or invoke {@code function} until it is called.
     */
    public static <F, T> Lazy<T> compose(LazyFunction<? super F, T> lazyFunction, Lazy<F> supplier) {
        Preconditions.checkNotNull(lazyFunction);
        Preconditions.checkNotNull(supplier);
        return new SupplierComposition<>(lazyFunction, supplier);
    }

    /**
     * Returns a supplier which caches the instance retrieved during the sw call to {@code get()}
     * and returns that value on subsequent calls to {@code get()}. See: <a
     * href="http://en.wikipedia.org/wiki/Memoization">memoization</a> <p> <p>The returned supplier
     * is thread-safe. The delegate's {@code get()} method will be invoked at most once. The
     * supplier's serialized form does not contain the cached value, which will be recalculated when
     * {@code get()} is called on the reserialized instance. <p> <p>If {@code delegate} is an
     * instance created by an earlier call to {@code memoize}, it is returned directly.
     */
    public static <T> Lazy<T> memoize(Lazy<T> delegate) {
        return (delegate instanceof MemoizingSupplier)
                ? delegate
                : new MemoizingSupplier<>(Preconditions.checkNotNull(delegate));
    }

    /**
     * Returns a supplier that caches the instance supplied by the delegate and removes the cached
     * value after the specified time has passed. Subsequent calls to {@code get()} return the
     * cached value if the expiration time has not passed. After the expiration time, a new value is
     * retrieved, cached, and returned. See: <a href="http://en.wikipedia.org/wiki/Memoization">memoization</a>
     * <p> <p>The returned supplier is thread-safe. The supplier's serialized form does not contain
     * the cached value, which will be recalculated when {@code get()} is called on the reserialized
     * instance.
     *
     * @param duration the length of time after a value is created that it should stop being
     *                 returned by subsequent {@code get()} calls
     * @param unit     the unit that {@code duration} is expressed in
     * @throws IllegalArgumentException if {@code duration} is not positive
     * @since 2.0
     */
    public static <T> Lazy<T> memoizeWithExpiration(
            Lazy<T> delegate, long duration, TimeUnit unit) {
        return new ExpiringMemoizingSupplier<>(delegate, duration, unit);
    }

    /**
     * Returns a supplier that always supplies {@code instance}.
     */
    public static <T> Lazy<T> ofInstance(@Nullable T instance) {
        return new SupplierOfInstance<>(instance);
    }

    /**
     * Returns a supplier whose {@code get()} method synchronizes on {@code delegate} before calling
     * it, making it thread-safe.
     */
    public static <T> Lazy<T> synchronizedSupplier(Lazy<T> delegate) {
        return new ThreadSafeSupplier<>(Preconditions.checkNotNull(delegate));
    }

    /**
     * Returns a function that accepts a supplier and returns the result of invoking {@link
     * Lazy#get} on that supplier.
     * <p>
     * <p><b>Java 8 users:</b> use the method reference {@code Lazy::get} instead.
     *
     * @since 8.0
     */
    public static <T> LazyFunction<Lazy<T>, T> supplierFunction() {
        @SuppressWarnings("unchecked") // implementation is "fully variant"
                SupplierFunction<T> sf = (SupplierFunction<T>) SupplierFunctionImpl.INSTANCE;
        return sf;
    }

    private interface SupplierFunction<T> extends LazyFunction<Lazy<T>, T> {
    }

    private static class SupplierComposition<F, T> implements Lazy<T>, Serializable {
        private static final long serialVersionUID = 0;
        final LazyFunction<? super F, T> lazyFunction;
        final Lazy<F> supplier;

        SupplierComposition(LazyFunction<? super F, T> lazyFunction, Lazy<F> supplier) {
            this.lazyFunction = lazyFunction;
            this.supplier = supplier;
        }

        @Override
        public T get() {
            return lazyFunction.apply(supplier.get());
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof SupplierComposition) {
                SupplierComposition<?, ?> that = (SupplierComposition<?, ?>) obj;
                return lazyFunction.equals(that.lazyFunction) && supplier.equals(that.supplier);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return lazyFunction.hashCode() / 2 + supplier.hashCode() / 2;
        }

        @Override
        public String toString() {
            return "LazyMem.compose(" + lazyFunction + ", " + supplier + ")";
        }
    }

    @VisibleForTesting
    static class MemoizingSupplier<T> implements Lazy<T>, Serializable {
        private static final long serialVersionUID = 0;
        final Lazy<T> delegate;
        transient volatile boolean initialized;
        // "value" does not need to be volatile; visibility piggy-backs
        // on volatile read of "initialized".
        transient T value;

        MemoizingSupplier(Lazy<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            // A 2-field variant of Double Checked Locking.
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        T t = delegate.get();
                        value = t;
                        initialized = true;
                        return t;
                    }
                }
            }
            return value;
        }

        @Override
        public String toString() {
            return "LazyMem.memoize(" + delegate + ")";
        }
    }

    @VisibleForTesting
    static class ExpiringMemoizingSupplier<T> implements Lazy<T>, Serializable {
        private static final long serialVersionUID = 0;
        final Lazy<T> delegate;
        final long durationNanos;
        transient volatile T value;
        // The special value 0 means "not yet initialized".
        transient volatile long expirationNanos;

        ExpiringMemoizingSupplier(Lazy<T> delegate, long duration, TimeUnit unit) {
            this.delegate = Preconditions.checkNotNull(delegate);
            this.durationNanos = unit.toNanos(duration);
            Preconditions.checkArgument(duration > 0);
        }

        @Override
        public T get() {
            // Another variant of Double Checked Locking.
            //
            // We use two volatile reads. We could reduce this to one by
            // putting our fields into a holder class, but (at least on x86)
            // the extra memory consumption and indirection are more
            // expensive than the extra volatile reads.
            long nanos = expirationNanos;
            long now = System.nanoTime();
            if (nanos == 0 || now - nanos >= 0) {
                synchronized (this) {
                    if (nanos == expirationNanos) { // recheck for lost race
                        T t = delegate.get();
                        value = t;
                        nanos = now + durationNanos;
                        // In the very unlikely event that nanos is 0, set it to 1;
                        // no one will notice 1 ns of tardiness.
                        expirationNanos = (nanos == 0) ? 1 : nanos;
                        return t;
                    }
                }
            }
            return value;
        }

        @Override
        public String toString() {
            // This is a little strange if the unit the data provided was not NANOS,
            // but we don't want to store the unit just for toString
            return "LazyMem.memoizeWithExpiration(" + delegate + ", " + durationNanos + ", NANOS)";
        }
    }

    private static class SupplierOfInstance<T> implements Lazy<T>, Serializable {
        private static final long serialVersionUID = 0;
        final T instance;

        SupplierOfInstance(@Nullable T instance) {
            this.instance = instance;
        }

        @Override
        public T get() {
            return instance;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof SupplierOfInstance) {
                SupplierOfInstance<?> that = (SupplierOfInstance<?>) obj;
                return instance != null && instance.equals(that.instance);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return instance != null ? instance.hashCode() : hashCode();
        }

        @Override
        public String toString() {
            return "LazyMem.ofInstance(" + instance + ")";
        }
    }

    private static class ThreadSafeSupplier<T> implements Lazy<T>, Serializable {
        private static final long serialVersionUID = 0;
        final Lazy<T> delegate;

        ThreadSafeSupplier(Lazy<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            synchronized (delegate) {
                return delegate.get();
            }
        }

        @Override
        public String toString() {
            return "LazyMem.synchronizedSupplier(" + delegate + ")";
        }
    }
}
