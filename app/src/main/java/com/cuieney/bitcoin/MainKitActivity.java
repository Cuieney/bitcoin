package com.cuieney.bitcoin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.CheckpointManager;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.core.listeners.PeerDisconnectedEventListener;
import org.bitcoinj.core.listeners.PeerDiscoveredEventListener;
import org.bitcoinj.net.discovery.MultiplexingDiscovery;
import org.bitcoinj.net.discovery.PeerDiscovery;
import org.bitcoinj.net.discovery.PeerDiscoveryException;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletProtobufSerializer;
import org.bitcoinj.wallet.listeners.WalletChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.bitcoinj.wallet.listeners.WalletReorganizeEventListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class MainKitActivity extends AppCompatActivity {

    private Button createLocalWallet;
    private FloatingActionButton transfer;
    private String TAG = "MainActivity";
    private ImageView walletAddressImage;
    private TextView walletAmount;
    private TextView walletAddress;
    private PeerGroup peerGroup;
    private Wallet wallet;
    private SPVBlockStore blockStore;
    private BlockChain blockChain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        transfer = ((FloatingActionButton) findViewById(R.id.btn_transfer));
        walletAddress = ((TextView) findViewById(R.id.wallet_address));
        walletAddressImage = ((ImageView) findViewById(R.id.wallet_img));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1008);
        }
        try {
            loadWalletFromFile();

            createBlockChain();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnreadableWalletException e) {
            e.printStackTrace();
        }


        transfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        walletAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("Label", walletAddress.getText().toString());
                cm.setPrimaryClip(mClipData);
                Toast.makeText(MainKitActivity.this, "地址已复制", Toast.LENGTH_SHORT).show();
            }
        });


    }


    @SuppressLint("SetTextI18n")
    private void loadWalletFromFile() throws FileNotFoundException, UnreadableWalletException {
        File walletFile = new File(App.WALLET_FILE_PATH);

        if (walletFile.exists()) {
            InputStream inputStream = new FileInputStream(walletFile);
            //反序列化
            wallet = new WalletProtobufSerializer().readWallet(inputStream);
            //设置自动保存
            wallet.autosaveToFile(walletFile, 3 * 1000, TimeUnit.MILLISECONDS, null);
            //清理钱包
            wallet.cleanup();
            Log.e(TAG, "loadWalletFromFile: " + wallet.getBalance().value);
            Address address = wallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);
            Log.e(TAG, "createWallet: " + address.toString() + wallet.getBalance().value);

            wallet.addChangeEventListener(mWalletListener);
            wallet.addCoinsReceivedEventListener(mWalletListener);
            wallet.addCoinsSentEventListener(mWalletListener);
            wallet.addReorganizeEventListener(mWalletListener);
            DeterministicSeed keyChainSeed = wallet.getKeyChainSeed();
            Log.i(TAG, "loadWalletFromFile: "+keyChainSeed.getCreationTimeSeconds());
            List<String> mnemonicCode = keyChainSeed.getMnemonicCode();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mnemonicCode.size(); i++) {
                sb.append(mnemonicCode.get(i)).append(" ");
            }
            Log.i(TAG, "loadWalletFromFile: "+sb.toString());
            updateUI(wallet);



        }
    }

    private WalletListener mWalletListener = new WalletListener();

    private void updateUI(final Wallet wallet) {
        final Address address = wallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        String s = BitcoinURI.convertToBitcoinURI(address, null, null, null);
        final Bitmap bitmap = bitmap(s);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Coin balance = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
                walletAddress.setText(address.toString());
                String balanceString = String.valueOf(balance.value / 100000);
                walletAmount.setText(balanceString);
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
            updateUI(wallet);
        }

        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            Log.d(TAG, "onCoinsSent: " + tx.getHashAsString() + "preBalance: "
                    + prevBalance.getValue() + "newBalance: " + newBalance.getValue());
            updateUI(wallet);
        }

        @Override
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            Log.d(TAG, "onCoinsReceived: " + tx.getHashAsString() + "prevBalance" + prevBalance.getValue()
                    + "newBalance " + newBalance.toFriendlyString());
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

    private void createBlockChain() {
        Log.d(TAG, "createBlockChain: ");
        File blockChainFile = new File(getDir("blockstore", Context.MODE_PRIVATE), "blockchain");
        boolean blockChainFileExists = blockChainFile.exists();
        if (!blockChainFileExists) {
            wallet.reset();
        }

        try {
            blockStore = new SPVBlockStore(App.PARAMS, blockChainFile);
            blockStore.getChainHead(); // detect corruptions as early as possible

            final long earliestKeyCreationTime = wallet.getEarliestKeyCreationTime();

            if (!blockChainFileExists && earliestKeyCreationTime > 0) {
                try {
                    final InputStream checkpointsInputStream = getAssets()
                            .open("checkpoints-testnet.txt");
                    CheckpointManager.checkpoint(App.PARAMS, checkpointsInputStream,
                            blockStore, earliestKeyCreationTime);
                } catch (final IOException x) {
                    x.printStackTrace();
                }
            }
        } catch (final BlockStoreException x) {
            blockChainFile.delete();
            x.printStackTrace();
        }
        try {
            blockChain = new BlockChain(App.PARAMS, wallet, blockStore);
        } catch (final BlockStoreException x) {
            throw new Error("blockchain cannot be created", x);
        }
        startup();
    }

    public static final String USER_AGENT = "Bitcoin Wallet";

    private void startup() {
        Log.d(TAG, "startup: ");
        peerGroup = new PeerGroup(App.PARAMS, blockChain);
        peerGroup.setDownloadTxDependencies(0); // recursive implementation causes StackOverflowError
        peerGroup.addWallet(wallet);//设置钱包，重要
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            peerGroup.setUserAgent(USER_AGENT, packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        peerGroup.setMaxConnections(8);
        int connectTimeout = (int) (15 * DateUtils.SECOND_IN_MILLIS);
        peerGroup.setConnectTimeoutMillis(connectTimeout);
        int discoveryTimeout = (int) (10 * DateUtils.SECOND_IN_MILLIS);
        peerGroup.addConnectedEventListener(mPeerConnectedEventListener);
        peerGroup.addDisconnectedEventListener(mPeerDisconnectedEventListener);
        peerGroup.addDiscoveredEventListener(mPeerDiscoveredEventListener);
        peerGroup.setPeerDiscoveryTimeoutMillis(discoveryTimeout);

        //添加节点探索器，重要
        peerGroup.addPeerDiscovery(new PeerDiscovery() {
            private final PeerDiscovery normalPeerDiscovery = MultiplexingDiscovery
                    .forServices(App.PARAMS, 0);

            @Override
            public InetSocketAddress[] getPeers(final long services, final long timeoutValue,
                                                final TimeUnit timeoutUnit) throws PeerDiscoveryException {
                return normalPeerDiscovery.getPeers(services, timeoutValue, timeoutUnit);
            }

            @Override
            public void shutdown() {
                normalPeerDiscovery.shutdown();
            }
        });
        peerGroup.startAsync();
        peerGroup.startBlockChainDownload(new DownloadProgressTracker(){
            @Override
            protected void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);
                int percentage = (int) pct;
                Log.i(TAG, "progress: "+percentage);
            }

            @Override
            protected void doneDownload() {
                super.doneDownload();
                Log.i(TAG, "progress: download success");
                updateUI(wallet);
            }
        });
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
