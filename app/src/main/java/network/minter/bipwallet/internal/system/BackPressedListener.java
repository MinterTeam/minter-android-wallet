package network.minter.bipwallet.internal.system;

/**
 * Wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public interface BackPressedListener {
    /**
     * @return False to stop delegating (onBackPressed() will returns immediately without calling
     * super.onBackPressed() after false detected), true - do nothing
     */
    boolean onBackPressed();
}
