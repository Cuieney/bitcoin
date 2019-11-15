package com.cuieney.bitcoin;

import android.app.Application;
import android.os.Environment;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.BriefLogFormatter;

public class App extends Application {
    public static final String WALLET_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();//muUDcDMeWbqPkphy1oXsRpyELEieqXrbvx
    public static final String WALLET_PREFIX = "users_wallet";//muUDcDMeWbqPkphy1oXsRpyELEieqXrbvx
    public static final String WALLET_FILE_PATH = WALLET_DIR + "/"+WALLET_PREFIX+".wallet";//muUDcDMeWbqPkphy1oXsRpyELEieqXrbvx
    public static final String WALLET_SEED_PATH = WALLET_DIR + "/seedcode.txt";//muUDcDMeWbqPkphy1oXsRpyELEieqXrbvx
    public static final String CHAIN_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/restore-from-seed.spvchain";//muUDcDMeWbqPkphy1oXsRpyELEieqXrbvx

    //    public static final String WALLET_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wallet-protobuf2";//muUDcDMeWbqPkphy1oXsRpyELEieqXrbvx
    public static final NetworkParameters PARAMS = true ? TestNet3Params.get() : RegTestParams.get();

    @Override
    public void onCreate() {
        super.onCreate();
        BriefLogFormatter.init();
    }
}
