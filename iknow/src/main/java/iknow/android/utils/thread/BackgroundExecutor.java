package iknow.android.utils.thread;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BackgroundExecutor {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Map<String, Task> TASKS = new ConcurrentHashMap<>();

    public static abstract class Task implements Runnable {
        private String id;
        private long delay;
        private String group;

        public Task(String id, long delay, String group) {
            this.id = id;
            this.delay = delay;
            this.group = group;
        }

        public abstract void execute();

        @Override
        public void run() {
            TASKS.put(id, this);
            try {
                execute();
            } finally {
                TASKS.remove(id);
            }
        }

        public String getId() { return id; }
        public String getGroup() { return group; }
    }

    public static void execute(Task task) {
        EXECUTOR.execute(task);
    }

    public static void cancelAll(String id, boolean mayInterruptIfRunning) {
        for (Task task : TASKS.values()) {
            if (task.getId().equals(id)) {
                TASKS.remove(task.getId());
            }
        }
    }
}
