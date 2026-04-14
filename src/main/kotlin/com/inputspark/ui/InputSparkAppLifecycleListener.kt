package com.inputspark.ui

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.wm.IdeFrame
import com.inputspark.model.CaretState
import com.inputspark.services.InputMethodSwitcher
import javax.swing.SwingUtilities

/**
 * 应用生命周期监听器
 * 负责在 IDE 启动时注册全局光标监听器和应用激活监听器
 *
 * @author 林龙祥
 * @since 2026-02-12
 */
class InputSparkAppLifecycleListener : AppLifecycleListener {

    // 当前是否在 IDE 外部
    private var isOutsideIde = false

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        val application = ApplicationManager.getApplication()
        if (application.isHeadlessEnvironment || application.isCommandLine) {
            return
        }

        // 延迟注册，确保 EditorFactory 已就绪
        SwingUtilities.invokeLater {
            registerGlobalListener()
            registerApplicationActivationListener()
        }
    }
    
    private fun registerGlobalListener() {
        val editorFactory = EditorFactory.getInstance()
        val editors = editorFactory.allEditors.toList()
        
        // 1. 为现有编辑器注册监听器
        // 注意：我们需要为每个编辑器创建一个独立的 Listener 实例，因为 Listener 内部维护了状态（lastContextType）
        for (editor in editors) {
            attachListener(editor)
        }
        
        // 2. 注册全局工厂监听，为新打开的编辑器注册监听器
        editorFactory.addEditorFactoryListener(object : EditorFactoryListener {
            override fun editorCreated(event: EditorFactoryEvent) {
                attachListener(event.editor)
            }
            override fun editorReleased(event: EditorFactoryEvent) {
                // 编辑器关闭时，CaretListener 通常会自动解绑，或者随 Editor 销毁
            }
        }, ApplicationManager.getApplication()) // 绑定到 Application 生命周期
    }
    
    private fun registerApplicationActivationListener() {
        // 注册应用激活/失活监听器，用于检测 IDE 窗口是否激活
        ApplicationManager.getApplication().messageBus.connect()
            .subscribe(ApplicationActivationListener.TOPIC, object : ApplicationActivationListener {
                override fun applicationActivated(ideFrame: IdeFrame) {
                    isOutsideIde = false
                    // IDE 窗口激活时，如果之前在 IDE 外，需要切换回编辑器状态
                    if (ideFrame.project != null) {
                        // 延迟切换，确保焦点已恢复
                        SwingUtilities.invokeLater {
                            switchToTargetInputMethod(CaretState.IN_CODE)
                        }
                    }
                }

                override fun applicationDeactivated(ideFrame: IdeFrame) {
                    isOutsideIde = true
                    // 离开 IDE 时切换到中文输入法
                    switchToTargetInputMethod(CaretState.OUTSIDE_IDE)
                }
            })
    }
    
    private fun attachListener(editor: Editor) {
        try {
            // 创建新的监听器实例，维护独立的状态
            val listener = InputSparkCaretListener()
            editor.caretModel.addCaretListener(listener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun switchToTargetInputMethod(state: CaretState) {
        val switcher = service<InputMethodSwitcher>()
        when (state.targetInputMethod) {
            com.inputspark.model.InputMethodType.CHINESE -> switcher.switchToChinese()
            com.inputspark.model.InputMethodType.ENGLISH -> switcher.switchToEnglish()
            else -> switcher.switchToEnglish() // 默认切换到英文
        }
    }
}
