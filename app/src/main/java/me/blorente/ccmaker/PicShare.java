package me.blorente.ccmaker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PicShare extends Activity implements View.OnClickListener {

    Button share, cancel;
    Intent receivedIntent;

    int watermarkResource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            watermarkResource = R.drawable.by;
            receivedIntent = intent; // Handle single image being sent
            setContentView(R.layout.create_watermark);

            this.getWindow().setLayout(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);

            installListeners();
        } else {
            this.finish();
        }
    }

    private void installListeners() {
        share = (Button) findViewById(R.id.ok);
        cancel = (Button) findViewById(R.id.cancel);

        share.setOnClickListener(this);
        cancel.setOnClickListener(this);
    }

    private void handleSendImage(Intent received) {
        Uri imageUri = received.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            try {
                Bitmap bitmap = getSharedImage(imageUri);
                bitmap = addCCWatermark(bitmap);
                saveBitmap(bitmap);
                sendImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void startProgressSpinner() {
        ProgressDialog spinner = new ProgressDialog(this);
        spinner.setMessage("Processing Image...");
        spinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        spinner.setIndeterminate(true);
        spinner.show();
    }

    private Bitmap getSharedImage(Uri imageUri) throws IOException {
        return MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
    }

    private Bitmap addCCWatermark(Bitmap bitmap) {
        Resources res = getResources();
        Bitmap watermark = BitmapFactory.decodeResource(res, watermarkResource);

        int w = bitmap.getWidth();
        int h = bitmap.getHeight() + watermark.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, bitmap.getConfig());
        Canvas canvas = new Canvas(result);

        // Draw original image
        canvas.drawBitmap(bitmap, 0, 0, null);

        // Fill watermark line with black
        Rect watermark_zone = new Rect(0, bitmap.getHeight(), bitmap.getWidth(), h);
        Paint blackPaint = new Paint(Color.BLACK);
        canvas.drawRect(watermark_zone, blackPaint);

        // Draw watermark
        canvas.drawBitmap(watermark, 0, watermark_zone.top, null);

        return result;
    }

    private void saveBitmap(Bitmap bitmap) {
        Context context = getApplicationContext();
        // save bitmap to cache directory
        try {

            File cachePath = new File(context.getCacheDir(), "images");
            cachePath.mkdirs(); // don't forget to make the directory
            FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendImage() {
        Context context = getApplicationContext();

        File imagePath = new File(context.getCacheDir(), "images");
        File newFile = new File(imagePath, "image.png");
        Uri contentUri = FileProvider.getUriForFile(context, "me.blorente.ccmaker.fileprovider", newFile);

        if (contentUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            startActivity(Intent.createChooser(shareIntent, "Choose an app"));
        }
    }

    public void onLicensePickerClick(View v) {
        boolean checked = ((RadioButton) v).isChecked();

        switch(v.getId()) {
            case R.id.cc_by_radio:
                if (checked)
                    watermarkResource = R.drawable.by;
                break;
            case R.id.cc_by_sa_radio:
                if (checked)
                    watermarkResource = R.drawable.by_sa;
                break;
            case R.id.cc_by_nc_radio:
                if (checked)
                    watermarkResource = R.drawable.by_nc;
                break;
            case R.id.cc_by_nd_radio:
                if (checked)
                    watermarkResource = R.drawable.by_nd;
                break;
            case R.id.cc_by_nc_sa_radio:
                if (checked)
                    watermarkResource = R.drawable.by_nc_sa;
                break;
            case R.id.cc_by_nc_nd_radio:
                if (checked)
                    watermarkResource = R.drawable.by_nc_nd;
                break;
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ok:
                startProgressSpinner();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        handleSendImage(receivedIntent);
                    }
                }).start();
                break;
            case R.id.cancel:
                this.finish();
                break;
        }
    }
}
