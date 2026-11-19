package com.gametranslator.ai;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.*;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;

public class CaptureService extends Service {
    private MediaProjection projection;
    private AudioRecord rec;
    private volatile boolean running;
    private Translator translator;
    private WindowManager wm;
    private TextView sub;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {
        NotificationChannel ch = new NotificationChannel("cap", "Altyazı",
                NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
        Notification n = new Notification.Builder(this, "cap")
                .setContentTitle("Canlı altyazı işləyir")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);

        MediaProjectionManager mpm =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        projection = mpm.getMediaProjection(
                intent.getIntExtra("code", 0), intent.getParcelableExtra("data"));

        setupOverlay();
        setupTranslator();
        StorageService.unpack(this, "model-en", "model",
                model -> startListening(model),
                e -> show("Model xətası: " + e.getMessage()));
        return START_NOT_STICKY;
    }

    private void setupOverlay() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        sub = new TextView(this);
        sub.setTextColor(Color.WHITE);
        sub.setTextSize(18);
        sub.setGravity(Gravity.CENTER);
        sub.setBackgroundColor(0x99000000);
        sub.setPadding(24, 12, 24, 12);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM;
        lp.y = 60;
        wm.addView(sub, lp);
    }

    private void setupTranslator() {
        translator = Translation.getClient(new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.TURKISH).build());
        translator.downloadModelIfNeeded(new DownloadConditions.Builder().build())
                .addOnFailureListener(e -> show("Tərcümə modeli yüklənmədi"));
    }

    private void startListening(Model model) {
        AudioPlaybackCaptureConfiguration cfg =
                new AudioPlaybackCaptureConfiguration.Builder(projection)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .build();
        AudioFormat fmt = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(16000)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
        rec = new AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(cfg)
                .setAudioFormat(fmt)
                .setBufferSizeInBytes(16000 * 2).build();

        running = true;
        new Thread(() -> {
            try {
                Recognizer r = new Recognizer(model, 16000f);
                byte[] buf = new byte[4096];
                String lastPartial = "";
                rec.startRecording();
                while (running) {
                    int n = rec.read(buf, 0, buf.length);
                    if (n <= 0) continue;
                    if (r.acceptWaveForm(buf, n)) {
                        String t = new JSONObject(r.getResult()).optString("text");
                        if (!t.isEmpty()) translate(t);
                        lastPartial = "";
                    } else {
                        String p = new JSONObject(r.getPartialResult()).optString("partial");
                        if (!p.isEmpty() && !p.equals(lastPartial)) {
                            lastPartial = p;
                            translate(p);
                        }
                    }
                }
            } catch (Exception e) {
                show("Xəta: " + e.getMessage());
            }
        }).start();
    }

    private void translate(String en) {
        translator.translate(en).addOnSuccessListener(this::show);
    }

    private void show(String text) {
        ui.post(() -> {
            sub.setText(text);
            sub.removeCallbacks(clear);
            sub.postDelayed(clear, 4000);
        });
    }
    private final Runnable clear = () -> sub.setText("");

    @Override
    public void onDestroy() {
        running = false;
        if (rec != null) rec.release();
        if (projection != null) projection.stop();
        if (sub != null) wm.removeView(sub);
        super.onDestroy();
    }
}
 