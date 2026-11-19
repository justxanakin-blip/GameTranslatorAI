package  gametranslator.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;

public class CaptureService extends Service {
    private MediaProjection projection;
    private ImageReader reader;
    private VirtualDisplay vDisplay;
    private volatile boolean wantCapture = false;
    private int w, h;
    private TextRecognizer recognizer;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public IBinder onBind(Intent i) { return null; }

    @SuppressWarnings("deprecation")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if ("CAPTURE".equals(action)) { wantCapture = true; return START_NOT_STICKY; }
        if ("STOP".equals(action)) { stopSelf(); return START_NOT_STICKY; }

        startForegroundCompat();

        int code = intent.getIntExtra("code", 0);
        Intent data = intent.getParcelableExtra("data");

        MediaProjectionManager mpm =
            (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        projection = mpm.getMediaProjection(code, data);
        projection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() { stopSelf(); }
        }, main);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        TrHelper.prepare(r -> {
            if (!"ok".equals(r)) toast("Çeviri modeli indirilemedi");
        });
        setupDisplay();
        return START_NOT_STICKY;
    }

    private void startForegroundCompat() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                "cap", "Capture", NotificationManager.IMPORTANCE_LOW));
        }
        Intent ci = new Intent(this, CaptureService.class).setAction("CAPTURE");
        PendingIntent pi = PendingIntent.getService(this, 0, ci,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new NotificationCompat.Builder(this, "cap")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Oyun tərcüməçisi")
            .addAction(0, "Tərcümə et", pi)
            .setOngoing(true)
            .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(1, n);
        }
    }

    private void setupDisplay() {
        DisplayMetrics m = getResources().getDisplayMetrics();
        w = m.widthPixels;
        h = m.heightPixels;
        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);
        vDisplay = projection.createVirtualDisplay("cap", w, h, m.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.getSurface(), null, null);

        reader.setOnImageAvailableListener(r -> {
            Image img = r.acquireLatestImage();
            if (img == null) return;
            if (!wantCapture) { img.close(); return; }
            wantCapture = false;
            Bitmap bmp = toBitmap(img);
            img.close();
            runOcr(bmp);
        }, main);
    }

    private Bitmap toBitmap(Image img) {
        Image.Plane p = img.getPlanes()[0];
        ByteBuffer buf = p.getBuffer();
        int pixelStride = p.getPixelStride();
        int rowPad = p.getRowStride() - pixelStride * w;
        Bitmap full = Bitmap.createBitmap(w + rowPad / pixelStride, h,
            Bitmap.Config.ARGB_8888);
        full.copyPixelsFromBuffer(buf);
        return Bitmap.createBitmap(full, 0, 0, w, h);
    }

    private void runOcr(Bitmap bmp) {
        recognizer.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener(result -> {
                String text = result.getText().trim();
                if (text.isEmpty()) { toast("Mətn tapılmadı"); return; }
                String joined = text.replace("\n", " ");
                Log.d("OCR", joined);
                TrHelper.translate(joined, tr -> {
                    Log.d("TR", tr);
                    toast(tr);
                });
            })
            .addOnFailureListener(e -> toast("OCR xətası: " + e.getMessage()));
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        if (vDisplay != null) vDisplay.release();
        if (reader != null) reader.close();
        if (projection != null) projection.stop();
        super.onDestroy();
    }
}