# FingerprintScanHelper
Android标准化指纹识别框架，只基于api23官方标准

## 使用方式

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.jianyuyouhun:FingerprintScanHelper:1.0.2'
	}

### 代码中

    new FingerprintScanHelper(context)
            .startAuth(new OnAuthResultListener() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onInputPwd(String pwd) {

                }

                @Override
                public void onFailed(String msg) {

                }

                @Override
                public void onDeviceNotSupport() {

                }
            }, true, true);//cancelAble&canTouchOutsideCancel