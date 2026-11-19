
package com.gametranslator.ai;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.*;
import android.graphics.Color;
import android.view.Gravity;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    TextView result, status;
    Spinner language;
    Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 45, 30, 30);
        layout.setBackgroundColor(Color.rgb(15, 18, 28));

        TextView title = new TextView(this);
        title.setText("GAME TRANSLATOR AI");
        title.setTextSize(25);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        status = new TextView(this);
        status.setText("\nReady to translate");
        status.setTextColor(Color.GREEN);
        status.setTextSize(16);
        layout.addView(status);

        TextView label = new TextView(this);
        label.setText("\nTranslation language:");
        label.setTextColor(Color.WHITE);
        layout.addView(label);

        language = new Spinner(this);
        String[] languages = {
            "Azərbaycan dili",
            "Türk dili"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages
        );

        language.setAdapter(adapter);
        layout.addView(language);

        startButton = new Button(this);
        startButton.setText("START LIVE SUBTITLES");
        layout.addView(startButton);

        result = new TextView(this);
        result.setText("\nSubtitles will appear here...");
        result.setTextColor(Color.WHITE);
        result.setTextSize(19);
        result.setPadding(15, 35, 15, 35);
        layout.addView(result);

        startButton.setOnClickListener(v -> startListening());

        setContentView(layout);
    }

    private void startListening() {
        try {
            Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            );

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.ENGLISH.toLanguageTag()
            );

            intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Listening..."
            );

            startActivityForResult(intent, 100);

        } catch (Exception e) {
            status.setText("Speech recognition unavailable");
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK
            && data != null) {

            ArrayList<String> matches =
                data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                );

            if (matches != null && !matches.isEmpty()) {
                result.setText(matches.get(0));
                status.setText("Speech recognized!");
            }
        }
    }
}
