package network.minter.bipwallet.external.ui;

import com.airbnb.deeplinkdispatch.DeepLinkSpec;

/**
 * Minter. 2019
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@DeepLinkSpec(prefix = "minter://")
public @interface AppDeepLink {
    String[] value();
}
