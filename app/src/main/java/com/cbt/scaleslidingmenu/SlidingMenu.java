package com.cbt.scaleslidingmenu;

import android.content.Context;
import android.graphics.PointF;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;

/**
 * Created by Admin on 2016/12/5.
 * 主交互面板
 */

public class SlidingMenu extends ViewGroup {

    //获取用户操作意图的距离
    private static final double GET_OPERATE_DISTANCE = 30;
    private static final int DURARION = 300;

    public SlidingMenu(Context context) {
        this(context, null);
    }

    public SlidingMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    //导航栏宽度
    private int naviWidth;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        naviWidth = (int) (getMeasuredWidth() >> 1);
        //测量导航栏,导航栏占本容器宽度的一半
        naviContainer.measure(MeasureSpec.makeMeasureSpec(naviWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
        contentContainer.measure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int naviLeft = -naviWidth >> 1;
        naviContainer.layout(naviLeft, t, naviLeft + naviWidth, b);
        contentContainer.layout(l, t, r, b);
    }

    private PointF pointF = new PointF();
    //是否是打开导航栏的操作
    private boolean isOpenNavigation;
    //导航栏当前的状态是否是打开状态
    private boolean isNaviOpend;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pointF.x = ev.getX();
                pointF.y = ev.getY();
                if (pointF.x <= getWidth() * 0.10f) {
                    isOpenNavigation = true;
                }
                //导航栏不是打开状态时，正常分发事件，反之不分发
                if (!isNaviOpend) {
                    super.dispatchTouchEvent(ev);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isNaviOpend) {//导航栏展开的状态下，任何事件都不下发，只能操作导航栏
                    //计算移动的距离
                    float x = ev.getX();
                    int dx = (int) (x - pointF.x);
                    pointF.x = x;
                    dealLeftRightSliding(dx);
                } else {
                    //导航栏不展开的状态下，进行处理事件
                    dealNaviCloseState(ev);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!isNaviOpend) {
                    super.dispatchTouchEvent(ev);//DOWN事件正常下发事件
                }

                isGetOperatType = false;
                isOpenNavigation = false;

                int curLeft = contentContainer.getLeft();
                if (curLeft >= naviWidth >> 1) {//展开导航栏
                    openNavigation();
                } else {//收起导航栏
                    closeNavigation();
                }
                break;
        }
        //导航栏不是打开时，正常分发事件，反之不分发
        if (!isNaviOpend) {
            super.dispatchTouchEvent(ev);
        }
        return true;
    }

    private void dealNaviCloseState(MotionEvent ev) {
        if (isOpenNavigation) {
            if (!isGetOperatType) {
                getEventType(ev);
            } else {
                if (isLeftRight) {
                    //计算移动的距离
                    float x = ev.getX();
                    int dx = (int) (x - pointF.x);
                    pointF.x = x;
                    //处理从左向右拉的事件
                    dealLeftRightSliding(dx);
                } else {
                    super.dispatchTouchEvent(ev);//正常分发事件
                }
            }
        } else {
            super.dispatchTouchEvent(ev);//正常分发事件
        }
    }


    private Scroller mScroller;
    private void init(Context context) {
        mScroller = new Scroller(context, new AccelerateDecelerateInterpolator());
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            //获取当前应该到达的坐标
            int curLeft = mScroller.getCurrX();
            //计算本次应该到达的坐标和当前坐标的差值
            int dx = curLeft - contentContainer.getLeft();
            dealLeftRightSliding(dx);
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void closeNavigation() {
        //主面板从当前位置移动到0
        int curLeft = contentContainer.getLeft();
        mScroller.startScroll(curLeft, 0, -curLeft, 0, DURARION);
        ViewCompat.postInvalidateOnAnimation(this);
        isNaviOpend = false;
    }

    public void openNavigation() {
        //主面板从当前位置移动到导航栏的宽度
        int curLeft = contentContainer.getLeft();
        mScroller.startScroll(curLeft, 0, naviWidth - curLeft, 0, DURARION);
        ViewCompat.postInvalidateOnAnimation(this);
        isNaviOpend = true;
    }

    //导航栏的默认缩放倍数
    private float naviDefaultScale = 0.75f;

    private void dealLeftRightSliding(int dx) {
        //主面板可以移动的最大距离
        int contentMaxTranX = naviWidth;
        //本次理论上主面板应该移动的距离
        int curContentLeft = contentContainer.getLeft() + dx;
        if (curContentLeft < 0) {
            curContentLeft = 0;
        } else if (curContentLeft > contentMaxTranX) {
            curContentLeft = contentMaxTranX;
        }
        //主面板移动
        contentContainer.layout(curContentLeft, contentContainer.getTop(), curContentLeft + contentContainer.getMeasuredWidth(), contentContainer.getBottom());
        //计算主面板当前平移的百分比
        float percent = curContentLeft / (float) contentMaxTranX;
        //主面板缩放
        float contentScale = 1 - percent * (1 - naviDefaultScale);
        contentContainer.setScaleX(contentScale);
        contentContainer.setScaleY(contentScale);
        //主面板渐变效果
        contentContainer.setAlpha((float) (1 - (percent * 0.5)));
        //计算缩小造成的X的平移
        float offsetX = contentContainer.getMeasuredWidth() * (1 - contentScale) / 2;
        contentContainer.setTranslationX(-offsetX);
        //导航栏平移
        int naviMaxOffset = -naviWidth >> 1;
        naviContainer.setTranslationX(-naviMaxOffset * percent);
        //导航栏缩放
        //1.计算导航栏的缩放倍数,导航栏默认缩放倍数为0.75，
        float scale = naviDefaultScale + ((1 - naviDefaultScale) * percent);
        naviContainer.setScaleX(scale);
        naviContainer.setScaleY(scale);
        //导航栏渐变效果
        naviContainer.setAlpha((float) (0.2 + percent));
    }

    //是否是左右操作
    private boolean isLeftRight;
    //标识是否获取到了用户的操作意图
    private boolean isGetOperatType;

    //获取用户的操作意图
    private void getEventType(MotionEvent ev) {
//        System.out.println("未判断出用户操作意图");
        float x = ev.getX();
        float y = ev.getY();
        //根据手指移动的坐标计算两个点之间的距离
        double distance = Math.sqrt(Math.pow(pointF.x - x, 2) + Math.pow(pointF.y - y, 2));

        //距离大于30，看操作类型是上下操作还是左右操作，如果是上下操作，那么事件就正常下发。
        // 如果是左右操作则不下发事件，而是本容器来展开或者关闭导航栏
        if (distance > GET_OPERATE_DISTANCE) {
            //根据X偏移量和Y偏移量来区分是什么样的操作
            if (Math.abs(pointF.x - x) >= Math.abs(pointF.y - y)) {//左右滑动

                //更新计算的起始点
                pointF.x = x;
                pointF.y = y;

                isLeftRight = true;
                //虚拟一个CANCEL事件发下去
                MotionEvent event = MotionEvent.obtain(ev);
                event.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(event);
            } else {//上下操作
                isLeftRight = false;
            }
            isGetOperatType = true;
        }

    }


    //导航栏
    private ViewGroup naviContainer;
    //内容显示区域
    private ViewGroup contentContainer;

    @Override
    protected void onFinishInflate() {
        //获取导航栏和内容显示容器
        int childCount = getChildCount();
        if (childCount != 2) {
            throw new IllegalArgumentException("本容器必须只能有2个容器作为直接Child");
        }

        View child = getChildAt(0);
        if (!(child instanceof ViewGroup)) {
            throw new IllegalArgumentException("本容器的直接Child，必须是ViewGroup!");
        }
        //实例化导航栏
        naviContainer = (ViewGroup) child;

        child = getChildAt(1);
        if (!(child instanceof ViewGroup)) {
            throw new IllegalArgumentException("本容器的直接Child，必须是ViewGroup!");
        }
        //实例化内容显示区域
        contentContainer = (ViewGroup) child;
    }
}
