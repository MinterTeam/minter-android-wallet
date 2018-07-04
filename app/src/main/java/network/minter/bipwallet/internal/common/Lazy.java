package network.minter.bipwallet.internal.common;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public interface Lazy<T> {

    /**
     * Retrieves an instance of the appropriate type. The returned object may or may not be a new
     * instance, depending on the implementation.
     *
     * @return an instance of the appropriate type
     */
    T get();
}
