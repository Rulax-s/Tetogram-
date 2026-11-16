/* Tetogram, 2026. GNU GPL v2 or later. Local experimental text-to-voice composer. */
package org.telegram.ui.tetogram;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.tetogram.VoiceEmotion;

/**
 * Stage 1: real offline Android TTS -> Ogg/Opus -> native Telegram voice-send callback.
 * Teto/Miku are EXPERIMENTAL pitch/rate profiles, NOT authentic trained character models.
 * Does not request INTERNET or an external endpoint. Real character packs are not bundled.
 */
public final class VoiceComposer {
    private VoiceComposer() {}

    public interface OnSend {
        void send(File ogg, double durationSeconds, String sourceText);
    }

    public static void show(Activity activity, String input, OnSend sender) {
        if (activity == null || activity.isFinishing()) return;
        if (Build.VERSION.SDK_INT < 29) {
            new AlertDialog.Builder(activity).setMessage("Для локального OGG/Opus нужен Android 10 или новее.").setPositiveButton("OK", null).show();
            return;
        }
        new Session(activity, input, sender).show();
    }

    private static final class Session {
        private final Activity activity;
        private final OnSend sender;
        private final EditText editor;
        private final Spinner character;
        private final Spinner quality;
        private final TextView status;
        private final ProgressBar spinner;
        private final String token = Long.toHexString(System.nanoTime());
        private AlertDialog dialog;
        private TextToSpeech engine;
        private MediaPlayer player;
        private File wav, ogg;
        private VoiceOggEncoder.Result encoded;
        private String generatedSource, generatedOptions;
        private boolean ready, busy, closed, sent;
        private boolean sendWhenReady;
        private int requestId;

        Session(Activity activity, String input, OnSend sender) {
            this.activity=activity;this.sender=sender;
            LinearLayout root=new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            int margin=dp(activity,18);
            root.setPadding(margin,dp(activity,12),margin,dp(activity,8));
            editor=new EditText(activity);
            editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            editor.setSingleLine(false);
            editor.setMinLines(2);editor.setMaxLines(6);
            editor.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
            editor.setText(input);
            root.addView(editor,new LinearLayout.LayoutParams(-1,-2));
            label(root,"Персонаж (экспериментальный тембр)");
            character=menu(root,new String[]{"Касане Тето · тестовый тембр","Хацунэ Мику · тестовый тембр"});
            label(root,"Режим синтеза");
            quality=menu(root,new String[]{"Лёгкий · офлайн","Качественный · офлайн, если доступен"});
            TextView note=label(root,"😊 😡 😢 🤩 🤫 📢 😂 — первая управляющая эмоция задаёт интонацию всего сообщения.\nНастоящие голосовые модели Тето и Мику пока не установлены: используются офлайн-голоса Android.");
            note.setTextSize(12);
            spinner=new ProgressBar(activity);spinner.setVisibility(View.GONE);
            LinearLayout.LayoutParams progressLayout=new LinearLayout.LayoutParams(dp(activity,32),dp(activity,32));progressLayout.gravity=Gravity.CENTER_HORIZONTAL;
            root.addView(spinner,progressLayout);
            status=label(root,"Проверка офлайн-синтезатора русского языка…");
            status.setTextSize(13);
            dialog=new AlertDialog.Builder(activity)
                .setTitle("Озвучить сообщение")
                .setView(root)
                .setPositiveButton("Отправить",null)
                .setNeutralButton("Прослушать",null)
                .setNegativeButton("Отмена",(d,w)->{})
                .create();
            dialog.setOnDismissListener(d->cleanup());
            dialog.setOnShowListener(d->{
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->process(true));
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->process(false));
                updateButtons();
            });
        }
        private static int dp(Context context,int value){return (int)(value*context.getResources().getDisplayMetrics().density+.5f);}
        private TextView label(LinearLayout root,String txt){TextView t=new TextView(activity);t.setText(txt);t.setPadding(0,dp(activity,8),0,dp(activity,3));root.addView(t,new LinearLayout.LayoutParams(-1,-2));return t;}
        private Spinner menu(LinearLayout root,String[] choices){Spinner s=new Spinner(activity);ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,choices);s.setAdapter(adapter);root.addView(s,new LinearLayout.LayoutParams(-1,dp(activity,48)));return s;}
        void show(){
            dialog.show();
            try {
                engine=new TextToSpeech(activity.getApplicationContext(),success->activity.runOnUiThread(()->{
                    if(closed)return;
                    ready=success==TextToSpeech.SUCCESS && engine!=null;
                    status.setText(ready?"Только локальный синтез. Подготовьте русский офлайн-голос в настройках Android.":"Синтезатор Android недоступен. Установите офлайн TTS.");
                    updateButtons();
                }));
            } catch (Exception e) {status.setText("Не удалось открыть локальный TTS.");}
        }
        private void updateButtons(){
            if(dialog==null||!dialog.isShowing())return;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(ready&&!busy);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(ready&&!busy);
            spinner.setVisibility(busy?View.VISIBLE:View.GONE);
        }
        private String options(){return character.getSelectedItemPosition()+"/"+quality.getSelectedItemPosition();}
        private Voice offlineRussianVoice(){
            if(engine==null)return null;
            Set<Voice> voices=engine.getVoices();
            if(voices==null)return null;
            ArrayList<Voice> candidates=new ArrayList<>();
            for(Voice voice:voices){
                Locale locale=voice.getLocale();
                if(locale!=null&&"ru".equals(locale.getLanguage())&&!voice.isNetworkConnectionRequired())candidates.add(voice);
            }
            if(candidates.isEmpty())return null;
            if(quality.getSelectedItemPosition()==1)candidates.sort(Comparator.comparingInt(Voice::getQuality).reversed());
            else candidates.sort(Comparator.comparingInt(Voice::getLatency));
            return candidates.get(0);
        }
        private void process(boolean send){
            if(!ready||busy||closed)return;
            String current=editor.getText().toString().trim();
            VoiceEmotion.Parsed parsed=VoiceEmotion.parse(current);
            if(parsed.text.isEmpty()){status.setText("Введите текст для озвучивания.");return;}
            if(parsed.text.length()>TextToSpeech.getMaxSpeechInputLength()){status.setText("Слишком длинный текст.");return;}
            if(encoded!=null&&current.equals(generatedSource)&&options().equals(generatedOptions)){
                if(send)sendNow();else play();return;
            }
            Voice voice=offlineRussianVoice();
            if(voice==null){status.setText("Нет установленного русского офлайн-голоса. Скачайте его в настройках синтеза речи Android.");return;}
            int request=++requestId;
            disposeAudio();busy=true;sendWhenReady=send;updateButtons();
            generatedSource=current;generatedOptions=options();
            int which=character.getSelectedItemPosition();
            float basePitch=which==0?1.11f:1.30f;
            float baseSpeed=which==0?1.02f:1.08f;
            status.setText("Генерация голосового без отправки текста в интернет…");
            try {
                wav=File.createTempFile("tg_tts_", ".wav", activity.getCacheDir());
                int selected=engine.setVoice(voice);
                if(selected!=TextToSpeech.SUCCESS)throw new IllegalStateException("Голос TTS недоступен");
                engine.setPitch(Math.min(1.6f,basePitch*VoiceEmotion.pitch(parsed.emotion)));
                engine.setSpeechRate(Math.min(1.6f,baseSpeed*VoiceEmotion.speed(parsed.emotion)));
                engine.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                    @Override public void onStart(String id){}
                    @Override public void onDone(String id){if(!id.equals(token+"-"+request))return;encodeOnWorker(request);}
                    @Override public void onError(String id){failure(request,"Ошибка синтеза. Проверьте выбранный офлайн-голос.");}
                    @Override public void onError(String id,int code){failure(request,"Синтез не удался ("+code+").");}
                });
                int queued=engine.synthesizeToFile(parsed.text,new Bundle(),wav,token+"-"+request);
                if(queued!=TextToSpeech.SUCCESS)throw new IllegalStateException("TTS queue rejected");
            } catch(Exception e){failure(request,"Не удалось запустить локальную озвучку: "+e.getMessage());}
        }
        private void encodeOnWorker(int request){
            new Thread(()->{
                File output=null;
                try {
                    File directory=FileLoader.getDirectory(FileLoader.MEDIA_DIR_AUDIO);
                    if(directory==null||!directory.isDirectory())directory=activity.getCacheDir();
                    output=File.createTempFile("tg_local_voice_", ".ogg", directory);
                    final File completed=output;
                    VoiceOggEncoder.Result result=VoiceOggEncoder.encode(wav,completed);
                    activity.runOnUiThread(()->{
                        if(closed||request!=requestId){completed.delete();return;}
                        ogg=completed;encoded=result;busy=false;
                        status.setText("Готово. Обычное голосовое Telegram (OGG/Opus). Тембр персонажа пока приблизительный.");
                        updateButtons();
                        if(sendWhenReady)sendNow();else play();
                    });
                } catch(Exception e){if(output!=null)output.delete();failure(request,"Не удалось кодировать OGG/Opus: "+e.getMessage());}
            },"TetogramLocalVoiceEncode").start();
        }
        private void failure(int request,String text){
            activity.runOnUiThread(()->{
                if(closed||request!=requestId)return;
                busy=false;status.setText(text);updateButtons();
            });
        }
        private void play(){
            if(closed||wav==null||!wav.isFile())return;
            try {
                if(player!=null){player.release();player=null;}
                player=new MediaPlayer();
                player.setDataSource(wav.getAbsolutePath());
                player.prepare();player.start();
            }catch(Exception e){status.setText("Не удалось воспроизвести предпросмотр: "+e.getMessage());}
        }
        private void sendNow(){
            if(encoded==null||closed||busy)return;
            if(!editor.getText().toString().trim().equals(generatedSource)||!options().equals(generatedOptions)){
                status.setText("Текст или настройки изменились. Нажмите отправить ещё раз для новой генерации.");return;
            }
            try{
                sender.send(encoded.file,encoded.durationSeconds,generatedSource);
                sent=true;dialog.dismiss();
            }catch(Exception e){status.setText("Отправка не выполнена: "+e.getMessage());}
        }
        private void disposeAudio(){
            if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}
            if(wav!=null)wav.delete();wav=null;
            if(ogg!=null&&!sent)ogg.delete();ogg=null;encoded=null;
        }
        private void cleanup(){closed=true;requestId++;if(engine!=null){engine.stop();engine.shutdown();engine=null;}disposeAudio();}
    }
}
