package com.hz.stickdoublerecyclerview.rv

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.hz.stickdoublerecyclerview.R
import java.lang.ref.WeakReference
import kotlin.math.max

class ChildRecyclerView : RecyclerView, OnChildFlingListener {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    /** true 开启滑动冲突处理*/
    var enableConflict = true
    private var draggingY = 0
    private var draggingTime = 0L

    /** 记录上次的parent,避免递归频繁*/
    private var parentView: WeakReference<com.hz.stickdoublerecyclerview.rv.OnFlingListener>? = null

    private var parentRecyclerView: ParentRecyclerView? = null

    init {
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                /** 滚动停止且到了顶部,快速滑动事件往上给parent view*/
                if (enableConflict && SCROLL_STATE_IDLE == newState) {
                    induceParentOfChildTopStatus()
                    val step = (System.currentTimeMillis() - draggingTime).toInt()
                    val speed = 1000 * draggingY / max(1000, step)
                    parentView?.get()?.onFling(speed)
                } else if (SCROLL_STATE_DRAGGING == newState) {
                    draggingY = 0
                    draggingTime = System.currentTimeMillis()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (SCROLL_STATE_DRAGGING == scrollState) {
                    draggingY += dy
                }
            }
        })
    }

    override fun onChildFling(speed: Int) {
        fling(0, speed)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (enableConflict) {
            induceParentOfChildTopStatus()
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun induceParentOfChildTopStatus() {
        /** true child在顶部*/
        (parentView?.get() ?: {
            var pv = parent
            while (pv != null) {
                if (pv is com.hz.stickdoublerecyclerview.rv.OnFlingListener) {
                    parentView = WeakReference(pv)
                    break
                }
                pv = pv.parent
            }
            pv as? com.hz.stickdoublerecyclerview.rv.OnFlingListener
        }())?.onScrollTop(!canScrollVertically(-1), top)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        connectToParent()
    }


    private fun connectToParent() {
        var viewPager: ViewPager? = null
        var lastTraverseView: View = this

        var parentView = this.parent as View
        while (parentView != null) {
            val parentClassName = parentView::class.java.canonicalName
            if (parentView is ViewPager) {
                // 使用ViewPager，parentView顺序如下：
                // ChildRecyclerView -> 若干View -> ViewPager -> 若干View -> ParentRecyclerView
                // 此处将ChildRecyclerView保存到ViewPager最直接的子View中
                if (lastTraverseView != this) {
                    // 这个tag会在ParentRecyclerView中用到
                    lastTraverseView.setTag(R.id.tag_saved_child_recycler_view, this)
                }

                // 碰到ViewPager，需要上报给ParentRecyclerView
                viewPager = parentView
            } else if (parentView is ParentRecyclerView) {
                // 碰到ParentRecyclerView，设置结束
                parentView.setInnerViewPager(viewPager)
                parentView.setChildPagerContainer(lastTraverseView)
                this.parentRecyclerView = parentView
                return
            }

            lastTraverseView = parentView
            parentView = parentView.parent as View
        }
    }
}