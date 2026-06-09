package com.taskguard.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Base64;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

public class UserPhotoUtils {

    private UserPhotoUtils() {
    }

    public static void loadPhoto(ImageView imageView, String photoBase64) {
        Bitmap bitmap = decodeBase64(photoBase64);
        if (bitmap == null) {
            bitmap = createDefaultIcon();
        }
        imageView.setImageBitmap(bitmap);
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }

    public static Bitmap decodeBase64(String photoBase64) {
        if (photoBase64 == null || photoBase64.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.decode(photoBase64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Bitmap createDefaultIcon() {
        int size = 160;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#0D0D0D"));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        paint.setColor(Color.parseColor("#00FF41"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, paint);

        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size * 0.38f, size * 0.16f, paint);
        canvas.drawRoundRect(size * 0.28f, size * 0.58f, size * 0.72f, size * 0.82f, 32f, 32f, paint);

        return bitmap;
    }
}
