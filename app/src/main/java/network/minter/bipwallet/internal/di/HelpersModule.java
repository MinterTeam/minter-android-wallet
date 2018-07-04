package network.minter.bipwallet.internal.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import network.minter.bipwallet.internal.helpers.DisplayHelper;
import network.minter.bipwallet.internal.helpers.ImageHelper;
import network.minter.bipwallet.internal.helpers.NetworkHelper;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public class HelpersModule {

    @Provides
    @WalletApp
    public DisplayHelper provideDisplayHelper(Context context) {
        return new DisplayHelper(context);
    }

    @Provides
    @WalletApp
    public NetworkHelper provideNetworkHelper(Context context) {
        return new NetworkHelper(context);
    }

    @Provides
    @WalletApp
    public ImageHelper provideImageHelper(Context context, DisplayHelper displayHelper) {
        return new ImageHelper(context, displayHelper);
    }
}
