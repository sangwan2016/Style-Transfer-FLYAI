package com.flyai.StyleTransfer;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

class RetrofitTest {

    Retrofit retrofit;
    MainActivity.RetrofitService service;
    final String BASE_URL = "http://172.20.10.9:5000";

    public static RetrofitTest retrofitTest = new RetrofitTest();

    private RetrofitTest() {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.callTimeout(10, TimeUnit.HOURS)
            .connectTimeout(10, TimeUnit.HOURS)
            .readTimeout(10, TimeUnit.HOURS)
            .writeTimeout(10, TimeUnit.HOURS);
        httpClient.addInterceptor(new Interceptor() {
            @NonNull
            @Override
            public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
                Request request = chain.request().newBuilder().addHeader("Connection", "close").build();
                return chain.proceed(request);
            }
        });

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build());
        retrofit = builder.build();

        service = retrofit.create(MainActivity.RetrofitService.class);
    }

    public static RetrofitTest getInstance() {
        return retrofitTest;
    }

    public MainActivity.RetrofitService getService() {
        return service;
    }
}

public class MainActivity extends AppCompatActivity {
    // Elements for flip pages
    ViewFlipper viewFlipper;
    Button afterButton;
    Button beforeButton;
    // Elements for style
    Button styleGallery;
    Button styleCamera;
    ImageView styleImage;
    final int styleIndex = 0;
    Uri styleUri;
    // Elements for content
    Button contentGallery;
    Button contentCamera;
    ImageView contentImage;
    final int contentIndex = 1;
    Uri contentUri;
    // Elements for result
    Button startTrain;
    TextView progressText;
    ProgressBar trainingProgress;
    ImageView resultImage;
    Button saveResult;
    Uri resultUri;
    final int resultIndex = 2;
    // Constants
    final int PICK_STYLE_GALLERY = 3;
    final int PICK_STYLE_CAMERA = 4;
    final int PICK_CONTENT_GALLERY = 5;
    final int PICK_CONTENT_CAMERA = 6;
    String basePath;

    interface RetrofitService {
        @Multipart
        @POST("/uploader")
        Call<ResponseBody> ImageUpload(
                @Part List<MultipartBody.Part> images
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialization
        viewFlipper = (ViewFlipper)  findViewById(R.id.viewFlipper);
        afterButton = (Button) findViewById(R.id.afterButton);
        beforeButton = (Button) findViewById(R.id.beforeButton);
        styleGallery = (Button) findViewById(R.id.styleGallery);
        styleCamera = (Button) findViewById(R.id.styleCamera);
        styleImage = (ImageView) findViewById(R.id.styleImage);
        contentGallery = (Button) findViewById(R.id.contentGallery);
        contentCamera = (Button) findViewById(R.id.contentCamera);
        contentImage = (ImageView) findViewById(R.id.contentImage);
        startTrain = (Button) findViewById(R.id.startTrain);
        progressText = (TextView) findViewById(R.id.progressText);
        trainingProgress = (ProgressBar) findViewById(R.id.trainingProgress);
        resultImage = (ImageView) findViewById(R.id.resultImage);
        saveResult = (Button) findViewById(R.id.saveResult);
        basePath = getApplicationContext().getFilesDir().getAbsolutePath();

        // Page actions
        afterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (viewFlipper.getDisplayedChild()) {
                    case styleIndex: {
                        beforeButton.setEnabled(true);
                        if (contentImage.getVisibility() == View.GONE) { afterButton.setEnabled(false); }
                        viewFlipper.showNext();
                        break;
                    }
                    case contentIndex: {
                        afterButton.setText("처음부터");
                        if (resultImage.getVisibility() == View.GONE) { afterButton.setEnabled(false); }
                        viewFlipper.showNext();
                        break;
                    }
                    case resultIndex: { // reset
                        beforeButton.setEnabled(false);
                        afterButton.setEnabled(false);
                        afterButton.setText("다음");
                        styleImage.setVisibility(View.GONE);
                        contentImage.setVisibility(View.GONE);
                        startTrain.setVisibility(View.VISIBLE);
                        progressText.setVisibility(View.GONE);
                        progressText.setText("모델 실행중입니다...");
                        trainingProgress.setVisibility(View.GONE);
                        resultImage.setVisibility(View.GONE);
                        saveResult.setVisibility(View.GONE);
                        saveResult.setEnabled(true);
                        viewFlipper.showNext();
                        break;
                    }
                }
            }
        });
        beforeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (viewFlipper.getDisplayedChild()) {
                    case styleIndex: {
                        break;
                    }
                    case contentIndex: {
                        beforeButton.setEnabled(false);
                        if (styleImage.getVisibility() == View.VISIBLE) { afterButton.setEnabled(true); }
                        viewFlipper.showPrevious();
                        break;
                    }
                    case resultIndex: {
                        afterButton.setText("다음");
                        if (contentImage.getVisibility() == View.VISIBLE) { afterButton.setEnabled(true); }
                        viewFlipper.showPrevious();
                        break;
                    }
                }
            }
        });

        // Pick Images
        styleGallery.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, PICK_STYLE_GALLERY);
            }
        });
        styleCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(basePath + "/style.jpg");
                if (file.exists()) { file.delete(); }
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePictureIntent.putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        FileProvider.getUriForFile(
                                getApplicationContext(),
                                "com.flyai.StyleTransfer.fileprovider",
                                file
                ));
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            PICK_STYLE_CAMERA);
                }
                else {
                    startActivityForResult(takePictureIntent, PICK_STYLE_CAMERA);
                }
            }
        });
        contentGallery.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, PICK_CONTENT_GALLERY);
            }
        });
        contentCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(basePath +"/content.jpg");
                if (file.exists()) { file.delete(); }
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePictureIntent.putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        FileProvider.getUriForFile(
                                getApplicationContext(),
                                "com.flyai.StyleTransfer.fileprovider",
                                file
                        ));
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            PICK_CONTENT_CAMERA);
                }
                else {
                    startActivityForResult(takePictureIntent, PICK_CONTENT_CAMERA);
                }
            }
        });

        // Start Training
        startTrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTrain.setVisibility(View.GONE);
                progressText.setVisibility(View.VISIBLE);
                trainingProgress.setVisibility(View.VISIBLE);
                beforeButton.setEnabled(false);

                // load style and content images
                File styleFile = new File(basePath + "/style.jpg");
                File contentFile = new File(basePath + "/content.jpg");
                // set requestbody and multipartbody for upload
                RequestBody requestStyleBody = RequestBody.create(MediaType.parse("image/jpeg"), styleFile);
                MultipartBody.Part styleToUpload = MultipartBody.Part.createFormData(
                        "style_img",
                        styleFile.getName(),
                        requestStyleBody
                );
                RequestBody requestContentBody = RequestBody.create(MediaType.parse("image/jpeg"), contentFile);
                MultipartBody.Part contentToUpload = MultipartBody.Part.createFormData(
                        "user_img",
                        contentFile.getName(),
                        requestContentBody
                );
                ArrayList<MultipartBody.Part> images = new ArrayList<>();
                images.add(styleToUpload); images.add(contentToUpload);
                // call to upload
                Call<ResponseBody> call = RetrofitTest.getInstance().getService().ImageUpload(images);

                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.body() != null) {
                            InputStream is = response.body().byteStream();
                            Bitmap resultBitmap = BitmapFactory.decodeStream(is);
                            // save to file
                            File file = new File(basePath +"/result.jpg");
                            if (file.exists()) { file.delete(); }
                            try {
                                FileOutputStream out = new FileOutputStream(file);
                                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                out.flush();
                                out.close();
                            } catch (IOException e) { e.printStackTrace(); }
                            // set imageview
                            resultImage.setImageBitmap(resultBitmap);
                            resultImage.setVisibility(View.VISIBLE);
                            saveResult.setVisibility(View.VISIBLE);
                            progressText.setVisibility(View.GONE);
                            trainingProgress.setVisibility(View.GONE);
                            afterButton.setEnabled(true);
                        } else {
                            Log.d("Debug", "no body");
                            progressText.setText("오류가 발생했습니다. 처음부터 다시 시도해주세요.");
                            trainingProgress.setVisibility(View.GONE);
                            afterButton.setEnabled(true);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        t.printStackTrace();
                        Log.d("Debug", "onFaliure" + t.toString());
                        progressText.setText("오류가 발생했습니다. 처음부터 다시 시도해주세요.");
                        trainingProgress.setVisibility(View.GONE);
                        afterButton.setEnabled(true);
                    }
                });
            }
        });

        // after training, save the result to gallery
        saveResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
//                bmpFactoryOptions.inSampleSize = 1;
//                Bitmap selectedImage = BitmapFactory.decodeFile(basePath + "/result.jpg", bmpFactoryOptions);
                try {
                    MediaStore.Images.Media.insertImage(
                            getContentResolver(),
                            basePath + "/result.jpg",
                            "result",
                            "");
                    Toast.makeText(getApplicationContext(), "저장을 완료하였습니다!", Toast.LENGTH_LONG).show();
                    saveResult.setEnabled(false);
                } catch (FileNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "파일 저장에 실패하였습니다.", Toast.LENGTH_LONG).show();
                }
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        switch (requestCode) {
            case PICK_STYLE_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    File file = new File(basePath +"/style.jpg");
                    takePictureIntent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            FileProvider.getUriForFile(
                                    getApplicationContext(),
                                    "com.flyai.StyleTransfer.fileprovider",
                                    file
                            ));
                    startActivityForResult(takePictureIntent, requestCode);
                } else {
                    Toast.makeText(MainActivity.this, "권한을 허용해야 합니다", Toast.LENGTH_LONG).show();
                }
                break;
            case PICK_CONTENT_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    File file = new File(basePath +"/content.jpg");
                    takePictureIntent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            FileProvider.getUriForFile(
                                    getApplicationContext(),
                                    "com.flyai.StyleTransfer.fileprovider",
                                    file
                            ));
                    startActivityForResult(takePictureIntent, requestCode);
                } else {
                    Toast.makeText(MainActivity.this, "권한을 허용해야 합니다", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_STYLE_GALLERY: {
                try {
                    styleUri = data.getData();
                    InputStream imageStream = getContentResolver().openInputStream(styleUri);
                    Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    // save to file
                    File file = new File(basePath +"/style.jpg");
                    if (file.exists()) { file.delete(); }
                    FileOutputStream out = new FileOutputStream(file);
                    selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush(); out.close();
                    // rotate bitmap
                    selectedImage = rotateBitmap(basePath + "/style.jpg");
                    // set imageview
                    styleImage.setImageBitmap(selectedImage);
                    styleImage.setVisibility(View.VISIBLE);
                    afterButton.setEnabled(true);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "사진을 선택해주세요", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "오류가 발생했습니다", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case PICK_STYLE_CAMERA: {
                try {
                    // rotate bitmap
                    Bitmap selectedImage = rotateBitmap(basePath + "/style.jpg");
                    styleImage.setImageBitmap(selectedImage);
                    styleImage.setVisibility(View.VISIBLE);
                    afterButton.setEnabled(true);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "사진을 촬영해주세요", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "오류가 발생했습니다.", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case PICK_CONTENT_GALLERY: {
                try {
                    contentUri = data.getData();
                    InputStream imageStream = getContentResolver().openInputStream(contentUri);
                    Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    // save to file
                    File file = new File(basePath +"/content.jpg");
                    if (file.exists()) { file.delete(); }
                    FileOutputStream out = new FileOutputStream(file);
                    selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush(); out.close();
                    // rotate bitmap
                    selectedImage = rotateBitmap(basePath + "/content.jpg");
                    // set imageview
                    contentImage.setImageBitmap(selectedImage);
                    contentImage.setVisibility(View.VISIBLE);
                    afterButton.setEnabled(true);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "사진을 선택해주세요", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "오류가 발생했습니다", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case PICK_CONTENT_CAMERA: {
                try {
                    // rotate bitmap
                    Bitmap selectedImage = rotateBitmap(basePath + "/content.jpg");
                    contentImage.setImageBitmap(selectedImage);
                    contentImage.setVisibility(View.VISIBLE);
                    afterButton.setEnabled(true);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "사진을 촬영해주세요", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "오류가 발생했습니다.", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    public static Bitmap rotateBitmap(String path) throws IOException{
        // load bitmap from file path
        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inSampleSize = 1;
        Bitmap bitmap = BitmapFactory.decodeFile(path, bmpFactoryOptions);
        // get rotation information
        ExifInterface exif = new ExifInterface(path);
        int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
        );

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            // create rotated bitmap
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            // save rotated file
            File file = new File(path);
            if (file.exists()) { file.delete(); }
            FileOutputStream out = new FileOutputStream(file);
            out.flush(); out.close();
            // return rotated bitmap
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }
}