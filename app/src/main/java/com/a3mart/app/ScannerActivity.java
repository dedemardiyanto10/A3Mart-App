package com.a3mart.app;

import android.content.Intent;
import androidx.camera.core.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.a3mart.app.databinding.ActivityScannerBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerActivity extends AppCompatActivity {

    private ActivityScannerBinding binding;
    private ExecutorService cameraExecutor;
    private boolean isScanned = false;
    private static final int RESULT_DELAY_MS = 600;

    private Camera camera;
    private boolean torchEnabled = false;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    private ToneGenerator toneGen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        binding = ActivityScannerBinding.inflate(getLayoutInflater());
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setDecorFitsSystemWindows(false);
        setContentView(binding.getRoot());

        toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        cameraExecutor = Executors.newSingleThreadExecutor();
        startCamera();

        ViewCompat.setOnApplyWindowInsetsListener(
                binding.getRoot(),
                (v, insets) -> {
                    Insets statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars());

                    // Geser tombol BACK ke bawah status bar
                    binding.btnBack.setTranslationY(statusBar.top);

                    return insets;
                });

        binding.btnBack.setOnClickListener(
                v -> {
                    // hentikan scan
                    isScanned = true;

                    // kembalikan tanpa hasil
                    setResult(RESULT_CANCELED);
                    finish();
                });

        binding.btnFlash.setOnClickListener(
                v -> {
                    if (camera == null) return;

                    torchEnabled = !torchEnabled;
                    camera.getCameraControl().enableTorch(torchEnabled);

                    binding.btnFlash.setIconResource(
                            torchEnabled ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
                });

        binding.btnSwitch.setOnClickListener(
                v -> {
                    torchEnabled = false;
                    if (camera != null) {
                        camera.getCameraControl().enableTorch(false);
                    }
                    binding.btnFlash.setIconResource(R.drawable.ic_flash_off);

                    cameraSelector =
                            (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                                    ? CameraSelector.DEFAULT_FRONT_CAMERA
                                    : CameraSelector.DEFAULT_BACK_CAMERA;

                    startCamera();
                });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(this);

        providerFuture.addListener(
                () -> {
                    try {
                        ProcessCameraProvider provider = providerFuture.get();

                        Preview preview = new Preview.Builder().build();
                        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                        ImageAnalysis analysis =
                                new ImageAnalysis.Builder()
                                        .setBackpressureStrategy(
                                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build();

                        analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                        provider.unbindAll();

                        camera = provider.bindToLifecycle(this, cameraSelector, preview, analysis);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(@NonNull ImageProxy proxy) {
        if (isScanned || proxy.getImage() == null) {
            proxy.close();
            return;
        }

        InputImage image =
                InputImage.fromMediaImage(
                        proxy.getImage(), proxy.getImageInfo().getRotationDegrees());

        BarcodeScanning.getClient()
                .process(image)
                .addOnSuccessListener(
                        barcodes -> {
                            if (barcodes.isEmpty()) {
                                binding.overlayView.setHint("Arahkan barcode ke kotak");
                            } else {
                                Barcode barcode = barcodes.get(0);

                                isScanned = true;
                                binding.overlayView.setHint("Berhasil!");
                                playBeep();

                                new Handler(Looper.getMainLooper())
                                        .postDelayed(
                                                () -> {
                                                    Intent i = new Intent();
                                                    i.putExtra("result", barcode.getRawValue());
                                                    setResult(RESULT_OK, i);
                                                    finish();
                                                },
                                                300);
                            }
                        })
                .addOnCompleteListener(t -> proxy.close());
    }

    private void playBeep() {
        if (toneGen != null) {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
