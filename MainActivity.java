
package com.gametranslator.ai;

import android.app.*;
import android.os.*;
import android.content.*;
import android.media.projection.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class MainActivity extends Activity {

    private TextView result;
    private MediaProjectionManager projectionManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        projectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 40, 30, 30);

        TextView title = new TextView(this);
        title.setText("GAME TRANSLATOR AI");
        title.setTextSize(24);

        Button scan = new Button(this);
        scan.setText("EKRANI OXU");

        result = new TextView(this);
        result.setTextSize(18);

        layout.addView(title);
        layout.addView(scan);
        layout.addView(result);

        setContentView(layout);

        scan.setOnClickListener(v ->
            startActivityForResult(
                projectionManager.createScreenCaptureIntent(), 100
            )
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            result.setText("Ekran icazəsi alındı. OCR sistemi hazırlanır...");
        }
    }
}
