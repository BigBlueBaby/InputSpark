package com.inputspark.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.inputspark.services.InputMethodSwitcher
import com.intellij.openapi.components.service

/**
 * 测试动作类
 * 用于验证InputSpark插件是否正常工作
 *
 * @author 林龙祥
 * @since 2026-02-24
 */
class TestToggleAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        try {
            // 测试服务是否正常
            val switcher = service<InputMethodSwitcher>()
            
            // 切换到英文
            val switchedToEnglish = switcher.switchToEnglish()
            
            // 显示测试结果
            val message = if (switchedToEnglish) {
                "InputSpark 测试成功！\n已切换到英文输入法"
            } else {
                "InputSpark 测试成功！\n当前已是英文输入法"
            }
            
            Messages.showInfoMessage(
                e.project,
                message,
                "InputSpark 测试"
            )
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                e.project,
                "测试失败: ${ex.message}",
                "InputSpark 测试"
            )
        }
    }
}
