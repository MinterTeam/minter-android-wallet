package network.minter.bipwallet.internal.common;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;

import static java.lang.String.format;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class Preconditions {
    /**
     * Ensures that an object reference passed as a parameter to the calling
     * method is not null.
     *
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    @NonNull
    public static <T> T checkNotNull(final T reference, final String message) {
        if (reference == null) {
            throw new NullPointerException(message);
        }
        return reference;
    }

    @NonNull
    public static <T> T checkNotNull(final T reference) {
        return checkNotNull(reference, null);
    }

    @NonNull
    public static <T> Collection<T> checkNotEmpty(final Collection<T> collection) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException();
        }

        return collection;
    }

    public static <T> Collection<T> checkNotEmpty(final Collection<T> collection, String message) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return collection;
    }

    @SafeVarargs
    @NonNull
    public static <T> T firstNonNull(final T ref0, final T... refs) {
        if (ref0 != null) {
            return ref0;
        }

        T outRef = null;
        for (T ref : refs) {
            if (ref != null) {
                outRef = ref;
                break;
            }
        }

        return checkNotNull(outRef);
    }

    @NonNull
    public static String firstNonEmpty(final String ref0, final String... refs) {
        if (ref0 != null && !ref0.isEmpty()) {
            return ref0;
        }

        String outRef = null;
        for (String ref : refs) {
            if (ref != null && !ref.isEmpty()) {
                outRef = ref;
                break;
            }
        }

        return checkNotNull(outRef);
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression   a boolean expression
     * @param errorMessage the exception message to use if the check fails; will be converted to a
     *                     string using {@link String#valueOf(Object)}
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression, @Nullable Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression           a boolean expression
     * @param errorMessageTemplate a template for the exception message should the check fail. The
     *                             message is formed by replacing each {@code %s} placeholder in the
     *                             template with an argument. These are matched by position - the
     *                             sw {@code %s} gets {@code errorMessageArgs[0]}, etc. Unmatched
     *                             arguments will be appended to the formatted message in square
     *                             braces. Unmatched placeholders will be left as-is.
     * @param errorMessageArgs     the arguments to be substituted into the message template.
     *                             Arguments are converted to strings using {@link
     *                             String#valueOf(Object)}.
     * @throws IllegalArgumentException if {@code expression} is false
     * @throws NullPointerException     if the check fails and either {@code errorMessageTemplate}
     *                                  or {@code errorMessageArgs} is null (don't let this happen)
     */
    public static void checkArgument(
            boolean expression,
            @Nullable String errorMessageTemplate,
            @Nullable Object... errorMessageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArgs));
        }
    }
}
