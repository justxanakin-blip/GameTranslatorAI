
package com.gametranslator.ai;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    TextView result, status;
    Spinner language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);
        layout.setPadding(30, 40, 30, 30);
        layout.setBackgroundColor(Color.rgb(15, 18, 28));

        TextView title = new TextView(this);
        title.setText("GAME TRANSLATOR AI");
        title.setTextSize(25);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        status = new TextView(this);
        status.setText("READY");
        status.setTextColor(Color.GREEN);
        layout.addView(status);

        language = new Spinner(this);
        String[] langs = {"Azərbaycan dili", "Türk dili"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            langs
        );

        language.setAdapter(adapter);
        layout.addView(language);

        Button start = new Button(this);
        start.setText("START LIVE SUBTITLES");
        layout.addView(start);

        result = new TextView(this);
        result.setText("Subtitles will appear here...");
        result.setTextSize(20);
        result.setTextColor(Color.WHITE);
        result.setPadding(15, 40, 15, 40);
        layout.addView(result);

        start.setOnClickListener(v -> listen());

        setContentView(layout);
    }

    void listen() {
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

        try {
            startActivityForResult(intent, 100);
        } catch (Exception e) {
            status.setText("Speech unavailable");
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 &&
            resultCode == RESULT_OK && data != null) {

            ArrayList<String> matches =
                data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                );

            if (matches != null && !matches.isEmpty()) {
                result.setText(matches.get(0));
                status.setText("Recognized!");
            }
        }
    }
}
