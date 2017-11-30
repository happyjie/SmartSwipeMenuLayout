package com.story.happyjie.smartswipemenulayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;

/**
 * Created by llj on 2017-09-11 .
 */

public class SmartSwipeMenuLayout extends ViewGroup{

    private static String TAG = "llj-->" + SmartSwipeMenuLayout.class.getSimpleName();

    private Scroller mScroller;     //滑动控制类
    private int mScaledTouchSlop;   //最小有效滑动距离
    private boolean isSwiping = false;  //是否正在滑动
    private float threshold = 0.5f;     //菜单打开的滑动阈值，超过这个值则打开菜单

    private static SmartSwipeMenuLayout mViewFocus;    //被打开的项，每次只能同时有一个菜单被打开,这个应该是static,多个SmartSwipeMenuLayout只能有一个获得焦点
    private static SwipeState mViewFocusSwipeState; //被打开的项的滑动状态

    private final ArrayList<View> mMatchParentChildren = new ArrayList<>(1);

    private int mLeftMenuViewId, mRightMenuViewId, mContentViewId;
    private View mLeftMenuView, mRightMenuView, mContentView;
    private boolean mLeftMenuEnable = false;    //默认不开启左侧菜单
    private boolean mRightMenuEnable = false;  //默认不开启右侧菜单

    public SmartSwipeMenuLayout(Context context) {
        this(context, null);
    }

    public SmartSwipeMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartSwipeMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mScaledTouchSlop = viewConfiguration.getScaledTouchSlop();
        mScroller = new Scroller(context);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SmartSwipeMenuLayout, defStyleAttr, 0);
        int count = typedArray.getIndexCount();
        try {
            for (int i = 0; i < count; i++) {
                int attr = typedArray.getIndex(i);
                if (attr == R.styleable.SmartSwipeMenuLayout_leftMenuView) {
                    mLeftMenuViewId = typedArray.getResourceId(R.styleable.SmartSwipeMenuLayout_leftMenuView, 0);
                } else if (attr == R.styleable.SmartSwipeMenuLayout_rightMenuView) {
                    mRightMenuViewId = typedArray.getResourceId(R.styleable.SmartSwipeMenuLayout_rightMenuView, 0);
                } else if (attr == R.styleable.SmartSwipeMenuLayout_contentView) {
                    mContentViewId = typedArray.getResourceId(R.styleable.SmartSwipeMenuLayout_contentView, 0);
                } else if (attr == R.styleable.SmartSwipeMenuLayout_leftMenuEnable) {
                    mLeftMenuEnable = typedArray.getBoolean(R.styleable.SmartSwipeMenuLayout_leftMenuEnable, true);
                } else if (attr == R.styleable.SmartSwipeMenuLayout_RightMenuEnable) {
                    mRightMenuEnable = typedArray.getBoolean(R.styleable.SmartSwipeMenuLayout_RightMenuEnable, true);
                }
            }
        } catch (Exception e) {
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

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            //跳过不需要显示的View
            if (child.getVisibility() != View.GONE) {
                //测量childView的margin
                LayoutParams layoutParams = child.getLayoutParams();
                MarginLayoutParams marginLayoutParams;
                if (layoutParams instanceof MarginLayoutParams) {
                    marginLayoutParams = (MarginLayoutParams) layoutParams;
                } else {
                    marginLayoutParams = new MarginLayoutParams(layoutParams);
                }
                child.setLayoutParams(marginLayoutParams);

                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);

                //通过每次循环时取较大值，来获取childView中宽度的最大值
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth() + marginLayoutParams.leftMargin
                        + marginLayoutParams.rightMargin);
                //通过每次循环时取较大值，来获取childView的最大高度
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight() + marginLayoutParams.leftMargin
                        + marginLayoutParams.rightMargin);

                childState = combineMeasuredStates(childState, child.getMeasuredState());

                //如果childView有MATCH_PARENT的，需要再次测量。此次先将其加入mMatchParentChildren集合中
                if (measureMatchParentChildren) {
                    if (marginLayoutParams.width == LayoutParams.MATCH_PARENT ||
                            marginLayoutParams.height == LayoutParams.MATCH_PARENT) {
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
        if (count > 1) {
            for (int i = 0; i < count; i++) {
                final View child = mMatchParentChildren.get(i);
                final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                //重新设置child测量所需的MeasureSpec对象
                final int childWidthMeasureSpec;
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    final int width = Math.max(0, getMeasuredWidth() - lp.leftMargin - lp.rightMargin);
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, lp.leftMargin + lp.rightMargin, lp.width);
                }

                final int childHeightMeasureSpec;
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    final int height = Math.max(0, getMeasuredHeight() - lp.topMargin - lp.bottomMargin);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, lp.topMargin + lp.bottomMargin, lp.height);
                }

                //重新测量
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    MarginLayoutParams mContentViewLayoutParam;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int left = getPaddingLeft();
        int top = getPaddingTop();

        //根据配置的id获取左侧菜单、右侧菜单及中间内容部分的View
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (null == mLeftMenuView && child.getId() == mLeftMenuViewId) {
                mLeftMenuView = child;
                mLeftMenuView.setClickable(true);
            } else if (null == mRightMenuView && child.getId() == mRightMenuViewId) {
                mRightMenuView = child;
                mRightMenuView.setClickable(true);
            } else if (null == mContentView && child.getId() == mContentViewId) {
                mContentView = child;
                mContentView.setClickable(true);
            }
        }

        //给mContentView布局，位于中间
        if (mContentView != null) {
            mContentViewLayoutParam = (MarginLayoutParams) mContentView.getLayoutParams();
            int cTop = top + mContentViewLayoutParam.topMargin;
            int cLeft = left + mContentViewLayoutParam.leftMargin;
            int cRight = cLeft + mContentView.getMeasuredWidth();
            int cBottom = cTop + mContentView.getMeasuredHeight();
            mContentView.layout(cLeft, cTop, cRight, cBottom);
        }

        //给mLeftMenuView布局（在左边，看不见）
        if (mLeftMenuView != null) {
            MarginLayoutParams leftViewLayoutParam = (MarginLayoutParams) mLeftMenuView.getLayoutParams();
            int lTop = top + leftViewLayoutParam.topMargin;
//            int lLeft = 0 - mLeftMenuView.getMeasuredWidth() + leftViewLayoutParam.leftMargin + leftViewLayoutParam.rightMargin;//?
            int lLeft = 0 - mLeftMenuView.getMeasuredWidth() - leftViewLayoutParam.rightMargin;
            int lRight = 0 - leftViewLayoutParam.rightMargin;
            int lBottom = lTop + mLeftMenuView.getMeasuredHeight();
            mLeftMenuView.layout(lLeft, lTop, lRight, lBottom);
        }

        //给mRightMenuView布局（在右边，看不见）
        if (mRightMenuView != null) {
            MarginLayoutParams rightViewLayoutParam = (MarginLayoutParams) mRightMenuView.getLayoutParams();
            int rTop = top + rightViewLayoutParam.topMargin;
            int rLeft = mContentView.getRight() + mContentViewLayoutParam.rightMargin + rightViewLayoutParam.leftMargin;
            int rRight = rLeft + mRightMenuView.getMeasuredWidth();
            int rBottom = rTop + mRightMenuView.getMeasuredHeight();
            mRightMenuView.layout(rLeft, rTop, rRight, rBottom);
        }
    }

    PointF mFirstPoint, mLastPoint;

    SwipeState swipeState;

    private float finallyDistanceX = 0;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.i("llj", "ACTION_DOWN--> x = " + ev.getRawX() + ", y = " + ev.getRawY());
                isSwiping = false;
                if (null == mLastPoint) {
                    mLastPoint = new PointF();
                }
                mLastPoint.set(ev.getRawX(), ev.getRawY());

                if (null == mFirstPoint) {
                    mFirstPoint = new PointF();
                }
                mFirstPoint.set(ev.getRawX(), ev.getRawY());

                if (null != mViewFocus) {
                    if (mViewFocus != this) {
                        mViewFocus.setSwipeState(SwipeState.STATE_NORMAL);
                    }

                    //告诉父布局不要拦截我的Touch事件
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE: {
//                Log.i("llj", "ACTION_MOVE--> x = " + ev.getRawX() + ", y = " + ev.getRawY());
                float distanceX = mLastPoint.x - ev.getRawX();
                float distanceY = mLastPoint.y - ev.getRawY();
                if (Math.abs(distanceY) > mScaledTouchSlop && Math.abs(distanceY) > Math.abs(distanceX)) {
                    break;
                }

                if (Math.abs(distanceX) > mScaledTouchSlop) {
                    scrollBy((int) (distanceX), 0);//滑动使用scrollBy
                }

                //越界修正
                if (getScrollX() < 0) { //→右滑，
                    if (!mLeftMenuEnable || null == mLeftMenuView) {
                        scrollTo(0, 0);
                    } else {//显示左侧菜单
                        if (getScrollX() < mLeftMenuView.getLeft()) {
                            scrollTo(mLeftMenuView.getLeft(), 0);
                        }
                    }
                } else if (getScrollX() > 0) {  //←左滑
                    if (!mRightMenuEnable || null == mRightMenuView) {
                        scrollTo(0, 0);
                    } else {
                        if (getScrollX() > mRightMenuView.getRight() - mContentView.getRight()
                                - mContentViewLayoutParam.rightMargin) {
                            scrollTo(mRightMenuView.getRight() - mContentView.getRight()
                                    - mContentViewLayoutParam.rightMargin, 0);
                        }
                    }
                }
                //当处于水平滑动时，禁止父类拦截
                if (Math.abs(distanceX) > mScaledTouchSlop) {
                    //  Log.i(TAG, ">>>>当处于水平滑动时，禁止父类拦截 true");
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                mLastPoint.set(ev.getRawX(), ev.getRawY());
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
//                Log.i("llj", (1 == ev.getAction() ? "ACTION_UP" : "ACTION_CANCEL") + "--> x = " + ev.getRawX() + ", y = " + ev.getRawY());
                finallyDistanceX = ev.getRawX() - mFirstPoint.x;
                if (Math.abs(finallyDistanceX) > mScaledTouchSlop) {
                    isSwiping = true;
                }
                swipeState = isShouldOpen();
//                Log.i("llj", "swipeState=" + swipeState);
                setSwipeState(swipeState);
                break;
            }

            default:
                break;
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                //滑动时拦截点击时间
                if (Math.abs(finallyDistanceX) > mScaledTouchSlop) {
                    // 当手指拖动值大于mScaledTouchSlop值时，认为应该进行滚动，拦截子控件的事件
                    //   Log.i(TAG, "<<<onInterceptTouchEvent true");
                    return true;
                }
//                if (Math.abs(finalyDistanceX) > mScaledTouchSlop || Math.abs(getScrollX()) > mScaledTouchSlop) {
//                    Log.d(TAG, "onInterceptTouchEvent: 2");
//                    return true;
//                }
                break;

            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                //滑动后不触发contentView的点击事件
                if (isSwiping) {
                    isSwiping = false;
                    finallyDistanceX = 0;
                    return true;
                }
            }

        }
        return super.onInterceptTouchEvent(event);
    }

    /**
     * 设置菜单打开状态
     *
     * @param swipeState
     */
    private void setSwipeState(SwipeState swipeState) {
        if (swipeState == SwipeState.STATE_LEFT_OPEN) {
            mScroller.startScroll(getScrollX(), 0, mLeftMenuView.getLeft() - getScrollX(), 0);
            mViewFocus = this;
            mViewFocusSwipeState = swipeState;
        } else if (swipeState == SwipeState.STATE_RIGHT_OPEN) {
            mViewFocus = this;
            mScroller.startScroll(getScrollX(), 0, mRightMenuView.getRight()
                    - mContentView.getRight() - mContentViewLayoutParam.rightMargin - getScrollX(), 0);
            mViewFocusSwipeState = swipeState;
        } else {
            mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0);
            mViewFocus = null;
            mViewFocusSwipeState = null;
        }
        invalidate();
    }

    /**
     * 不要忘记了重写这个方法，否则松手后无法实现自己滚动到指定位置，详细原因可以先去了解Scroller与computeScroll（）的关系
     */
    @Override
    public void computeScroll() {
        //判断Scroller是否执行完毕：
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            //通知View重绘-invalidate()->onDraw()->computeScroll()
            invalidate();
        }
    }


    /**
     * 根据当前的scrollX的值判断松开手后应处于何种状态
     *
     * @return
     */
    private SwipeState isShouldOpen() {
        //小于最小滑动距离，保持原来的状态
        if (!(mScaledTouchSlop < Math.abs(finallyDistanceX))) {
            return mViewFocusSwipeState;
        }
        if (finallyDistanceX > 0) {  //➡滑动
            //1、展开左边菜单
            //获得leftView的测量长度
            if (getScrollX() < 0 && mLeftMenuView != null) {
                if (Math.abs(mLeftMenuView.getWidth() * threshold) < Math.abs(getScrollX())) {
                    return SwipeState.STATE_LEFT_OPEN;
                }
            }
            //2、关闭右边按钮  ???
            if (getScrollX() > 0 && mRightMenuView != null) {
                if (Math.abs(mRightMenuView.getWidth() * threshold) > Math.abs(getScaleX())) { //此处应该是>
                    return SwipeState.STATE_NORMAL;
                }
            }
        } else if (finallyDistanceX < 0) { //⬅滑动
            //3、开启右边菜单按钮
            if (getScrollX() > 0 && mRightMenuView != null) {
                if (Math.abs(mRightMenuView.getWidth() * threshold) < Math.abs(getScrollX())) {
                    return SwipeState.STATE_RIGHT_OPEN;
                }
            }
            //关闭左边 ???
            if (getScrollX() < 0 && mLeftMenuView != null) {
                if (Math.abs(mLeftMenuView.getWidth() * threshold) > Math.abs(getScaleX())) {   //此处应该是>
                    return SwipeState.STATE_NORMAL;
                }
            }
        }

        return SwipeState.STATE_NORMAL;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this == mViewFocus) {
            mViewFocus.setSwipeState(mViewFocusSwipeState);
        }
    }

    public void resetStatus() {
        if (mViewFocus != null) {
            if (mViewFocusSwipeState != null && mViewFocusSwipeState != SwipeState.STATE_NORMAL && mScroller != null) {
                mScroller.startScroll(mViewFocus.getScrollX(), 0, -mViewFocus.getScrollX(), 0);
                mViewFocus.invalidate();
                mViewFocus = null;
                mViewFocusSwipeState = null;
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this == mViewFocus) {
            mViewFocus.setSwipeState(SwipeState.STATE_NORMAL);
        }
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
