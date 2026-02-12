package com.inputspark.ui

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import javax.swing.SwingUtilities

/**
 * 应用生命周期监听器
 * 负责在 IDE 启动时注册全局光标监听器
 *
 * @author 林龙祥
 * @since 2026-02-12
 */
class InputSparkAppLifecycleListener : AppLifecycleListener {

    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        // println("InputSpark: AppLifecycleListener - App Frame Created!")
        
        // 延迟注册，确保 EditorFactory 已就绪
        SwingUtilities.invokeLater {
            registerGlobalListener()
        }
    }
    
    private fun registerGlobalListener() {
        val editorFactory = EditorFactory.getInstance()
        
        // 1. 为现有编辑器注册监听器
        // 注意：我们需要为每个编辑器创建一个独立的 Listener 实例，因为 Listener 内部维护了状态（lastContextType）
        for (editor in editorFactory.allEditors) {
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
        
        // println("InputSpark: Global listeners registered successfully.")
    }
    
    private fun attachListener(editor: Editor) {
        try {
            // 创建新的监听器实例，维护独立的状态
            val listener = InputSparkCaretListener()
            editor.caretModel.addCaretListener(listener)
            // println("InputSpark: Listener attached to editor ${editor.hashCode()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
