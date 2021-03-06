package com.zzw.customview.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import com.zzw.customview.R;
import com.zzw.customview.view.utils.DisplayUtil;

/**
 * Created by zzw on 2017/6/20.
 * Version:
 * Des:酷狗侧滑菜单
 */

public class SkiddingMenuLayout extends HorizontalScrollView {

    // 菜单的宽度
    private int mMenuWidth;

    private View mMenuView, mContentView;

    //处理快速滑动
    private GestureDetector mGestureDetector;

    //菜单是否打开
    private boolean mMenuIsOpen;

    //是否拦截
    private boolean isIntercept;

    public SkiddingMenuLayout(Context context) {
        this(context, null);
    }

    public SkiddingMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SkiddingMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);


        // 初始化自定义属性
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SkiddingMenuLayout);

        float rightMargin = array.getDimension(
                R.styleable.SkiddingMenuLayout_menuRightMargin, DisplayUtil.dip2px(context, 50));
        // 菜单页的宽度是 = 屏幕的宽度 - 右边的一小部分距离（自定义属性）
        mMenuWidth = (int) (DisplayUtil.getScreenWidth(context) - rightMargin);
        array.recycle();

        //处理快速滑动
        mGestureDetector = new GestureDetector(context, mOnGestureListener);
    }


    //xml 布局解析完毕回调的方法
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //指定宽高
        //先拿到整体容器
        ViewGroup container = (ViewGroup) getChildAt(0);

        int childCount = container.getChildCount();
        if (childCount != 2)
            throw new RuntimeException("只能放置两个子View");
        //菜单
        mMenuView = container.getChildAt(0);
        ViewGroup.LayoutParams meauParams = mMenuView.getLayoutParams();
        meauParams.width = mMenuWidth;
        //7.0一下的不加这句代码是正常的   7.0以上的必须加
        mMenuView.setLayoutParams(meauParams);

        //内容页
        mContentView = container.getChildAt(1);
        ViewGroup.LayoutParams contentParams = mContentView.getLayoutParams();
        contentParams.width = DisplayUtil.getScreenWidth(getContext());
        //7.0一下的不加这句代码是正常的   7.0以上的必须加
        mContentView.setLayoutParams(contentParams);

        //进入是关闭状态  在这里调用没用  在onLayout之后调用
        // scrollTo(mMenuWidth, 0);

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        //进入是关闭状态
        scrollTo(mMenuWidth, 0);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        isIntercept = false;
        if (mMenuIsOpen && ev.getX() > mMenuWidth) {//打开状态  触摸右边关闭
            isIntercept = true;//拦截的话就不执行自己的onTouchEvent
            closeMenu();
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (isIntercept)//拦截的话就不执行自己的onTouchEvent
            return true;

        if (mGestureDetector.onTouchEvent(ev))//快速滑动触发了下面的就不要执行了
            return true;


        //抬起的时候判断是关闭还是开启  根据当前滚动的距离来判断
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            int currentScrollX = getScrollX();
            if (currentScrollX > mMenuWidth / 2) {
                closeMenu();
            } else {
                openMenu();
            }
            //让其不执行super
            return true;
        }
        return super.onTouchEvent(ev);
    }

    //滑动改变触发
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

//        //抽屉效果  两种一样
//        ViewCompat.setTranslationX(mMenuView, l);
//        ViewCompat.setX(mMenuView, l);

//        Log.e("zzz", "l->" + l + " t->" + t + " oldl->" + oldl + " oldt->" + oldt);
        //主要看l  手指从左往右滑动 由大变小
        //计算一个梯度值 1->0
        float scale = 1.0f * l / mMenuWidth;

        //酷狗侧滑效果...
//        //右边的缩放 最小是0.7f ，最大是1.0f
        float rightScale = 0.7f + 0.3f * scale;
        //设置mContentView缩放的中心点位置
        ViewCompat.setPivotX(mContentView, 0);
        ViewCompat.setPivotY(mContentView, mContentView.getHeight() / 2);
        //设置右边缩放
        ViewCompat.setScaleX(mContentView, rightScale);
        ViewCompat.setScaleY(mContentView, rightScale);

        //菜单
        //透明度是半透明到全透明  0.5f-1.0f
        float alpha = 0.5f + (1.0f - scale) * 0.5f;
        ViewCompat.setAlpha(mMenuView, alpha);

        //缩放  0.7-1.0
        float leftScale = 0.7f + 0.3f * (1 - scale);
        ViewCompat.setScaleX(mMenuView, leftScale);
        ViewCompat.setScaleY(mMenuView, leftScale);

        //退出按钮在右边
        ViewCompat.setTranslationX(mMenuView, 0.2f * l);
    }

    //打开菜单
    public void openMenu() {
        smoothScrollTo(0, 0);
        mMenuIsOpen = true;
    }

    //关闭菜单
    public void closeMenu() {
        smoothScrollTo(mMenuWidth, 0);
        mMenuIsOpen = false;
    }

    //快速滑动
    private GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //快速滑动回调
            //打开的时候从右到左滑动关闭   关闭的时候从左往右打开
//            Log.e("zzz", "velocityX->" + velocityX);
            // >0 从左往右边滑动  <0 从右到左
            if (mMenuIsOpen) {
                if (velocityX < 0) {
                    closeMenu();
                    return true;
                }
            } else {
                if (velocityX > 0) {
                    openMenu();
                    return true;
                }
            }

            return super.onFling(e1, e2, velocityX, velocityY);
        }
    };

}
