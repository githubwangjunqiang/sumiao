package com.xiaoqiang.xiaoxiang;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BitmapCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int CODE_PERISSIM = 1001;
    private static final int PHOTO_REQUEST_GLIDE_CODE = 1002;
    private static final String TAG = "12345";
    private ImageView mImageView;
    private Bitmap bitmap;

    /**
     * 最新 的插入图库方式
     *
     * @param context
     * @param inputStream
     * @param mime_type
     * @param description
     */
    public static Uri saveDcim(Context context, InputStream inputStream, String name, String mime_type, String description) {

        Uri imgUri = null;
        OutputStream os = null;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DESCRIPTION, description);
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name + "." + mime_type);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + mime_type);
            values.put(MediaStore.Images.Media.TITLE, name + "." + mime_type);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/肖像");

            Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ContentResolver resolver = context.getContentResolver();

            Uri insertUri = resolver.insert(external, values);
            Log.d(TAG, "insertUri: " + insertUri);


            if (insertUri != null) {
                os = resolver.openOutputStream(insertUri);
            }
            if (os != null && inputStream != null) {
                byte[] data = new byte[1024 * 4];
                int len;
                while ((len = inputStream.read(data)) != -1) {
                    os.write(data, 0, len);
                }
                imgUri = insertUri;
            }
        } catch (Exception e) {
            Log.d(TAG, "fail: " + e.getCause());
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.d(TAG, "fail in close: " + e.getCause());
            }
        }
        return imgUri;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.imagevieqw);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (PHOTO_REQUEST_GLIDE_CODE == requestCode) {
            if (resultCode != RESULT_OK || data.getData() == null) {
                Toast.makeText(this, "选择失败", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Uri dataData = data.getData();
                ContentResolver resolver = getContentResolver();
                Cursor query = resolver.query(dataData, null, null, null, null);
                query.moveToFirst();
                int columnIndex = query.getColumnIndex(MediaStore.MediaColumns.ORIENTATION);
                Log.d(TAG, "columnIndex: " + columnIndex);
                Log.d(TAG, "getColumnCount: " + query.getColumnCount());
                Log.d(TAG, "getCount: " + query.getCount());
                int angle = 0;
                if (columnIndex != -1) {
                    angle = Integer.parseInt(query.getString(columnIndex));
                    Log.d(TAG, "angle: " + angle);
                    //0, 90, 180, or 270 degrees.

                }
                query.close();


                InputStream inputStream = resolver.openInputStream(dataData);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                options.inSampleSize = Math.max(options.outWidth / 1000, options.outHeight / 1000);
                options.inJustDecodeBounds = false;
                inputStream = resolver.openInputStream(dataData);
                bitmap = BitmapFactory.decodeStream(inputStream, null, options);

                Log.d(TAG, "getWidth: " + bitmap.getWidth());
                Log.d(TAG, "getHeight: " + bitmap.getHeight());
                bitmap = rotateBitmap(bitmap, angle);
                mImageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int degress) {
        if (degress == 0) {
            return bitmap;
        }
        if (bitmap != null) {
            Matrix m = new Matrix();
            m.postRotate(degress);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m,
                    true);
            return bitmap;
        }
        return bitmap;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_PERISSIM) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "没有授予权限", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            doClickGallery(null);

        }
    }

    public void doClickGallery(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, CODE_PERISSIM);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PHOTO_REQUEST_GLIDE_CODE);

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initImageAngle(Bitmap bitmap, InputStream inputStream) {
        Bitmap handlerImage = null;
        try {
            Matrix matrix = new Matrix();
            ExifInterface exifInterface = new ExifInterface(inputStream);
            int angle = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            switch (angle) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(270, bitmap.getWidth(), bitmap.getHeight());
                    handlerImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180, bitmap.getWidth(), bitmap.getHeight());
                    handlerImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90, bitmap.getWidth(), bitmap.getHeight());
                    handlerImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                    break;
                default:

                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (handlerImage != null) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream("FilePathSession.getFaceImagePath()");
                handlerImage.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
                bitmap.recycle();
                handlerImage.recycle();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void doClickGrayscale(View view) {

//        Paint paint = new Paint();
//        ColorMatrix cm = new ColorMatrix();
//        cm.setSaturation(0);
//        paint.setColorFilter(new ColorMatrixColorFilter(cm));
//        mImageView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);


        Sketch sketch = new Sketch();
        Bitmap pencli = sketch.createPencli(bitmap);
        mImageView.setImageBitmap(pencli);

    }

    public void saveGrayscale(View view) {

        File file = new File(getExternalCacheDir(), "肖像." + Bitmap.CompressFormat.JPEG);

        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            Bitmap bitmapForView = getBitmapForView(mImageView);
            boolean compress = bitmapForView.compress(Bitmap.CompressFormat.JPEG, 80, out);
            Log.d(TAG, "compress: " + compress);
            out.flush();
            out.close();
            Uri bitmapUri = saveDcim(this, file, Bitmap.CompressFormat.JPEG.name(), "肖像");
            Toast.makeText(this, "保存完成-》" + bitmapUri, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "bitmapUri: " + bitmapUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static Bitmap getBitmapForView(View src) {
        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        src.draw(canvas);
        return bitmap;
    }

    /**
     * 最新 的插入图库方式
     *
     * @param context
     * @param file
     * @param mime_type
     * @param description
     */
    public static Uri saveDcim(Context context, File file, String mime_type, String description) {

        Uri imgUri = null;
        OutputStream os = null;
        InputStream inputStream = null;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DESCRIPTION, description);
            values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName() + "." + mime_type);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + mime_type);
            values.put(MediaStore.Images.Media.TITLE, file.getName() + "." + mime_type);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/肖像");

            Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ContentResolver resolver = context.getContentResolver();

            Uri insertUri = resolver.insert(external, values);
            Log.d(TAG, "insertUri: " + insertUri);


            if (insertUri != null) {
                os = resolver.openOutputStream(insertUri);
            }
            if (file != null) {
                inputStream = new FileInputStream(file);
            }
            if (os != null && inputStream != null) {
                byte[] data = new byte[1024 * 4];
                int len;
                while ((len = inputStream.read(data)) != -1) {
                    os.write(data, 0, len);
                }
                imgUri = insertUri;
            }
        } catch (Exception e) {
            Log.d(TAG, "fail: " + e.getCause());
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.d(TAG, "fail in close: " + e.getCause());
            }
        }
        return imgUri;
    }

    public static class Sketch {
        /**
         * 整合  最终调用方法
         *
         * @param bitmap
         * @return
         */
        public Bitmap createPencli(Bitmap bitmap) {
            if (bitmap == null) {
                return bitmap;
            }
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            int[] gray = getGray(pixels, width, height);
            int[] inverse = getInverse(gray);

            int[] guassBlur = guassBlur(inverse, width, height);

            int[] output = deceasecolorCompound(guassBlur, gray, width, height);

            return create(pixels, output, width, height);
        }

        /**
         * 原图片去色
         *
         * @param pixels
         * @param width
         * @param height
         * @return
         */
        private int[] getGray(int[] pixels, int width, int height) {
            int gray[] = new int[width * height];
            for (int i = 0; i < width - 1; i++) {
                for (int j = 0; j < height - 1; j++) {
                    int index = width * j + i;
                    int rgba = pixels[index];
                    int g = ((rgba & 0x00FF0000) >> 16) * 3 + ((rgba & 0x0000FF00) >> 8) * 6 + ((rgba & 0x000000FF)) * 1;
                    gray[index] = g / 10;
                }
            }

            return gray;
        }

        /**
         * 对去色灰度图取反色
         *
         * @param gray
         * @return
         */
        private int[] getInverse(int[] gray) {
            int[] inverse = new int[gray.length];

            for (int i = 0, size = gray.length; i < size; i++) {
                inverse[i] = 255 - gray[i];
            }
            return inverse;
        }

        /**
         * 对反色高斯模糊
         *
         * @param inverse
         * @param width
         * @param height
         * @return
         */
        private int[] guassBlur(int[] inverse, int width, int height) {
            int[] guassBlur = new int[inverse.length];

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int temp = width * (j) + (i);
                    if ((i == 0) || (i == width - 1) || (j == 0) || (j == height - 1)) {
                        guassBlur[temp] = 0;
                    } else {
                        int i0 = width * (j - 1) + (i - 1);
                        int i1 = width * (j - 1) + (i);
                        int i2 = width * (j - 1) + (i + 1);
                        int i3 = width * (j) + (i - 1);
                        int i4 = width * (j) + (i);
                        int i5 = width * (j) + (i + 1);
                        int i6 = width * (j + 1) + (i - 1);
                        int i7 = width * (j + 1) + (i);
                        int i8 = width * (j + 1) + (i + 1);

                        int sum = inverse[i0] + 2 * inverse[i1] + inverse[i2] + 2 * inverse[i3] + 4 * inverse[i4] + 2 * inverse[i5] + inverse[i6] + 2 * inverse[i7] + inverse[i8];

                        sum /= 16;

                        guassBlur[temp] = sum;
                    }
                }
            }
            return guassBlur;
        }

        /**
         * 对取得高斯灰度值与 去色灰度值 进行颜色减淡混合
         *
         * @param guassBlur
         * @param gray
         * @param width
         * @param height
         * @return
         */
        private int[] deceasecolorCompound(int[] guassBlur, int[] gray, int width, int height) {
            int a, b, temp;
            float ex;
            int[] output = new int[guassBlur.length];

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int index = j * width + i;
                    b = guassBlur[index];
                    a = gray[index];

                    temp = a + a * b / (256 - b);
                    ex = temp * temp * 1.0f / 255 / 255;
                    temp = (int) (temp * ex);

                    a = Math.min(temp, 255);

                    output[index] = a;
                }
            }
            return output;
        }

        /**
         * 根据混合结果灰度值生成图片
         *
         * @param pixels
         * @param output
         * @param width
         * @param height
         * @return
         */
        private Bitmap create(int[] pixels, int[] output, int width, int height) {
            for (int i = 0, size = pixels.length; i < size; i++) {
                int gray = output[i];
                int pixel = (pixels[i] & 0xff000000) | (gray << 16) | (gray << 8) | gray;//注意加上原图的 alpha通道

                output[i] = pixel;
            }

            return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888);
        }
    }

}