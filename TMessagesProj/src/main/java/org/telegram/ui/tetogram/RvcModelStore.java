/* Tetogram. GPL-2.0-or-later. Local RVC ONNX model pack storage. */
package org.telegram.ui.tetogram;

import android.content.Context;
import android.net.Uri;
import android.os.StatFs;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

/** Four user-imported ONNX files, never uploaded or fetched over the network. */
public final class RvcModelStore {
    private RvcModelStore() {}
    public static final int HUBERT = 0, RMVPE = 1, TETO = 2, MIKU = 3;
    public static final String[] LABELS = {"HuBERT / ContentVec", "RMVPE (F0)", "Касане Тето · Synth ONNX", "Хацунэ Мику · Synth ONNX"};
    private static final String[] FILES = {"hubert.onnx", "rmvpe.onnx", "teto.onnx", "miku.onnx"};
    private static final long MAX_BYTES = 900L * 1024 * 1024;

    public static File path(Context context, int slot) {
        if (slot < 0 || slot >= FILES.length) throw new IllegalArgumentException("slot");
        return new File(new File(context.getFilesDir(), "rvc-models"), FILES[slot]);
    }
    public static boolean available(Context ctx, int which) {
        return valid(path(ctx, HUBERT)) && valid(path(ctx, RMVPE)) && valid(path(ctx, which == 0 ? TETO : MIKU));
    }
    private static boolean valid(File f) {return f.isFile() && f.length() > 1024;}
    public static String description(Context ctx, int which) {
        return available(ctx, which) ? "RVC: необходимые модели установлены" : "RVC: выберите HuBERT, RMVPE и модель персонажа в настройках";
    }

    /** Copies untrusted SAF input into private storage, with a hard limit and atomic replacement. */
    public static void importModel(Context ctx, Uri source, int slot) throws IOException {
        File dest = path(ctx, slot);
        File dir = dest.getParentFile();
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Не удалось создать папку моделей");
        File temp = new File(dir, FILES[slot] + ".part");
        if (temp.exists() && !temp.delete()) throw new IOException("Временный файл занят");
        StatFs fs = new StatFs(dir.getAbsolutePath());
        if (fs.getAvailableBytes() < 20L * 1024 * 1024) throw new IOException("Недостаточно свободной памяти");
        long count = 0;
        try {
            try (InputStream in = ctx.getContentResolver().openInputStream(source);
                 FileOutputStream out = new FileOutputStream(temp)) {
                if (in == null) throw new IOException("Не удалось открыть выбранный файл");
                byte[] buffer = new byte[64 * 1024];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    count += n;
                    if (count > MAX_BYTES) throw new IOException("Модель слишком большая (лимит 900 МиБ)");
                    out.write(buffer, 0, n);
                }
                out.getFD().sync();
            }
            if (count <= 1024) throw new IOException("Модель пустая или повреждена");
            if (!temp.renameTo(dest)) throw new IOException("Не удалось сохранить модель");
        } finally {
            if (temp.exists()) temp.delete();
        }
    }
}
