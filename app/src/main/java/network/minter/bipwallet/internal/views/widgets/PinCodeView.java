package network.minter.bipwallet.internal.views.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import butterknife.BindView;
import butterknife.ButterKnife;
import network.minter.bipwallet.R;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class PinCodeView extends FrameLayout {

    @BindView(R.id.pin_key_container) ViewGroup pinContainer;
    @BindView(R.id.pin_indicator_container) ViewGroup pinIndicatorContainer;
    @BindView(R.id.pin_hint) TextView pinHint;
    @BindView(R.id.pin_error) TextView pinError;
    @BindView(R.id.pin_key_fp) View fingerprintButton;

    private List<View> mPinKeys = new ArrayList<>(12);
    /**
     * Sateful drawables:
     * checked = error
     * active = success
     * empty state = default
     */
    private List<View> mIndicators = new ArrayList<>(4);
    private OnInputListener mOnInputListener;
    private OnValueListener mOnValueListener;
    private OnValidationErrorListener mOnValidationErrorListener;
    private OnFingerprintClickListener mOnFingerprintClickListener;
    private Stack<String> mValue = new Stack<>();
    private List<String> mValidValue = new ArrayList<>(4);
    private boolean mEnableValidation = false;
    private boolean mClearReset = false;
    private boolean mEnableFingerprint = false;

    public PinCodeView(@NonNull Context context) {
        super(context);
    }

    public PinCodeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public PinCodeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    @TargetApi(21)
    public PinCodeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr) {
        inflate(getContext(), R.layout.view_pincode_kbd, this);
        ButterKnife.bind(this);

        for (int i = 0; i < pinContainer.getChildCount(); i++) {
            mPinKeys.add(pinContainer.getChildAt(i));
        }

        for (int i = 0; i < pinIndicatorContainer.getChildCount(); i++) {
            mIndicators.add(pinIndicatorContainer.getChildAt(i));
        }

        for (View key : mPinKeys) {
            if (isDigitKey(((String) key.getTag()))) {
                key.setOnClickListener(v -> {
                    if (mClearReset) {
                        mValue.clear();
                        Stream.of(mIndicators).forEach(item -> item.getBackground().setState(new int[0]));
                        mClearReset = false;
                    }
                    String value = getKey(((String) key.getTag()));
                    addValue(value);
                    updateIndicator();

                    if (mOnInputListener != null) {
                        mOnInputListener.onInput(value);
                    }
                });
            } else if (isBackspaceKey(((String) key.getTag()))) {
                key.setOnClickListener(v -> {
                    popValue();
                    updateIndicator();
                });
            } else if (isFingerprintKey(((String) key.getTag()))) {
                key.setOnClickListener(v -> {
                    if (mEnableFingerprint && mOnFingerprintClickListener != null) {
                        mOnFingerprintClickListener.onClick(v);
                    }
                });
            }

        }
    }

    public void setOnFingerprintClickListener(OnFingerprintClickListener listener) {
        mOnFingerprintClickListener = listener;
    }

    public void setEnableFingerprint(boolean enable) {
        mEnableFingerprint = enable;
        fingerprintButton.setVisibility(mEnableFingerprint ? VISIBLE : INVISIBLE);
    }

    public void reset() {
        setEnabled(true);
        mValue.clear();
        mValidValue.clear();
        Stream.of(mIndicators).forEach(item -> item.getBackground().setState(new int[0]));
        setPinHint(null);
        setError(null);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Stream.of(mPinKeys).forEach(item -> {
            item.setEnabled(enabled);
            item.setAlpha(enabled ? 1f : .5f);
        });

    }

    public void setOnValidationErrorListener(OnValidationErrorListener listener) {
        mOnValidationErrorListener = listener;
    }

    public void setError(CharSequence error) {
        if (error == null) {
            pinError.setText(null);
            pinError.setVisibility(GONE);
        } else {
            pinError.setText(error);
            pinError.setVisibility(VISIBLE);
        }
    }

    public void setError(@StringRes int resId) {
        setError(getContext().getResources().getString(resId));
    }

    public void setPinHint(@StringRes int resId) {
        pinHint.setText(resId);
    }

    public void setPinHint(CharSequence hint) {
        pinHint.setText(hint);
    }

    public void setOnValueListener(OnValueListener listener) {
        mOnValueListener = listener;
    }

    public void setOnInputListener(OnInputListener listener) {
        mOnInputListener = listener;
    }

    public String getValue() {
        if (mValue.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String v : mValue) {
            sb.append(v);
        }

        return sb.toString();
    }

    public void setValue(String value) {
        mEnableValidation = true;
        mValidValue.clear();
        if (value.length() != 4) {
            throw new IllegalArgumentException("Value length must contains exactly 4 digits");
        }
        for (char v : value.toCharArray()) {
            mValidValue.add(new String(new char[]{v}));
        }
    }

    public void setEnableValidation(boolean enable) {
        mEnableValidation = enable;
    }

    private void popValue() {
        if (mValue.isEmpty()) return;
        mValue.pop();
    }

    private void addValue(String value) {
        if (mValue.size() >= 4) return;
        mValue.push(value);
    }

    private void updateIndicator() {

        if (mValue.isEmpty()) {
            Stream.of(mIndicators).forEach(item -> item.getBackground().setState(new int[0]));
            if (mOnValueListener != null) {
                mOnValueListener.onValue("", mValue.size(), false);
            }
            return;
        }

        int validCount = 0;
        for (int i = 0; i < 4; i++) {
            Drawable d = mIndicators.get(i).getBackground();
            if (i > mValue.size() - 1) {
                d.setState(new int[0]);
                if (mOnValueListener != null) {
                    mOnValueListener.onValue("", mValue.size(), false);
                }
                break;
            }

            String inputValue = mValue.get(i);
            String validValue;
            if (!mEnableValidation) {
                validValue = inputValue;
            } else {
                if (mValidValue.size() != 4) {
                    throw new IllegalStateException("You must set 4 digits value");
                }
                validValue = mValidValue.get(i);
            }

            boolean valid = inputValue.equals(validValue);

            if (valid) {
                validCount++;
            }
            // success
            d.setState(new int[]{android.R.attr.state_active});
        }

        if (mValue.size() == 4 && mEnableValidation && validCount != mValidValue.size()) {
            for (int i = 0; i < 4; i++) {
                Drawable d = mIndicators.get(i).getBackground();
                // error
                d.setState(new int[]{android.R.attr.state_checked});
            }
            if (mOnValidationErrorListener != null) {
                mOnValidationErrorListener.onError(getValue());
            }
            mClearReset = true;
        }

        if (mOnValueListener != null) {
            mOnValueListener.onValue(getValue(), mValue.size(), validCount == mValidValue.size());
        }
    }

    private boolean isValidKey(String tag) {
        return tag != null && !tag.isEmpty() && tag.length() > 4;
    }

    private boolean isDigitKey(String tag) {
        if (!isValidKey(tag)) {
            return false;
        }

        return tag.substring(4).matches("^[0-9]$");
    }

    private boolean isBackspaceKey(String tag) {
        if (!isValidKey(tag)) return false;

        return tag.substring(4).equals("bsp");
    }

    private boolean isFingerprintKey(String tag) {
        if (!isValidKey(tag)) return false;

        return tag.substring(4).equals("fp");
    }

    private String getKey(String tag) {
        if (!isValidKey(tag)) return null;

        return tag.substring(4);
    }

    public interface OnFingerprintClickListener {
        void onClick(View view);
    }

    public interface OnInputListener {
        void onInput(String key);
    }

    public interface OnValidationErrorListener {
        void onError(String value);
    }

    public interface OnValueListener {
        void onValue(String value, int valueLength, boolean valid);
    }

}
