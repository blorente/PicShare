package me.blorente.ccmaker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CreateWatermark extends Activity {

    private static final int WATERMARK_PADDING_X = 10;
    private static final int WATERMARK_PADDING_Y = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            handleSendImage(intent); // Handle single image being sent
        }
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

    private Bitmap getSharedImage(Uri imageUri) throws IOException {
        return MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
    }

    private Bitmap addCCWatermark(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, bitmap.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(bitmap, 0, 0, null);

        Resources res = getResources();
        Bitmap watermark = BitmapFactory.decodeResource(res, R.drawable.by_sa);
        watermark = Bitmap.createScaledBitmap(watermark, w / 5, h / 5, false);

        int watermarkX = w - watermark.getWidth() - WATERMARK_PADDING_X;
        int watermarkY = h - watermark.getHeight() - WATERMARK_PADDING_Y;
        canvas.drawBitmap(watermark, watermarkX, watermarkY, null);

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
}
