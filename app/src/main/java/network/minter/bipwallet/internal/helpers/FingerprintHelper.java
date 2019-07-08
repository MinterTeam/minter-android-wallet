package network.minter.bipwallet.internal.helpers;

import android.content.Context;

import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public final class FingerprintHelper {

    private Context mContext;
    private FingerprintManagerCompat mFingerprintManagerCompat;

    public FingerprintHelper(Context context) {
        mContext = context;
        mFingerprintManagerCompat = FingerprintManagerCompat.from(mContext);
    }

    public boolean isHardwareDetected() {
        return mFingerprintManagerCompat.isHardwareDetected();
    }

    public boolean hasEnrolledFingerprints() {
        return mFingerprintManagerCompat.hasEnrolledFingerprints();
    }
}
