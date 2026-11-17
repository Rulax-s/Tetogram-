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
import android.widget.CheckBox;
import android.widget.Button;
import android.content.Intent;
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
        private final CheckBox rvcToggle;
        private final android.widget.SeekBar pitchSlider;
        private final TextView pitchLabel;
        private final Button sourcePreview;
        private final Button rvcModels;
        private volatile boolean rvcRequested;
        private volatile int rvcCharacter;
        private volatile int rvcTranspose;
        private File rvcWav;
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
            label(root,"Персонаж / модель голоса");
            character=menu(root,new String[]{"Касане Тето · UTAU (старая)","Хацунэ Мику","Касане Тето · SynthV (новая)"});
            label(root,"Режим синтеза");
            quality=menu(root,new String[]{"Лёгкий · офлайн","Качественный · офлайн, если доступен"});
            rvcToggle = new CheckBox(activity);
            rvcToggle.setText("Настоящий голос RVC (локально · экспериментально)");
            rvcToggle.setChecked(false);
            root.addView(rvcToggle, new LinearLayout.LayoutParams(-1,-2));
            pitchLabel = label(root,"Высота RVC: 0 полутонов");
            pitchSlider = new android.widget.SeekBar(activity);
            pitchSlider.setMax(24);
            pitchSlider.setProgress(12);
            pitchSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(android.widget.SeekBar b,int p,boolean user) {
                    int semitones=p-12;
                    pitchLabel.setText("Высота RVC: " + (semitones>0?"+":"") + semitones + " полутонов");
                }
                @Override public void onStartTrackingTouch(android.widget.SeekBar b) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar b) {}
            });
            root.addView(pitchSlider,new LinearLayout.LayoutParams(-1,-2));
            rvcModels = new Button(activity);
            rvcModels.setText("Установить модели RVC");
            rvcModels.setOnClickListener(v -> activity.startActivity(new Intent(activity, RvcModelManagerActivity.class)));
            root.addView(rvcModels, new LinearLayout.LayoutParams(-1,-2));
            sourcePreview=new Button(activity);
            sourcePreview.setText("Слушать исходный TTS (сравнение)");
            sourcePreview.setOnClickListener(v -> playOriginal());
            root.addView(sourcePreview,new LinearLayout.LayoutParams(-1,-2));
            TextView note=label(root,"😊 😡 😢 🤩 🤫 📢 😂 — первая управляющая эмоция задаёт интонацию всего сообщения.\nРежим RVC работает только с установленными ONNX-пакетами. Отключите его для обычного Android TTS.");
            note.setTextSize(12);
            spinner=new ProgressBar(activity);spinner.setVisibility(View.GONE);
            LinearLayout.LayoutParams progressLayout=new LinearLayout.LayoutParams(dp(activity,32),dp(activity,32));progressLayout.gravity=Gravity.CENTER_HORIZONTAL;
            root.addView(spinner,progressLayout);
            status=label(root,"Проверка офлайн-синтезатора русского языка…");
            rvcToggle.setOnCheckedChangeListener((button, checked) -> {
                status.setText(checked ? RvcModelStore.description(activity,character.getSelectedItemPosition()) : "Режим Android TTS без преобразования RVC");
            });
            status.setTextSize(13);
            android.widget.ScrollView scroll=new android.widget.ScrollView(activity);
            scroll.addView(root);
            dialog=new AlertDialog.Builder(activity)
                .setTitle("Озвучить сообщение")
                .setView(scroll)
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
            rvcToggle.setEnabled(!busy);
            rvcModels.setEnabled(!busy);
            pitchSlider.setEnabled(!busy);
            character.setEnabled(!busy);
            quality.setEnabled(!busy);
            sourcePreview.setEnabled(!busy && wav!=null && wav.isFile());
        }
        private String options(){return rvcToggle.isChecked()+"/"+character.getSelectedItemPosition()+"/"+quality.getSelectedItemPosition()+"/"+pitchSlider.getProgress();}
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
            if(rvcToggle.isChecked() && !RvcModelStore.available(activity,character.getSelectedItemPosition())) {
                status.setText("Для RVC сначала установите HuBERT, RMVPE и ONNX-модель Тето/Мику.");return;
            }
            Voice voice=offlineRussianVoice();
            if(voice==null){status.setText("Нет установленного русского офлайн-голоса. Скачайте его в настройках синтеза речи Android.");return;}
            rvcRequested=rvcToggle.isChecked();
            rvcCharacter=character.getSelectedItemPosition();
            rvcTranspose=pitchSlider.getProgress()-12;
            int request=++requestId;
            disposeAudio();busy=true;sendWhenReady=send;updateButtons();
            generatedSource=current;generatedOptions=options();
            int which=character.getSelectedItemPosition();
            float basePitch=which==1?1.30f:1.11f;
            float baseSpeed=which==1?1.08f:1.02f;
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
                    File inputAudio = wav;
                    if(rvcRequested){
                        rvcWav=File.createTempFile("tetogram_rvc_", ".wav", activity.getCacheDir());
                        LocalRvcEngine.convert(activity.getApplicationContext(), wav, rvcWav, rvcCharacter, rvcTranspose);
                        inputAudio=rvcWav;
                    }
                    VoiceOggEncoder.Result result=VoiceOggEncoder.encode(inputAudio,completed);
                    activity.runOnUiThread(()->{
                        if(closed||request!=requestId){completed.delete();return;}
                        ogg=completed;encoded=result;busy=false;
                        status.setText(rvcRequested ? "Готово: голосовое RVC (OGG/Opus). Можно прослушать или отправить." : "Готово: стандартный Android TTS (OGG/Opus). Можно прослушать или отправить.");
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
        private void playOriginal(){
            File original=wav;
            if (original==null || !original.isFile()) {
                status.setText("Сначала сгенерируйте сообщение, чтобы сравнить исходный голос.");
                return;
            }
            playFile(original);
        }
        private void play(){
            File preview=(rvcRequested&&rvcWav!=null&&rvcWav.isFile())?rvcWav:wav;
            playFile(preview);
        }
        private void playFile(File preview){
            if(closed||preview==null||!preview.isFile())return;
            try {
                if(player!=null){player.release();player=null;}
                player=new MediaPlayer();
                player.setDataSource(preview.getAbsolutePath());
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
            if(rvcWav!=null)rvcWav.delete();rvcWav=null;
            if(ogg!=null&&!sent)ogg.delete();ogg=null;encoded=null;
        }
        private void cleanup(){closed=true;requestId++;if(engine!=null){engine.stop();engine.shutdown();engine=null;}disposeAudio();}
    }
}
