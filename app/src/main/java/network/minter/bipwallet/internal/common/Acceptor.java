package network.minter.bipwallet.internal.common;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public interface Acceptor<T> {
    void accept(T t);
}
