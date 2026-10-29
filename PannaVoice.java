package com.harpreet.panna;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

/** Streams bounded chunks and ignores stale callbacks after Stop or a lifecycle change. */
public final class PannaVoice {
    public interface Feedback { void show(String message); }
    private final Activity activity;
    private final Feedback feedback;
    private TextToSpeech engine;
    private boolean ready, active, closed;
    private String text = "";
    private int cursor, generation;
    private Locale language = Locale.getDefault();
    private float rate = 1f;
    public PannaVoice(Activity activity, Feedback feedback) { this.activity = activity; this.feedback = feedback; }
    public void start(String text, int offset, Locale language, float rate) {
        if (closed) return;
        stop(); this.text = text; this.cursor = PannaText.clamp(offset, 0, text.length());
        this.language = language; this.rate = rate;
        if (cursor >= text.length()) { feedback.show("You are at the end. Start again to listen from the beginning."); return; }
        active = true;
        if (engine == null) {
            engine = new TextToSpeech(activity, status -> activity.runOnUiThread(() -> {
                if (closed) return;
                ready = status == TextToSpeech.SUCCESS;
                if (!ready) { active = false; engine.shutdown(); engine = null; feedback.show("No working text-to-speech engine. Enable a voice engine in Android settings, then reopen Panna Reader."); return; }
                engine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) {}
                    @Override public void onDone(String id) { activity.runOnUiThread(() -> finished(id)); }
                    @Override public void onError(String id) { activity.runOnUiThread(() -> failed(id)); }
                });
                if (active) configure();
            }));
        } else if (ready) configure();
        else { active = false; feedback.show("Voice engine is initializing. Please try again shortly, or check Android voice settings."); }
    }
    private void configure() {
        int result = engine.setLanguage(language);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            active = false; feedback.show("This voice language is unavailable. Install its voice data in Android settings or choose another language."); return;
        }
        engine.setSpeechRate(rate); queue();
    }
    private void queue() {
        if (!active || closed) return;
        if (cursor >= text.length()) { active = false; text = ""; return; }
        int end = PannaText.speechChunkEnd(text, cursor, Math.min(3000, TextToSpeech.getMaxSpeechInputLength()));
        String id = generation + ":" + end;
        if (engine.speak(text.substring(cursor, end), TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR) failed(id);
    }
    private boolean current(String id) { return active && !closed && id != null && id.startsWith(generation + ":"); }
    private void finished(String id) {
        if (!current(id)) return;
        try { cursor = Integer.parseInt(id.substring(id.indexOf(':') + 1)); queue(); }
        catch (NumberFormatException e) { failed(id); }
    }
    private void failed(String id) {
        if (!current(id)) return;
        stop(); feedback.show("Read Aloud could not continue. Check your installed voice data and engine settings.");
    }
    public void stop() {
        active = false; generation++; text = "";
        if (engine != null) engine.stop();
    }
    public void close() { stop(); closed = true; if (engine != null) engine.shutdown(); }
}
