package com.cuieney.bitcoin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletFiles;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CreateWalletActivity extends AppCompatActivity {
    private String TAG = "MainActivity";
    private RecyclerView deterministicList;
    private TextView confirm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_wallet);
        deterministicList = ((RecyclerView) findViewById(R.id.deterministic_list));
        try {
            createWallet();
        } catch (IOException e) {
            e.printStackTrace();
        }

        confirm = ((TextView) findViewById(R.id.create_wallet));

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CreateWalletActivity.this,MainActivity.class));
            }
        });

    }

//    TestNet3Params；// 公共测试网络
//    MainNetParams；// 私有测试网络
//    RegTestParams；// 生产网络
    private void createWallet() throws IOException {
        NetworkParameters params = TestNet3Params.get();
        Wallet wallet = Wallet.createDeterministic(params, Script.ScriptType.P2PKH);
        File file = new File(App.WALLET_FILE_PATH);
        if (!file.exists()) {
            file.createNewFile();
        }
        WalletFiles walletFiles = wallet.autosaveToFile(file, 3 * 1000, TimeUnit.MILLISECONDS, new WalletFiles.Listener() {
            @Override
            public void onBeforeAutoSave(File tempFile) {
                Log.e(TAG, "onBeforeAutoSave: " + tempFile.getAbsolutePath());

            }
            @Override
            public void onAfterAutoSave(File newlySavedFile) {
                Log.e(TAG, "onBeforeAutoSave: " + newlySavedFile.getAbsolutePath());
            }
        });
        walletFiles.saveNow();

        DeterministicSeed keyChainSeed = wallet.getKeyChainSeed();
        List<String> mnemonicCode = keyChainSeed.getMnemonicCode();
        initKeyList(mnemonicCode);
    }

    private void initKeyList(List<String> mnemonicCode){
        deterministicList.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));
        deterministicList.setAdapter(new KeyAdapter(mnemonicCode,this));
    }





}
