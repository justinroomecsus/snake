package com.gamecodeschool.snake;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class SnakeGame extends SurfaceView implements Runnable, GameObject {
    private static final int NUM_BLOCKS_WIDE = 40;
    private static final int TARGET_FPS = 10;
    private static final long MILLIS_PER_SECOND = 1000;

    private Thread mThread = null;
    private long mNextFrameTime;
    private volatile boolean mPlaying = false;
    private volatile boolean mPaused = true;
    private SoundPool mSP;
    private int mEat_ID = -1;
    private int mCrashID = -1;
    private int mNumBlocksHigh;
    private int mScore;
    private Canvas mCanvas;
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private Snake mSnake;
    private Apple mApple;

    public SnakeGame(Context context, Point size) {
        super(context);
        initializeGame(context, size);
    }

    private void initializeGame(Context context, Point size) {
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        mNumBlocksHigh = size.y / blockSize;
        initializeSoundPool(context);
        mSurfaceHolder = getHolder();
        mPaint = new Paint();
        mApple = new Apple(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize);
        mSnake = new Snake(context, new Point(NUM_BLOCKS_WIDE, mNumBlocksHigh), blockSize);
    }

    private void initializeSoundPool(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            mSP = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            mSP = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }
        loadSoundFiles(context);
    }

    private void loadSoundFiles(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            descriptor = assetManager.openFd("get_apple.ogg");
            mEat_ID = mSP.load(descriptor, 0);
            descriptor = assetManager.openFd("snake_death.ogg");
            mCrashID = mSP.load(descriptor, 0);

        } catch (IOException e) {
            // Log the exception
        }
    }

    public void newGame() {
        mSnake.reset(NUM_BLOCKS_WIDE, mNumBlocksHigh);
        mApple.spawn();
        mScore = 0;
        mNextFrameTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (mPlaying) {
            if (!mPaused) {
                if (updateRequired()) {
                    update();
                }
            }
            draw();
        }
    }

    public boolean updateRequired() {
        if (mNextFrameTime <= System.currentTimeMillis()) {
            mNextFrameTime = System.currentTimeMillis() + MILLIS_PER_SECOND / TARGET_FPS;
            return true;
        }
        return false;
    }

    public void update() {
        mSnake.move();
        if (mSnake.checkDinner(mApple.getLocation())) {
            mApple.spawn();
            mScore = mScore + 1;
            mSP.play(mEat_ID, 1, 1, 0, 0, 1);
        }

        if (mSnake.detectDeath()) {
            mSP.play(mCrashID, 1, 1, 0, 0, 1);
            mPaused = true;
        }
    }

    public void draw() {
        if (mSurfaceHolder.getSurface().isValid()) {
            mCanvas = mSurfaceHolder.lockCanvas();
            mCanvas.drawColor(Color.argb(255, 26, 128, 182));
            mPaint.setColor(Color.argb(255, 255, 255, 255));
            mPaint.setTextSize(120);
            mCanvas.drawText("" + mScore, 20, 120, mPaint);
            mApple.draw(mCanvas, mPaint);
            mSnake.draw(mCanvas, mPaint);

            if (mPaused) {
                mPaint.setColor(Color.argb(255, 255, 255, 255));
                mPaint.setTextSize(250);
                mCanvas.drawText(getResources().getString(R.string.tap_to_play), 200, 700, mPaint);
            }
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                if (mPaused) {
                    mPaused = false;
                    newGame();
                    return true;
                }

                mSnake.switchHeading(motionEvent);
                break;

            default:
                break;
        }
        return true;
    }

    public void pause() {
        mPlaying = false;
        try {
            mThread.join();
        } catch (InterruptedException e) {
            // Log the exception
        }
    }

    public void resume() {
        mPlaying = true;
        mThread = new Thread(this);
        mThread.start();
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        mApple.draw(canvas, paint);
        mSnake.draw(canvas, paint);
    }

    @Override
    public void update(Point size) {
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        mNumBlocksHigh = size.y / blockSize;
        mApple.update(size);
        mSnake.update(size);
    }
}