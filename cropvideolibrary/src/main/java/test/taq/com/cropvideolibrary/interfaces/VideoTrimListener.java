package test.taq.com.cropvideolibrary.interfaces;

/**
 * Author：J.Chou
 * Date：  2016.08.01 2:23 PM
 * Email： who_know_me@163.com
 * Describe:
 */
public interface VideoTrimListener {
    void onStartTrim(float start,float duration);
    void onFinishTrim(String url);
    void onCancel();

    void onProgress(String Progress);
}
