/* Tetogram. GPL-2.0-or-later. Safe on-device RVC model import screen. */
package org.telegram.ui.tetogram;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;

/** Explicit user interaction through Storage Access Framework. No storage permission. */
public final class RvcModelManagerActivity extends Activity {
    private TextView[] labels = new TextView[4];
    private Button[] buttons = new Button[4];
    private boolean copying;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroller = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density + .5f);
        root.setPadding(pad, pad, pad, pad);
        TextView intro = new TextView(this);
        intro.setText("RVC · локальные ONNX-модели\n\nЗагрузите HuBERT, RMVPE и модели синтезатора Тето/Мику. Файлы остаются в памяти телефона. Нужны ONNX-модели, экспортированные для voice-changer с метаданными; обычные .pth не подходят. Размеры могут составить сотни мегабайт.");
        root.addView(intro);
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            labels[i] = new TextView(this);
            labels[i].setPadding(0, pad, 0, 4);
            root.addView(labels[i]);
            buttons[i] = new Button(this);
            buttons[i].setText("Выбрать файл ONNX");
            buttons[i].setOnClickListener(v -> pick(slot));
            root.addView(buttons[i]);
        }
        TextView privacy = new TextView(this);
        privacy.setPadding(0, pad, 0, 0);
        privacy.setText("Примечание: модели должны иметь разрешение на использование. Тестируйте преобразование на коротких фразах. Первое включение может использовать много памяти и времени.");
        root.addView(privacy);
        scroller.addView(root);
        setContentView(scroller);
        refresh();
    }
    private void refresh() {
        for (int i = 0; i < 4; i++) {
            File f = RvcModelStore.path(this, i);
            labels[i].setText(RvcModelStore.LABELS[i] + "\n" + (f.isFile() ? ("Установлена: " + (f.length() / (1024 * 1024)) + " МиБ") : "Не установлена"));
            buttons[i].setEnabled(!copying);
        }
    }
    private void pick(int slot) {
        if (copying) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, 1000 + slot);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request < 1000 || request >= 1004 || result != RESULT_OK || data == null || data.getData() == null || copying) return;
        int slot = request - 1000;
        Uri uri = data.getData();
        copying = true; refresh();
        new Thread(() -> {
            String error = null;
            try { RvcModelStore.importModel(getApplicationContext(), uri, slot); }
            catch (Exception e) {error = e.getMessage();}
            final String message = error;
            runOnUiThread(() -> {
                copying = false;
                refresh();
                if (!isFinishing() && message != null) {
                    new AlertDialog.Builder(this).setTitle("Ошибка импорта").setMessage(message).setPositiveButton("ОК", null).show();
                }
            });
        }, "TetogramRvcImport").start();
    }
    @Override public void onBackPressed() {if (!copying) super.onBackPressed();}
}
