package network.minter.bipwallet.internal.common;

import android.support.annotation.Nullable;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public interface LazyFunction<F, T> {
    /**
     * Returns the result of applying this function to {@code input}. This method is <i>generally
     * expected</i>, but not absolutely required, to have the following properties:
     * <p>
     * <ul>
     * <li>Its execution does not cause any observable side effects.
     * <li>The computation is <i>consistent with equals</i>; that is, {@link Objects#equal
     * Objects.equal}{@code (a, b)} implies that {@code Objects.equal(function.apply(a),
     * function.apply(b))}.
     * </ul>
     *
     * @throws NullPointerException if {@code input} is null and this function does not accept null
     *                              arguments
     */
    @Nullable
    T apply(@Nullable F input);

    /**
     * <i>May</i> return {@code true} if {@object} is a {@code Function} that behaves identically to
     * this function.
     * <p>
     * <p><b>Warning: do not depend</b> on the behavior of this method.
     * <p>
     * <p>Historically, {@code Function} instances in this library have implemented this method to
     * recognize certain cases where distinct {@code Function} instances would in fact behave
     * identically. However, as code migrates to {@code java.util.function}, that behavior will
     * disappear. It is best not to depend on it.
     */
    @Override
    boolean equals(@Nullable Object object);
}
