package iknow.android.utils.thread;

import android.os.Handler;
import android.os.Looper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UiThreadExecutor {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final Map<String, Runnable> TASKS = new ConcurrentHashMap<>();

    public static void runTask(String id, Runnable runnable, long delay) {
        TASKS.put(id, runnable);
        if (delay > 0) {
            HANDLER.postDelayed(runnable, delay);
        } else {
            HANDLER.post(runnable);
        }
    }

    public static void cancelAll(String id) {
        Runnable runnable = TASKS.remove(id);
        if (runnable != null) {
            HANDLER.removeCallbacks(runnable);
        }
    }
}
