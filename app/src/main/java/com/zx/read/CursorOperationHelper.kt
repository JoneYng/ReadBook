package com.zx.read

import android.view.MotionEvent
import android.view.View
import com.zx.read.ui.PageView
import com.zx.readbook.R


/**
 * @description:
 * @author: zhouxiang
 * @created: 2025/03/09 10:59
 * @version: V1.0
 */
/**
 * 处理光标移动选择操作
 * @param event 触摸事件对象，用于获取原始坐标
 * @param cursorLeft 左侧光标视图，用于获取宽度和高度
 * @param cursorRight 右侧光标视图，用于获取宽度和高度
 * @param pageView 包含当前页面的视图容器
 * @param onFinalAction 最终操作的回调（可选）
 */
fun handleCursorSelection(
    event: MotionEvent,
    cursorLeft: View,
    cursorRight: View,
    cursorView: View,
    pageView: PageView,
    onFinalAction: () -> Unit = {}
) {
    // 解构获取触摸点原始坐标（基于屏幕坐标系）
    val (startX, startY) = event.rawX to event.rawY

    // 计算光标控件尺寸（右光标宽度用于后续偏移计算）
    val cursorWidth = cursorRight.width
    val cursorHeight = cursorRight.height

    // region 预计算关键坐标点
    // 结束移动目标坐标：当前触点坐标 - 右光标尺寸（实现光标吸附效果）
    val endMoveOffsetX = startX - cursorWidth
    val endMoveOffsetY = startY - cursorHeight

    // 开始移动目标坐标：当前触点坐标 + 左光标宽度（保持水平间距）
    // Y轴使用左光标高度进行垂直对齐调整
    val startMoveOffsetX = startX + cursorLeft.width
    val startMoveOffsetY = startY - cursorLeft.height
    // endregion

    when (cursorView.id) {
        R.id.cursor_left ->
            // 左光标操作序列：启动移动 -> 二次启动 -> 结束移动 -> 最终回调
            pageView.curPage.selectStartMove(
                startMoveOffsetX,
                startMoveOffsetY
            ) {
                // 定义操作链步骤（使用lambda延迟执行特性）
                val selectEndStep = {
                    pageView.curPage.selectEndMove(
                        endMoveOffsetX,
                        endMoveOffsetY,
                        onFinalAction  // 将最终回调接入操作链末端
                    )
                }

                val initialSelectStart = {
                    pageView.curPage.selectStartMove(
                        startMoveOffsetX,
                        startMoveOffsetY,
                        selectEndStep  // 将结束移动接入回调
                    )
                }

                // 执行连锁操作：启动移动 -> (回调中)结束移动
                initialSelectStart()
            }

        R.id.cursor_right -> {
            // 右光标操作序列：结束移动 -> 启动移动 -> 最终回调
            val selectStartStep = {
                pageView.curPage.selectStartMove(
                    startMoveOffsetX,
                    startMoveOffsetY,
                    onFinalAction  // 最终回调接入操作链末端
                )
            }
            val initialSelectEnd = {
                pageView.curPage.selectEndMove(
                    endMoveOffsetX,
                    endMoveOffsetY,
                    selectStartStep  // 将启动移动接入回调
                )
            }
            // 执行连锁操作：结束移动 -> (回调中)启动移动
            initialSelectEnd()
        }
    }
}
