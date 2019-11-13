package com.cuieney.bitcoin;

import android.app.Application;
import android.os.Environment;

public class App extends Application {
    public static final String WALLET_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wallet-protobuf1";

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
