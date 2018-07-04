package network.minter.bipwallet.internal.mvp;

/**
 * Atlas_Android. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public interface ErrorView {
    void onError(Throwable t);
    void onError(String err);
}
