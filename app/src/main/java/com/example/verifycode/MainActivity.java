package com.example.verifycode;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.slidercaptcha.SliderCaptchaView;

import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final SliderCaptchaView ccv = findViewById(R.id.ccv);
        final SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setMax(100);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ccv.setDragOffset(progress-10);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                ccv.startDragSlider();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ccv.stopDragSlider();
            }
        });

        ccv.setCaptchaDragListener(new SliderCaptchaView.CaptchaDragListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onVerifySuccess(long interval) {
                seekBar.setEnabled(false);
            }

            @Override
            public void onVerifyFailure() {
                seekBar.setProgress(0);
                Toast.makeText(MainActivity.this, "验证失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReload(SliderCaptchaView slider) {
                seekBar.setProgress(0);
                ccv.setImageResource(R.mipmap.kobe);
                ccv.createNewCaptcha();
                Toast.makeText(MainActivity.this, "重新载入", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
