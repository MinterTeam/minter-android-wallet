package network.minter.bipwallet.internal.system;

/**
 * Wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public interface BackPressedDelegate {
    void addBackPressedListener(BackPressedListener listener);
    void removeBackPressedListener(BackPressedListener listener);
    void clearBackPressedListeners();
}
