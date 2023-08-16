package com.fingertip.baselib.view;

import android.content.Context;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fingertip.baseLib.R;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer;


/**
 * 带封面 播放器UI
 */

public class SampleCoverVideo extends StandardGSYVideoPlayer {

    ImageView mCoverImage;

    String mCoverOriginUrl;

    int  mCoverOriginId = 0;

    int mDefaultRes;

    ProgressBar video_loading;//加载动画
    ImageView btn_start;//开始/重播
    LinearLayout ll_error;//加载失败
    TextView tv_reload;//重新加载

    public SampleCoverVideo(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    public SampleCoverVideo(Context context) {
        super(context);
    }

    public SampleCoverVideo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        mCoverImage =  findViewById(R.id.thumbImage);
        video_loading = findViewById(R.id.video_loading);
        btn_start = findViewById(R.id.btn_start);
        ll_error = findViewById(R.id.ll_error);
        tv_reload = findViewById(R.id.tv_reload);

        mCache = true;

        if (mThumbImageViewLayout != null &&
                (mCurrentState == -1 || mCurrentState == CURRENT_STATE_NORMAL || mCurrentState == CURRENT_STATE_ERROR)) {
            mThumbImageViewLayout.setVisibility(VISIBLE);
        }

        btn_start.setOnClickListener(v -> {
            startPlayLogic();
        });
        tv_reload.setOnClickListener(v -> {
            startPlayLogic();
        });
    }

    @Override
    public int getLayoutId() {
        return R.layout.video_layout_cover;
    }

    public void loadCoverImage(String url, int res) {
        mCoverOriginUrl = url;
        mDefaultRes = res;
        Glide.with(getContext().getApplicationContext()).load(url).placeholder(R.mipmap.icon_default_img).into(mCoverImage);
    }

    public void loadCoverImageBy(int id, int res) {
        mCoverOriginId = id;
        mDefaultRes = res;
        mCoverImage.setImageResource(id);
    }

    @Override
    public GSYBaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
        GSYBaseVideoPlayer gsyBaseVideoPlayer = super.startWindowFullscreen(context, actionBar, statusBar);
        SampleCoverVideo sampleCoverVideo = (SampleCoverVideo) gsyBaseVideoPlayer;
        if(mCoverOriginUrl != null) {
            sampleCoverVideo.loadCoverImage(mCoverOriginUrl, mDefaultRes);
        } else  if(mCoverOriginId != 0) {
            sampleCoverVideo.loadCoverImageBy(mCoverOriginId, mDefaultRes);
        }
        return gsyBaseVideoPlayer;
    }


    @Override
    public GSYBaseVideoPlayer showSmallVideo(Point size, boolean actionBar, boolean statusBar) {
        //下面这里替换成你自己的强制转化
        SampleCoverVideo sampleCoverVideo = (SampleCoverVideo) super.showSmallVideo(size, actionBar, statusBar);
        sampleCoverVideo.mStartButton.setVisibility(GONE);
        sampleCoverVideo.mStartButton = null;
        return sampleCoverVideo;
    }

    @Override
    protected void cloneParams(GSYBaseVideoPlayer from, GSYBaseVideoPlayer to) {
        super.cloneParams(from, to);
        SampleCoverVideo sf = (SampleCoverVideo) from;
        SampleCoverVideo st = (SampleCoverVideo) to;
        st.mShowFullAnimation = sf.mShowFullAnimation;
    }


    /******************* 下方两个重载方法，在播放开始前不屏蔽封面，不需要可屏蔽 ********************/
    @Override
    public void onSurfaceUpdated(Surface surface) {
        super.onSurfaceUpdated(surface);
        if (mThumbImageViewLayout != null && mThumbImageViewLayout.getVisibility() == VISIBLE) {
            mThumbImageViewLayout.setVisibility(INVISIBLE);
        }
    }

    @Override
    protected void setViewShowState(View view, int visibility) {
        if (view == mThumbImageViewLayout && visibility != VISIBLE) {
            return;
        }
        super.setViewShowState(view, visibility);
    }

    @Override
    public void onSurfaceAvailable(Surface surface) {
        super.onSurfaceAvailable(surface);
        if (GSYVideoType.getRenderType() != GSYVideoType.TEXTURE) {
            if (mThumbImageViewLayout != null && mThumbImageViewLayout.getVisibility() == VISIBLE) {
                mThumbImageViewLayout.setVisibility(INVISIBLE);
            }
        }
    }

    /******************* 下方重载方法，在播放开始不显示底部进度和按键，不需要可屏蔽 ********************/

    @Override
    protected void onClickUiToggle(MotionEvent e) {

    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.thumb){
            if (TextUtils.isEmpty(mUrl)) return;

            if (mCurrentState == CURRENT_STATE_NORMAL || mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
                startPlayLogic();
            }
            return;
        }
        super.onClick(v);
    }




    //滑动的距离，超过20就表示滑动，否则视为点击
    float absDeltaX;
    float absDeltaY;
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int id = v.getId();
        float x = event.getX();
        float y = event.getY();
        if (mIfCurrentIsFullscreen && mLockCurScreen && mNeedLockFull) {
            onClickUiToggle(event);
            startDismissControlViewTimer();
            return true;
        }

        if (id == R.id.fullscreen) {
            return false;
        }
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchSurfaceDown(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    absDeltaX = Math.abs(deltaX);
                    absDeltaY = Math.abs(deltaY);

                    if ((mIfCurrentIsFullscreen && mIsTouchWigetFull)
                            || (mIsTouchWiget && !mIfCurrentIsFullscreen)) {
                        if (!mChangePosition && !mChangeVolume && !mBrightness) {
                            touchSurfaceMoveFullLogic(absDeltaX, absDeltaY);
                        }
                    }
                    touchSurfaceMove(deltaX, deltaY, y);
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    touchSurfaceUp();
                    startProgressTimer();
                    //不要和隐藏虚拟按键后，滑出虚拟按键冲突
                    if (mHideKey && mShowVKey) {
                        return true;
                    }
                    break;
            }
            if (absDeltaX>20 || absDeltaY>20){
                return true;
            }
            gestureDetector.onTouchEvent(event);
        } else if (id == R.id.progress) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    cancelDismissControlViewTimer();
                case MotionEvent.ACTION_MOVE:
                    cancelProgressTimer();
                    ViewParent vpdown = getParent();
                    while (vpdown != null) {
                        vpdown.requestDisallowInterceptTouchEvent(true);
                        vpdown = vpdown.getParent();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    startProgressTimer();
                    ViewParent vpup = getParent();
                    while (vpup != null) {
                        vpup.requestDisallowInterceptTouchEvent(false);
                        vpup = vpup.getParent();
                    }
                    mBrightnessData = -1f;
                    break;
            }
        }

        return false;
    }


    //双击暂停/播放
    @Override
    protected void touchDoubleUp(MotionEvent e) {
//        super.touchDoubleUp(e);
    }


    @Override
    protected void hideAllWidget() {
        super.hideAllWidget();
        setViewShowState(btn_start, INVISIBLE);
        setViewShowState(ll_error, INVISIBLE);
    }

    @Override
    protected void changeUiToPreparingShow() {
        super.changeUiToPreparingShow();
        setViewShowState(ll_error, INVISIBLE);
        setViewShowState(video_loading, VISIBLE);
        setViewShowState(btn_start, INVISIBLE);
    }


    @Override
    protected void changeUiToNormal() {
        setViewShowState(mTopContainer, VISIBLE);
        setViewShowState(btn_start, isInPlayingState()?INVISIBLE:VISIBLE);
        setViewShowState(video_loading, INVISIBLE);
        setViewShowState(ll_error, INVISIBLE);
        setViewShowState(mThumbImageViewLayout, VISIBLE);
        setViewShowState(mBottomProgressBar, INVISIBLE);
        if (!isInPlayingState())updateStartImage();

    }

    @Override
    protected void changeUiToPlayingShow() {
        setViewShowState(mTopContainer, VISIBLE);
        setViewShowState(btn_start, INVISIBLE);
        setViewShowState(video_loading, INVISIBLE);
        setViewShowState(ll_error, INVISIBLE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mBottomProgressBar, VISIBLE);
        if (!isInPlayingState())updateStartImage();
    }

    @Override
    protected void changeUiToCompleteShow() {
        super.changeUiToCompleteShow();
        setViewShowState(btn_start, VISIBLE);
        setViewShowState(video_loading, INVISIBLE);
        setViewShowState(ll_error, INVISIBLE);
        setViewShowState(mStartButton, INVISIBLE);
        updateStartImage();
    }

    @Override
    protected void changeUiToPauseShow() {
        super.changeUiToPauseShow();
        setViewShowState(mStartButton, INVISIBLE);
        setViewShowState(btn_start, VISIBLE);
        updateStartImage();
    }

    @Override
    protected void updatePauseCover() {
        //暂停时不替换成封面
    }

    @Override
    protected void changeUiToPauseClear() {
        super.changeUiToPauseClear();
        setViewShowState(btn_start, INVISIBLE);
    }

    @Override
    protected void changeUiToError() {
        super.changeUiToError();
        setViewShowState(ll_error, VISIBLE);
        setViewShowState(btn_start, INVISIBLE);
        setViewShowState(video_loading, INVISIBLE);
    }

    @Override
    public void startAfterPrepared() {
        super.startAfterPrepared();
        setViewShowState(video_loading, INVISIBLE);
        setViewShowState(ll_error, INVISIBLE);
        setViewShowState(mBottomProgressBar, VISIBLE);
    }


    @Override
    protected void updateStartImage() {
        if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE && isIfCurrentIsFullscreen()){ //全屏状态下播放结束则显示重播
            btn_start.setImageResource(R.mipmap.icon_video_replay);
        }else {
            btn_start.setImageResource(R.mipmap.icon_play);
        }
    }
}
