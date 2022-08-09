package com.flyai.StyleTransfer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class MainActivity extends AppCompatActivity {
    // Elements for flip pages
    ViewFlipper viewFlipper;
    Button afterButton;
    Button beforeButton;
    // Elements for style
    Button styleGallery;
    Button styleCamera;
    ImageView styleImage;
    // Elements for content
    Button contentGallery;
    Button contentCamera;
    ImageView contentImage;
    // Elements for result
    TextView progressText;
    ImageView resultImage;
    Button saveResult;



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
        progressText = (TextView) findViewById(R.id.progressText);
        resultImage = (ImageView) findViewById(R.id.resultImage);
        saveResult = (Button) findViewById(R.id.saveResult);

        afterButton.setOnClickListener(
                view -> viewFlipper.showNext()
        );
        beforeButton.setOnClickListener(
                view -> viewFlipper.showPrevious()
        );
    }
}