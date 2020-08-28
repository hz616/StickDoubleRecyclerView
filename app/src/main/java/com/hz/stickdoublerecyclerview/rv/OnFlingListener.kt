
package com.hz.stickdoublerecyclerview.rv

internal interface OnFlingListener {
    fun onScrollTop(isTop: Boolean,top:Int)
    fun onFling(childSpeed: Int)
}