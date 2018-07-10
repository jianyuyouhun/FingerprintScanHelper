package com.jianyuyouhun.fingerprintscan.library;

import android.app.KeyguardManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Created by wangyu on 2018/7/10.
 */

public class FingerprintScanHelper {

    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private KeyguardManager keyguardManager;
    private static final String DEFAULT_KEY_NAME = "default_key";
    private AppCompatActivity context;
    private FingerprintManager fingerprintManager;

    public FingerprintScanHelper(AppCompatActivity context) {
        this.context = context;
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager = context.getSystemService(KeyguardManager.class);
            fingerprintManager = context.getSystemService(FingerprintManager.class);
            try {
                mKeyGenerator = KeyGenerator
                        .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
            }
        }
    }

    public void startAuth(OnAuthResultListener listener) {
        startAuth(listener, true, true);
    }

    public void startAuth(OnAuthResultListener listener, boolean cancelAble, boolean canTouchOutsideCancel) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || fingerprintManager == null
                || keyguardManager == null
                || !fingerprintManager.isHardwareDetected()) {
            listener.onDeviceNotSupport();
        } else if (!keyguardManager.isKeyguardSecure()) {
            listener.onFailed("请先设置锁屏密码");
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            listener.onFailed("请录入至少一个指纹");
        } else {
            Cipher defaultCipher;
            try {
                defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new RuntimeException("Failed to get an instance of Cipher", e);
            }
            createKey(DEFAULT_KEY_NAME);
            doAuth(defaultCipher, DEFAULT_KEY_NAME, listener, cancelAble, canTouchOutsideCancel);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void doAuth(Cipher defaultCipher, String defaultKeyName, OnAuthResultListener listener,
                        boolean cancelAble, boolean canTouchOutsideCancel) {
        if (initCipher(defaultCipher, defaultKeyName)) {
            FingerprintScanFragmentDialog dialog
                    = new FingerprintScanFragmentDialog(context, listener, cancelAble, canTouchOutsideCancel);
            dialog.setCryptoObject(new FingerprintManager.CryptoObject(defaultCipher));
            dialog.setStage(
                    FingerprintScanFragmentDialog.Stage.FINGERPRINT);
            dialog.show();
        } else {
            // This happens if the lock screen has been disabled or or a fingerprint got
            // enrolled. Thus show the dialog to authenticate with their password first
            // and ask the user if they want to authenticate with fingerprints in the
            // future
            FingerprintScanFragmentDialog dialog
                    = new FingerprintScanFragmentDialog(context, listener, cancelAble, canTouchOutsideCancel);
            dialog.setCryptoObject(new FingerprintManager.CryptoObject(defaultCipher));
            dialog.setStage(
                    FingerprintScanFragmentDialog.Stage.NEW_FINGERPRINT_ENROLLED);
            dialog.show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void createKey(String keyName) {
        try {
            mKeyStore.load(null);
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            mKeyGenerator.init(builder.build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean initCipher(Cipher cipher, String keyName) {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }
}
