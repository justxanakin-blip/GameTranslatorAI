package com.gametranslator.ai;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public class TrHelper {
    public interface Callback { void onResult(String text); }

    private static final Translator translator = Translation.getClient(
        new TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.TURKISH)
            .build());

    private static volatile boolean ready = false;

    public static void prepare(Callback cb) {
        translator.downloadModelIfNeeded(new DownloadConditions.Builder().build())
            .addOnSuccessListener(v -> { ready = true; cb.onResult("ok"); })
            .addOnFailureListener(e -> cb.onResult("fail"));
    }

    public static void translate(String text, Callback cb) {
        if (!ready) { cb.onResult("Model henüz hazır değil"); return; }
        translator.translate(text)
            .addOnSuccessListener(cb::onResult)
            .addOnFailureListener(e -> cb.onResult("Çeviri hatası: " + e.getMessage()));
    }
}