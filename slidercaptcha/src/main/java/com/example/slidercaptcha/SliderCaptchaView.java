package com.example.slidercaptcha;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatImageView;

import java.math.BigDecimal;
import java.util.Random;

public class SliderCaptchaView extends AppCompatImageView {

    private Paint mShadowPaint;
    private int mSliderHeight;
    private int mSliderWidth;
    private int mWidth;
    private int mHeight;
    private Path mSliderPath;
    private Paint mSliderPaint;
    private Paint mPaint;
    private int mSliderX;
    private Bitmap sliderShadowBitmap;
    private Bitmap sliderBitmap;
    private int mSliderLeftPadding = 40;
    private int mDragOffset;
    private int mPosition;
    private long mStartDragTime;
    private int mCount;
    private int mSliderY;
    private int mDragLimit = 3;
    private float c = 0.5522847498f;
    private PorterDuffXfermode mPorterDuffXfermode;
    private boolean isSuccess;
    private Bitmap mSuccessBitmap;
    private Paint mSuccessPaint;
    private Paint mTextPaint;
    private long mInterval;
    private float mSliderSize = 50;

    public SliderCaptchaView(Context context) {
        this(context, null);
    }

    public SliderCaptchaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SliderCaptchaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.sliderCaptcha, defStyleAttr, 0);
        mSliderSize = typedArray.getDimension(R.styleable.sliderCaptcha_sliderSize, 50);
        mDragLimit = typedArray.getInteger(R.styleable.sliderCaptcha_limit, 3);
        typedArray.recycle();
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        int defaultSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_PX, mSliderSize, getResources().getDisplayMetrics());
        mSliderHeight = defaultSize;
        mSliderWidth = defaultSize;

        //防锯齿和防抖动
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(0x77000000);

        //滑块画笔
        mPorterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        mSliderPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mSliderPaint.setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.SOLID));

        //阴影画笔
        mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mShadowPaint.setColor(Color.BLACK);
        //初始化滑块path
        mSliderPath = new Path();

        mSuccessBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.success);

        mSuccessPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mSuccessPaint.setColor(0x77ffffff);

        mTextPaint = new Paint();
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setStrokeWidth(8);
        mTextPaint.setTextSize(32);
        mTextPaint.setColor(Color.GREEN);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //获取控件的宽高
        mWidth = w;
        mHeight = h;
        //seekBar初始位置
        mPosition = (mSliderLeftPadding + mSliderWidth / 2) / mWidth * 100;
        post(new Runnable() {
            @Override
            public void run() {
                createNewCaptcha();
            }
        });
    }

    /**
     * 重载滑块
     */
    public void createNewCaptcha() {
        createSliderPath();
        createSliderBitmap();
        invalidate();
    }

    private void createSliderBitmap() {
        sliderBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(sliderBitmap);
        c.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        //绘制遮罩
        c.drawPath(mSliderPath, mSliderPaint);
        mSliderPaint.setXfermode(mPorterDuffXfermode);
        c.drawBitmap(((BitmapDrawable) getDrawable()).getBitmap(), getImageMatrix(), mSliderPaint);
        mSliderPaint.setXfermode(null);
        //滑块阴影
        sliderShadowBitmap = sliderBitmap.extractAlpha();
    }

    private void createSliderPath() {
        //​​ 一个良好的三阶贝塞尔近似圆弧：
        //c ≈ 0.5522847498
        //半径为1，P_0 = (0,1)  P_1 = (c, 1)  P_2 = (1,c)  P_3 = (1,0)

        Random random = new Random();
        int gap = mSliderWidth / 3;
        mSliderX = random.nextInt(mWidth / 2 - mSliderWidth - gap) + mWidth / 2;
        int max = mHeight - mSliderHeight - gap - mSliderLeftPadding;
        int min = gap + mSliderLeftPadding;
        mSliderY = random.nextInt(max) % (max - min + 1) + min;

        //重置path
        mSliderPath.reset();


        mSliderPath.lineTo(0, 0);

        //开始绘制阴影区域
        //半径画一个随机凹凸的半圆,运用三阶贝塞尔曲线
        int r = gap / 2;

        topPath(random, gap, r);

        rightPath(r, gap, random);

        bottomPath(random, gap, r);

        leftPath(r, gap, random);


        mSliderPath.close();
    }

    private void topPath(Random random, int gap, int r) {
        mSliderPath.moveTo(mSliderX, mSliderY);
        mSliderPath.lineTo(mSliderX + gap, mSliderY);
        boolean isOut = random.nextBoolean();
        PointF p1 = new PointF(mSliderX + gap, isOut ? mSliderY - c * r : mSliderY + c * r);
        PointF p2 = new PointF(mSliderX + gap + (r - c * r), isOut ? mSliderY - r : mSliderY + r);
        PointF p3 = new PointF(mSliderX + gap + r, isOut ? mSliderY - r : mSliderY + r);
        mSliderPath.cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
        PointF _p1 = new PointF(mSliderX + gap + r + c * r, isOut ? mSliderY - r : mSliderY + r);
        PointF _p2 = new PointF(mSliderX + 2 * gap, isOut ? mSliderY - c * r : mSliderY + c * r);
        PointF _p3 = new PointF(mSliderX + 2 * gap, mSliderY);
        mSliderPath.cubicTo(_p1.x, _p1.y, _p2.x, _p2.y, _p3.x, _p3.y);
    }

    private void rightPath(int r, int gap, Random random) {
        mSliderPath.lineTo(mSliderX + mSliderWidth, mSliderY);//右上角
        mSliderPath.lineTo(mSliderX + mSliderWidth, mSliderY + gap);
        boolean isOut = random.nextBoolean();
        PointF p1 = new PointF(isOut ? mSliderX + mSliderWidth + c * r : mSliderX + mSliderWidth - c * r, mSliderY + gap);
        PointF p2 = new PointF(isOut ? mSliderX + mSliderWidth + r : mSliderX + mSliderWidth - r, mSliderY + gap + r - c * r);
        PointF p3 = new PointF(isOut ? mSliderX + mSliderWidth + r : mSliderX + mSliderWidth - r, mSliderY + gap + r);
        mSliderPath.cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
        PointF _p1 = new PointF(isOut ? mSliderX + mSliderWidth + r : mSliderX + mSliderWidth - r, mSliderY + gap + r + c * r);
        PointF _p2 = new PointF(isOut ? mSliderX + mSliderWidth + c * r : mSliderX + mSliderWidth - c * r, mSliderY + 2 * gap);
        PointF _p3 = new PointF(mSliderX + mSliderWidth, mSliderY + 2 * gap);
        mSliderPath.cubicTo(_p1.x, _p1.y, _p2.x, _p2.y, _p3.x, _p3.y);
    }

    private void bottomPath(Random random, int gap, int r) {
        mSliderPath.lineTo(mSliderX + mSliderWidth, mSliderY + mSliderHeight);
        mSliderPath.lineTo(mSliderX + mSliderWidth - gap, mSliderY + mSliderHeight);
        boolean isOut = random.nextBoolean();
        PointF p1 = new PointF(mSliderX + mSliderWidth - gap, isOut ? mSliderY + mSliderHeight + c * r : mSliderY + mSliderHeight - c * r);
        PointF p2 = new PointF(mSliderX + mSliderWidth / 2.0F + c * r, isOut ? mSliderY + mSliderHeight + r : mSliderY + mSliderHeight - r);
        PointF p3 = new PointF(mSliderX + mSliderWidth / 2.0F, isOut ? mSliderY + mSliderHeight + r : mSliderY + mSliderHeight - r);
        mSliderPath.cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
        PointF _p1 = new PointF(mSliderX + mSliderWidth / 2.0F - c * r, isOut ? mSliderY + mSliderHeight + r : mSliderY + mSliderHeight - r);
        PointF _p2 = new PointF(mSliderX + gap, isOut ? mSliderY + mSliderHeight + c * r : mSliderY + mSliderHeight - c * r);
        PointF _p3 = new PointF(mSliderX + gap, mSliderY + mSliderHeight);
        mSliderPath.cubicTo(_p1.x, _p1.y, _p2.x, _p2.y, _p3.x, _p3.y);
    }

    private void leftPath(int r, int gap, Random random) {
        mSliderPath.lineTo(mSliderX, mSliderY + mSliderHeight);
        mSliderPath.lineTo(mSliderX, mSliderY + mSliderHeight - gap);
        boolean isOut = random.nextBoolean();
        PointF p1 = new PointF(isOut ? mSliderX - c * r : mSliderX + c * r, mSliderY + 2 * gap);
        PointF p2 = new PointF(isOut ? mSliderX - r : mSliderX + r, mSliderY + gap + r + c * r);
        PointF p3 = new PointF(isOut ? mSliderX - r : mSliderX + r, mSliderY + gap + r);
        mSliderPath.cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
        PointF _p1 = new PointF(isOut ? mSliderX - r : mSliderX + r, mSliderY + gap + r - c * r);
        PointF _p2 = new PointF(isOut ? mSliderX - c * r : mSliderX + c * r, mSliderY + gap);
        PointF _p3 = new PointF(mSliderX, mSliderY + gap);
        mSliderPath.cubicTo(_p1.x, _p1.y, _p2.x, _p2.y, _p3.x, _p3.y);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制阴影
        canvas.drawPath(mSliderPath, mPaint);
        //绘制滑块
        drawSlider(canvas);
        if (isSuccess) {
            canvas.drawRect(0, 0, mWidth, mHeight, mSuccessPaint);
            canvas.drawBitmap(mSuccessBitmap, mWidth / 2.0f - mSuccessBitmap.getWidth() / 2.0f, mHeight / 2.0f - mSuccessBitmap.getHeight() / 2.0f, null);
            Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
            float distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
            canvas.drawText("共用时：" + new BigDecimal(mInterval / 1000.0).setScale(1,
                    BigDecimal.ROUND_UP).toString() + "s", mWidth / 2.0f, mHeight / 2 +
                    mSuccessBitmap.getWidth() / 2.0f + 40 + distance, mTextPaint);
        }
    }

    private void drawSlider(Canvas canvas) {
        canvas.drawBitmap(sliderShadowBitmap, -mSliderX + mDragOffset + mSliderLeftPadding, 0, mShadowPaint);
        canvas.drawBitmap(sliderBitmap, -mSliderX + mDragOffset + mSliderLeftPadding, 0, null);
    }

    public void setDragOffset(int offset) {
        int realOffset = offset - 10;
        this.mDragOffset = (int) (realOffset / 100f * (getWidth() - mSliderLeftPadding));
//        Log.i("mango", "offset:" + offset + " --- DragOffset:" + mDragOffset + " --- SliderX:" + mSliderX);
        invalidate();
    }

    public void startDragSlider() {
        mStartDragTime = System.currentTimeMillis();
        if (mCaptchaDragListener != null)
            mCaptchaDragListener.onStart();
    }

    public void stopDragSlider() {
        mCount++;
        int currentPosition = mDragOffset + mSliderLeftPadding;
        if (currentPosition >= mSliderX - 6 && currentPosition <= mSliderX + 6) {
            //验证成功
            isSuccess = !isSuccess;
            mInterval = System.currentTimeMillis() - mStartDragTime;
            if (mCaptchaDragListener != null)
                mCaptchaDragListener.onVerifySuccess(mInterval);
            invalidate();
        } else {
            //验证失败
            if (mCount < mDragLimit) {
                if (mCaptchaDragListener != null) {
                    // TODO: 2020-02-20 次数不达上限滑块返回初始位置
                    mCaptchaDragListener.onVerifyFailure();
                    mDragOffset = 0;
                    invalidate();
                }
            } else {
                // TODO: 2020-02-20 次数已达重新载入图片，重新生成滑块
                mCount = 0;
                mCaptchaDragListener.onReload(this);
                mDragOffset = 0;
            }

        }


    }

    private CaptchaDragListener mCaptchaDragListener;

    public void setCaptchaDragListener(CaptchaDragListener captchaDragListener) {
        this.mCaptchaDragListener = captchaDragListener;
    }

    public interface CaptchaDragListener {
        void onStart();

        void onVerifySuccess(long interval);

        void onVerifyFailure();

        void onReload(SliderCaptchaView slider);
    }


}
