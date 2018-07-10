package com.jianyuyouhun.fingerprintscan.library;

/**
 * 认证回调监听
 * Created by wangyu on 2018/7/10.
 */

public interface OnAuthResultListener {
    /**
     * 认证成功
     */
    void onSuccess();

    /**
     * 使用密码
     *
     * @param pwd
     */
    void onInputPwd(String pwd);

    /**
     * 认证失败，返回原因
     *
     * @param msg
     */
    void onFailed(String msg);

    /**
     * 设备不支持
     */
    void onDeviceNotSupport();
}
