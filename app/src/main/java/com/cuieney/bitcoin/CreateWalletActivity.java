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

import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bitcoinj.wallet.KeyChainGroupStructure;
import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletExtension;
import org.bitcoinj.wallet.WalletProtobufSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class CreateWalletActivity extends AppCompatActivity {
    private String TAG = "CreateWalletActivity";
    private RecyclerView deterministicList;
    private TextView confirm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_wallet);
        deterministicList = ((RecyclerView) findViewById(R.id.deterministic_list));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    createWallet();

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "onCreate: ", e);
                }
            }
        }).start();


        confirm = ((TextView) findViewById(R.id.create_wallet));

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CreateWalletActivity.this, MainActivity.class));
                finish();
            }
        });

    }


    private Wallet loadWallet() throws Exception {
        Wallet wallet;
        FileInputStream walletStream = new FileInputStream(App.WALLET_FILE_PATH);
        try {
            List<WalletExtension> extensions = provideWalletExtensions();
            WalletExtension[] extArray = extensions.toArray(new WalletExtension[extensions.size()]);
            Protos.Wallet proto = WalletProtobufSerializer.parseToProto(walletStream);
            final WalletProtobufSerializer serializer;
            serializer = new WalletProtobufSerializer();
            wallet = serializer.readWallet(App.PARAMS, extArray, proto);

        } finally {
            walletStream.close();
        }
        return wallet;
    }


    protected List<WalletExtension> provideWalletExtensions() throws Exception {
        return ImmutableList.of();
    }


    private void createWallet() throws Exception {
        NetworkParameters params = App.PARAMS;
        KeyChainGroup.Builder kcg = KeyChainGroup.builder(params, KeyChainGroupStructure.DEFAULT);
        kcg.fromRandom(Script.ScriptType.P2PKH);
        Wallet wallet = new Wallet(params, kcg.build()); // default
        wallet.freshReceiveKey();
        for (WalletExtension e : provideWalletExtensions()) {
            wallet.addExtension(e);
        }

        wallet.saveToFile(new File(App.WALLET_FILE_PATH));

        wallet = loadWallet();

        DeterministicSeed keyChainSeed = wallet.getKeyChainSeed();
        List<String> mnemonicCode = keyChainSeed.getMnemonicCode();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mnemonicCode.size(); i++) {
            sb.append(mnemonicCode.get(i)).append(" ");
        }
        Log.i(TAG, "createWallet: " + sb.toString());
        initKeyList(mnemonicCode);
    }

    private void initKeyList(final List<String> mnemonicCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deterministicList.setLayoutManager(new LinearLayoutManager(CreateWalletActivity.this, LinearLayoutManager.HORIZONTAL, false));
                deterministicList.setAdapter(new KeyAdapter(mnemonicCode, CreateWalletActivity.this));
            }
        });

    }


}
