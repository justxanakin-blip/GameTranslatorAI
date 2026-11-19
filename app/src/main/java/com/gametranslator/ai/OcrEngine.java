package com.gametranslator.ai;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * ML Kit əsaslı OCR mühərriki (Java, əlavə asılılıq lazım deyil).
 *
 * İstifadə:
 *   OcrEngine ocr = new OcrEngine();
 *   ocr.recognize(bitmap, new OcrEngine.Callback() {
 *       public void onResult(List<OcrEngine.OcrLine> lines) { ... }
 *       public void onError(Exception e) { ... }
 *   });
 *   // Activity bağlananda:
 *   ocr.close();
 *
 * Callback əsas (UI) axınında çağırılır.
 */
public class OcrEngine implements AutoCloseable {

    /** Tapılmış mətn sətri və ekrandakı mövqeyi. */
    public static class OcrLine {
        public final String text;
        public final Rect box;
        public final float confidence;

        public OcrLine(String text, Rect box, float confidence) {
            this.text = text;
            this.box = box;
            this.confidence = confidence;
        }
    }

    public interface Callback {
        void onResult(List<OcrLine> lines);
        void onError(Exception e);
    }

    private final TextRecognizer recognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    public void recognize(Bitmap bitmap, Callback callback) {
        recognize(bitmap, 0, 0.4f, callback);
    }

    public void recognize(Bitmap bitmap, int rotationDegrees,
                          final float minConfidence, final Callback callback) {
        if (bitmap == null || bitmap.isRecycled()
                || bitmap.getWidth() < 2 || bitmap.getHeight() < 2) {
            callback.onResult(new ArrayList<OcrLine>());
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, rotationDegrees);

        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    List<OcrLine> lines = new ArrayList<>();
                    for (Text.TextBlock block : result.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            String text = line.getText().trim();
                            if (text.isEmpty()
                                    || line.getConfidence() < minConfidence) {
                                continue;
                            }
                            lines.add(new OcrLine(
                                    text, line.getBoundingBox(), line.getConfidence()));
                        }
                    }
                    callback.onResult(lines);
                })
                .addOnFailureListener(callback::onError);
    }

    /** Sətirləri tək mətn kimi birləşdirir (tərcümə üçün). */
    public static String joinLines(List<OcrLine> lines) {
        StringBuilder sb = new StringBuilder();
        for (OcrLine l : lines) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(l.text);
        }
        return sb.toString();
    }

    @Override
    public void close() {
        recognizer.close();
    }
}
 