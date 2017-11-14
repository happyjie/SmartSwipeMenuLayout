package com.story.happyjie.swipemenulayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;

/**
 * Created by llj on 2017-09-11 .
 */

public class SmartSwipeMenuLayout extends ViewGroup{

    private static String TAG = "llj-->"+ SmartSwipeMenuLayout.class.getSimpleName();

    private Scroller mScroller;     //滑动控制类
    private int mScaledTouchSlop;   //最小有效滑动距离

    private final ArrayList<View> mMatchParentChildren = new ArrayList<>(1);

    private View mLeftMenuView, mRightMenuView, mContentView;
    private boolean mLeftMenuEnable = false;    //默认不开启左侧菜单
    private boolean mRightMenuEnable = false;  //默认不开启右侧菜单

    public SmartSwipeMenuLayout(Context context) {
        super(context, null);
    }

    public SmartSwipeMenuLayout(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public SmartSwipeMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr){
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mScaledTouchSlop = viewConfiguration.getScaledTouchSlop();
        mScroller = new Scroller(context);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SmartSwipeMenuLayout, defStyleAttr, 0);
        int count = typedArray.getIndexCount();
        try {
            for (int i = 0; i < count; i++) {
                int attr = typedArray.getIndex(i);
                if(attr == R.styleable.SmartSwipeMenuLayout_LeftMenuView){
                    int leftViewId = typedArray.getResourceId(R.styleable.SmartSwipeMenuLayout_LeftMenuView, 0);
                    mLeftMenuView = findViewById(leftViewId);
                } else if (attr == R.styleable.SmartSwipeMenuLayout_RightMenuView){
                    int rightViewId = typedArray.getResourceId(R.styleable.SmartSwipeMenuLayout_RightMenuView, 0);
                    mRightMenuView = findViewById(rightViewId);
                } else if(attr == R.styleable.SmartSwipeMenuLayout_ContentView){
                    int contentViewId = typedArray.getResourceId(R.styleable.SmartSwipeMenuLayout_ContentView, 0);
                    mContentView = findViewById(contentViewId);
                } else if(attr == R.styleable.SmartSwipeMenuLayout_LeftMenuEnable){
                    mLeftMenuEnable = typedArray.getBoolean(R.styleable.SmartSwipeMenuLayout_LeftMenuEnable, true);
                } else if(attr == R.styleable.SmartSwipeMenuLayout_RightMenuEnable){
                    mRightMenuEnable = typedArray.getBoolean(R.styleable.SmartSwipeMenuLayout_RightMenuEnable, true);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            typedArray.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int childCount = getChildCount();

        //判断parent是不是Exactly模式,
        final boolean measureMatchParentChildren =
                (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                        MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY);

        //清理存储模式为MATCH_PARENT的队列
        mMatchParentChildren.clear();

        int maxWidth = 0;
        int maxHeight = 0;
        int childState = 0;

        for(int i = 0; i < childCount; i++){
            View child = getChildAt(i);

            //跳过不需要显示的View
            if(child.getVisibility() != View.GONE){
                //测量childView的margin
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                MarginLayoutParams marginLayoutParams = (MarginLayoutParams) child.getLayoutParams();
                //通过每次循环时取较大值，来获取childView中宽度的最大值
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth() + marginLayoutParams.leftMargin
                    + marginLayoutParams.rightMargin);
                //通过每次循环时取较大值，来获取childView的最大高度
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight() + marginLayoutParams.leftMargin
                    + marginLayoutParams.rightMargin);

                childState = combineMeasuredStates(childState, child.getMeasuredState());

                //如果childView有MATCH_PARENT的，需要再次测量。此次先将其加入mMatchParentChildren集合中
                if(measureMatchParentChildren){
                    if(marginLayoutParams.width == LayoutParams.MATCH_PARENT ||
                            marginLayoutParams.height == LayoutParams.MATCH_PARENT){
                        mMatchParentChildren.add(child);
                    }
                }
            }
        }

        //最大宽度及高度还需要考虑parent的背景的大小
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());

        //设置parent的具体宽高
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));

        //parent的宽高已经知道了，前面MATCH_PARENT的childView的值也就知道了，所以需要再次测量它
        int count = mMatchParentChildren.size();
        if(count > 1){
            for(int i = 0; i < count; i++){
                final View child = mMatchParentChildren.get(i);
                final  MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                //重新设置child测量所需的MeasureSpec对象
                final int childWidthMeasureSpec;
                if(lp.width == LayoutParams.MATCH_PARENT){
                    final int width = Math.max(0, getMeasuredWidth() - lp.leftMargin -lp.rightMargin);
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, lp.leftMargin + lp.rightMargin,
                            lp.width);
                }

                final int childHeightMeasureSpec;
                if(lp.height == LayoutParams.MATCH_PARENT){
                    final int height = Math.max(0, getMeasuredHeight() - lp.topMargin - lp.bottomMargin);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, lp.topMargin + lp.bottomMargin,
                            lp.height);
                }

                //重新测量
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }


    public View getmLeftMenuView() {
        return mLeftMenuView;
    }

    public View getmRightMenuView() {
        return mRightMenuView;
    }

    public View getmContentView() {
        return mContentView;
    }

    public boolean ismLeftMenuEnable() {
        return mLeftMenuEnable;
    }

    public boolean ismRightMenuEnable() {
        return mRightMenuEnable;
    }
}
