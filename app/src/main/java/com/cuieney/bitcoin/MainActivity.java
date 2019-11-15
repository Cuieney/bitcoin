package com.cuieney.bitcoin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.core.listeners.PeerDisconnectedEventListener;
import org.bitcoinj.core.listeners.PeerDiscoveredEventListener;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.bitcoinj.wallet.listeners.WalletReorganizeEventListener;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    private FloatingActionButton transfer;
    private String TAG = "MainActivity";
    private ImageView walletAddressImage;
    private TextView walletAmount;
    private TextView walletAddress;
    private TextView walletRefreshAddress;
    private Wallet wallet;
    private WalletAppKit walletAppKit;
    private TextInputLayout targetAddress;
    private TextInputLayout targetAmount;

    protected FrameLayout flDownloadContent_LDP;
    protected ProgressBar pbProgress_LDP;
    protected TextView tvPercentage_LDP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setBtcSDKThread();
        flDownloadContent_LDP = findViewById(R.id.flDownloadContent_LDP);
        targetAddress = findViewById(R.id.target_address);
        targetAmount = findViewById(R.id.target_amount);
        pbProgress_LDP = findViewById(R.id.pbProgress_LDP);
        tvPercentage_LDP = findViewById(R.id.tvPercentage_LDP);
        transfer = ((FloatingActionButton) findViewById(R.id.btn_transfer));
        walletAmount = ((TextView) findViewById(R.id.tvMyBalance_AM));
        walletAddress = ((TextView) findViewById(R.id.wallet_address));
        walletRefreshAddress = ((TextView) findViewById(R.id.wallet_refresh_address));
        walletAddressImage = ((ImageView) findViewById(R.id.wallet_img));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1008);
        }


        flDownloadContent_LDP.setVisibility(View.VISIBLE);
        walletRefreshAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Address address = walletAppKit.wallet().freshReceiveAddress();
                String s = BitcoinURI.convertToBitcoinURI(address, null, null, null);
                final Bitmap bitmap = bitmap(s);
                walletAddress.setText(address.toString());
                BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
                bitmapDrawable.setFilterBitmap(false);
                walletAddressImage.setImageDrawable(bitmapDrawable);
                walletAmount.setText(walletAppKit.wallet().getBalance().toFriendlyString());

            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadWalletFromFile();
            }
        }).start();


        transfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCoins();
            }
        });

        walletAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("Label", walletAddress.getText().toString());
                cm.setPrimaryClip(mClipData);
                Toast.makeText(MainActivity.this, "地址已复制", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void sendCoins() {
        String address = targetAddress.getEditText().getText().toString().trim();
        String amount = targetAmount.getEditText().getText().toString();

        if (TextUtils.isEmpty(address) || address.equals("Scan recipient QR")) {
            Toast.makeText(MainActivity.this, "地址不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(amount) | Double.parseDouble(amount) <= 0) {
            Toast.makeText(MainActivity.this, "无效的数量", Toast.LENGTH_SHORT).show();
            return;
        }
        if (walletAppKit.wallet().getBalance().isLessThan(Coin.parseCoin(amount))) {
            Toast.makeText(MainActivity.this, "你没有足够的coins", Toast.LENGTH_SHORT).show();
            targetAmount.getEditText().setText(null);
            return;
        }

        Coin value = Coin.parseCoin(amount);
        LegacyAddress to = LegacyAddress.fromBase58(App.PARAMS, address);

        try {
            Wallet.SendResult result = wallet.sendCoins(walletAppKit.peerGroup(), to, value);
            Log.i(TAG, "coins sent. transaction hash: " + result.tx.getTxId());
            targetAddress.getEditText().setText(null);
            targetAmount.getEditText().setText(null);
        } catch (InsufficientMoneyException e) {
            Log.i(TAG, "Not enough coins in your wallet. Missing " + e.missing.getValue() + " satoshis are missing (including fees)");
            Log.i(TAG, "Send money to: " + walletAppKit.wallet().currentReceiveAddress().toString());

            ListenableFuture<Coin> balanceFuture = walletAppKit.wallet().getBalanceFuture(value, Wallet.BalanceType.AVAILABLE);
            FutureCallback<Coin> callback = new FutureCallback<Coin>() {
                @Override
                public void onSuccess(Coin balance) {
                    Log.i(TAG, "coins arrived and the wallet now has enough balance");
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.i(TAG, "something went wrong");
                }
            };
            Futures.addCallback(balanceFuture, callback, MoreExecutors.directExecutor());
        }

    }

    private void setBtcSDKThread() {
        final Handler handler = new Handler();
        Threading.USER_THREAD = handler::post;
    }

    @SuppressLint("SetTextI18n")
    private void loadWalletFromFile() {
        walletAppKit = new WalletAppKit(App.PARAMS, new File(App.WALLET_DIR), App.WALLET_PREFIX) {
            @Override
            protected void onSetupCompleted() {
                wallet = wallet();
                if (wallet.getImportedKeys().size() < 1) wallet.importKey(new ECKey());
                wallet.allowSpendingUnconfirmedTransactions();
                wallet.addChangeEventListener(mWalletListener);
                wallet.addCoinsReceivedEventListener(mWalletListener);
                wallet.addCoinsSentEventListener(mWalletListener);
                wallet.addReorganizeEventListener(mWalletListener);
                updateUI(wallet);
            }
        };

        walletAppKit.setDownloadListener(new DownloadProgressTracker() {
            @Override
            protected void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);
                final int percentage = (int) pct;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbProgress_LDP.setProgress(percentage);
                    }
                });

                Log.i(TAG, "progress: " + percentage);
            }

            @Override
            protected void startDownload(int blocks) {
                super.startDownload(blocks);
                Log.i(TAG, "startDownload: ");
            }

            @Override
            protected void doneDownload() {
                super.doneDownload();
                updateUI(wallet);
                Log.i(TAG, "doneDownload: ");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        flDownloadContent_LDP.setVisibility(View.GONE);
                        transfer.setVisibility(View.VISIBLE);
                    }
                });
            }


        });

        walletAppKit.setBlockingStartup(false);
        walletAppKit.startAsync();

    }

    private WalletListener mWalletListener = new WalletListener();

    private void updateUI(final Wallet wallet) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Address address = wallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);
                String s = BitcoinURI.convertToBitcoinURI(address, null, null, null);
                final Bitmap bitmap = bitmap(s);

                walletAddress.setText(address.toString());
                walletAmount.setText(walletAppKit.wallet().getBalance().toFriendlyString());
                BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
                bitmapDrawable.setFilterBitmap(false);
                walletAddressImage.setImageDrawable(bitmapDrawable);
            }
        });
    }


    private class WalletListener implements WalletChangeEventListener,
            WalletCoinsSentEventListener, WalletReorganizeEventListener, WalletCoinsReceivedEventListener {

        @Override
        public void onWalletChanged(Wallet wallet) {
            Log.d(TAG, "onWalletChanged: " + wallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS));
        }

        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            Log.d(TAG, "onCoinsSent: " + tx.getHashAsString() + "preBalance: "
                    + prevBalance.getValue() + "newBalance: " + newBalance.getValue());

            Toast.makeText(MainActivity.this, "Sent " + prevBalance.minus(newBalance).minus(tx.getFee()).toFriendlyString(), Toast.LENGTH_LONG).show();
            updateUI(wallet);
        }

        @Override
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            Log.d(TAG, "onCoinsReceived: " + tx.getHashAsString() + "prevBalance" + prevBalance.getValue()
                    + "newBalance " + newBalance.toFriendlyString());
            Toast.makeText(MainActivity.this, "Receive " + newBalance.minus(prevBalance).toFriendlyString(), Toast.LENGTH_LONG).show();
            updateUI(wallet);
        }

        @Override
        public void onReorganize(Wallet wallet) {
            updateUI(wallet);
        }
    }

    private PeerConnectedEventListener mPeerConnectedEventListener = new PeerConnectedEventListener() {
        @Override
        public void onPeerConnected(Peer peer, int peerCount) {
            Log.d(TAG, "onPeerConnected: " + peer.toString());
        }
    };

    private PeerDisconnectedEventListener mPeerDisconnectedEventListener = new PeerDisconnectedEventListener() {
        @Override
        public void onPeerDisconnected(Peer peer, int peerCount) {
            Log.d(TAG, "onPeerDisconnected: " + peer.toString());
        }
    };

    private PeerDiscoveredEventListener mPeerDiscoveredEventListener = new PeerDiscoveredEventListener() {
        @Override
        public void onPeersDiscovered(Set<PeerAddress> peerAddresses) {
            if (peerAddresses != null) {
                Log.d(TAG, "onPeersDiscovered: " + peerAddresses.iterator().next().getHostname());
            }
        }
    };

    private void shutdown(){
        if (wallet != null) {
            wallet.removeChangeEventListener(mWalletListener);
            wallet.removeCoinsReceivedEventListener(mWalletListener);
            wallet.removeCoinsSentEventListener(mWalletListener);
            wallet.removeReorganizeEventListener(mWalletListener);
        }

        PeerGroup peerGroup = walletAppKit.peerGroup();
        if (peerGroup != null) {
            peerGroup.removeWallet(wallet);
            peerGroup.stopAsync();
            peerGroup.removeConnectedEventListener(mPeerConnectedEventListener);
            peerGroup.removeDisconnectedEventListener(mPeerDisconnectedEventListener);
            peerGroup.removeDiscoveredEventListener(mPeerDiscoveredEventListener);
            peerGroup = null;
        }
        walletAppKit.stopAsync();
        wallet = null;
        walletAppKit = null;
        System.exit(0);
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutdown();
    }

    public Bitmap bitmap(final String content) {
        try {
            final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
            hints.put(EncodeHintType.MARGIN, 0);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final BitMatrix result = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);

            final int width = result.getWidth();
            final int height = result.getHeight();
            final byte[] pixels = new byte[width * height];

            for (int y = 0; y < height; y++) {
                final int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = (byte) (result.get(x, y) ? -1 : 0);
                }
            }

            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(pixels));
            return bitmap;
        } catch (final WriterException x) {
            Log.e(TAG, "problem creating qr code", x);
            return null;
        }
    }

}
