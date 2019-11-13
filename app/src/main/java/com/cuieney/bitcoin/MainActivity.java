package com.cuieney.bitcoin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletFiles;
import org.bitcoinj.wallet.WalletProtobufSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private Button createLocalWallet;
    private FloatingActionButton transfer;
    private String TAG = "MainActivity";
    private ImageView walletAddressImage;
    private TextView walletAmount;
    private TextView walletAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        transfer = ((FloatingActionButton) findViewById(R.id.btn_transfer));
        walletAmount = ((TextView) findViewById(R.id.wallet_amount));
        walletAddress = ((TextView) findViewById(R.id.wallet_address));
        walletAddressImage = ((ImageView) findViewById(R.id.wallet_img));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1008);
        }
        try {
            loadWalletFromFile();
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
    }



    @SuppressLint("SetTextI18n")
    private void loadWalletFromFile() throws FileNotFoundException, UnreadableWalletException {
        File walletFile = new File(App.WALLET_FILE_PATH);
        Wallet wallet = null;
        if (walletFile.exists()) {
            InputStream inputStream = new FileInputStream(walletFile);
            //反序列化
            wallet = new WalletProtobufSerializer().readWallet(inputStream);
            //设置自动保存
            wallet.autosaveToFile(walletFile, 3 * 1000, TimeUnit.MILLISECONDS, null);
            //清理钱包
            Log.e(TAG, "loadWalletFromFile: " + wallet.getBalance().value);
            Address address = wallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);
            Log.e(TAG, "createWallet: " + address.toString() + wallet.getBalance().value);


            String s = BitcoinURI.convertToBitcoinURI(address, null, null, null);
            Bitmap bitmap = bitmap(s);
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            bitmapDrawable.setFilterBitmap(false);
            walletAddressImage.setImageDrawable(bitmapDrawable);

            walletAmount.setText(wallet.getBalance().value + "");
            walletAddress.setText(s);
        }
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
