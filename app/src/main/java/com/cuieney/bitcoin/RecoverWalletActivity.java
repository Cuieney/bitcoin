package com.cuieney.bitcoin;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.io.IOException;



@SuppressLint("Registered")
public class RecoverWalletActivity extends AppCompatActivity {
    private String TAG = "RecoverWalletActivity";
    private Button confirm;
    private EditText seedCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recover_wallet);

        confirm = findViewById(R.id.confirm);
        seedCode = ((EditText) findViewById(R.id.seed_code));

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recoveryWallet();
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    public void recoveryWallet(){
        final String seed = seedCode.getText().toString();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                NetworkParameters params = App.PARAMS;
                DeterministicSeed deterministicSeed = null;
                try {
                    deterministicSeed = new DeterministicSeed(seed, null, "", 0);
                } catch (UnreadableWalletException e) {
                    e.printStackTrace();
                    Log.i(TAG, "recoveryWallet: "+e);
                }
                Wallet wallet = Wallet.fromSeed(params, deterministicSeed, Script.ScriptType.P2PKH);
                wallet.clearTransactions(0);
                File file = new File(App.WALLET_FILE_PATH);

                try {
                    wallet.saveToFile(file);
                    wallet.clearTransactions(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                startActivity(new Intent(RecoverWalletActivity.this,MainActivity.class));
            }
        }.execute();

    }
}

