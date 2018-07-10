package com.jianyuyouhun.fingerprintscan.library;

import android.app.Dialog;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 指纹认证弹窗
 * Created by wangyu on 2018/7/10.
 */

@RequiresApi(api = Build.VERSION_CODES.M)
public class FingerprintScanFragmentDialog extends Dialog
        implements TextView.OnEditorActionListener, FingerprintDialogController.Callback {

    private TextView mTitleView;
    private Button mCancelButton;
    private Button mSecondDialogButton;
    private View mFingerprintContent;
    private View mBackupContent;
    private EditText mPassword;
    private CheckBox mUseFingerprintFutureCheckBox;
    private TextView mPasswordDescriptionTextView;
    private TextView mNewFingerprintEnrolledTextView;

    private Stage mStage = Stage.FINGERPRINT;
    private FingerprintManager.CryptoObject mCryptoObject;
    private FingerprintDialogController mFingerprintDialogController;

    private InputMethodManager mInputMethodManager;

    private OnAuthResultListener listener;

    FingerprintScanFragmentDialog(Context context, @NonNull OnAuthResultListener listener) {
        super(context, R.style.FingerTheme);
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_dialog_container);
        initView();
        registerListener();
        initData();
    }

    private void initView() {
        mTitleView = findViewById(R.id.auth_title);
        mCancelButton = findViewById(R.id.cancel_button);
        mSecondDialogButton = findViewById(R.id.second_dialog_button);
        mFingerprintContent = findViewById(R.id.fingerprint_container);
        mBackupContent = findViewById(R.id.backup_container);
        mPassword = findViewById(R.id.password);
        mPasswordDescriptionTextView = findViewById(R.id.password_description);
        mUseFingerprintFutureCheckBox = findViewById(R.id.use_fingerprint_in_future_check);
        mNewFingerprintEnrolledTextView = findViewById(R.id.new_fingerprint_enrolled_description);
        //noinspection ConstantConditions
        mFingerprintDialogController = new FingerprintDialogController(
                (FingerprintManager) getContext().getSystemService(Context.FINGERPRINT_SERVICE),
                (ImageView) findViewById(R.id.fingerprint_icon),
                (TextView) findViewById(R.id.fingerprint_status), this);
    }

    private void registerListener() {
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        mSecondDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mStage == Stage.FINGERPRINT) {
                    goToBackup();
                } else {
                    verifyPassword();
                }
            }
        });
        mPassword.setOnEditorActionListener(this);
    }

    private void initData() {
        mTitleView.setText(R.string.sign_in);
        updateStage();

        // If fingerprint authentication is not available, switch immediately to the backup
        // (password) screen.
        if (!mFingerprintDialogController.isFingerprintAuthAvailable()) {
            goToBackup();
        }
    }

    @Override
    public void show() {
        super.show();
        if (mStage == Stage.FINGERPRINT) {
            mFingerprintDialogController.startListening(mCryptoObject);
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mFingerprintDialogController.stopListening();
    }

    public void setStage(Stage stage) {
        mStage = stage;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mInputMethodManager = getContext().getSystemService(InputMethodManager.class);
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        mCryptoObject = cryptoObject;
    }

    /**
     * Switches to backup (password) screen. This either can happen when fingerprint is not
     * available or the user chooses to use the password authentication method by pressing the
     * button. This can also happen when the user had too many fingerprint attempts.
     */
    private void goToBackup() {
        mStage = Stage.PASSWORD;
        updateStage();
        mPassword.requestFocus();

        // Show the keyboard.
        mPassword.postDelayed(mShowKeyboardRunnable, 500);

        // Fingerprint is not used anymore. Stop listening for it.
        mFingerprintDialogController.stopListening();
    }

    /**
     * Checks whether the current entered password is correct, and dismisses the the dialog and
     * let's the activity know about the result.
     */
    private void verifyPassword() {
        String password = mPassword.getText().toString();
        if (!checkPassword(password)) {
            return;
        }
        if (mStage == Stage.NEW_FINGERPRINT_ENROLLED) {
            mStage = Stage.FINGERPRINT;
        }
        mPassword.setText("");
        listener.onInputPwd(password);
        dismiss();
    }

    private boolean checkPassword(String password) {
        // Assume the password is always correct.
        // In the real world situation, the password needs to be verified in the server side.
        return password.length() > 0;
    }

    private final Runnable mShowKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            mInputMethodManager.showSoftInput(mPassword, 0);
        }
    };

    private void updateStage() {
        switch (mStage) {
            case FINGERPRINT:
                mCancelButton.setText(R.string.finger_auth_cancel);
                mSecondDialogButton.setText(R.string.use_password);
                mFingerprintContent.setVisibility(View.VISIBLE);
                mBackupContent.setVisibility(View.GONE);
                break;
            case NEW_FINGERPRINT_ENROLLED:
                // Intentional fall through
            case PASSWORD:
                mCancelButton.setText(R.string.finger_auth_cancel);
                mSecondDialogButton.setText(R.string.ok);
                mFingerprintContent.setVisibility(View.GONE);
                mBackupContent.setVisibility(View.VISIBLE);
                if (mStage == Stage.NEW_FINGERPRINT_ENROLLED) {
                    mPasswordDescriptionTextView.setVisibility(View.GONE);
                    mNewFingerprintEnrolledTextView.setVisibility(View.VISIBLE);
                    mUseFingerprintFutureCheckBox.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            verifyPassword();
            return true;
        }
        return false;
    }

    @Override
    public void onAuthenticated() {
        listener.onSuccess();
        dismiss();
    }

    @Override
    public void onError() {
        goToBackup();
    }

    /**
     * 认证方式
     */
    public enum Stage {
        FINGERPRINT,//正常认证
        NEW_FINGERPRINT_ENROLLED,//新指纹认证
        PASSWORD//密码认证
    }
}
