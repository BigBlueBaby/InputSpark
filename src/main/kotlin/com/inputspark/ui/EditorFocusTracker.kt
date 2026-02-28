package com.inputspark.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.UIUtil
import com.intellij.util.messages.MessageBusConnection
import java.awt.Component
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

/**
 * 编辑器焦点追踪器
 * 用于监听编辑器焦点变化，检测用户是否在编辑器内部
 *
 * @author 林龙祥
 * @since 2026-02-28
 */
object EditorFocusTracker {
    
    // 用于存储每个项目的监听器连接，避免重复注册
    private val projectConnections = ConcurrentHashMap<Project, MessageBusConnection>()
    
    /**
     * 检查当前焦点是否在编辑器内
     */
    fun isFocusInsideEditor(project: Project?): Boolean {
        if (project == null || project.isDisposed) {
            return false
        }
        
        val focusOwner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
        if (focusOwner == null) return false
        
        val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor
        if (currentEditor == null) return false
        
        val editorComponent = (currentEditor as EditorEx).contentComponent
        return UIUtil.isDescendingFrom(focusOwner, editorComponent)
    }

    /**
     * 注册焦点变化监听器
     */
    fun addFocusListener(project: Project?, onFocusChanged: Consumer<Boolean>) {
        if (project == null || project.isDisposed) {
            return
        }
        
        // 避免重复注册
        if (projectConnections.containsKey(project)) {
            return
        }
        
        val connection = project.messageBus.connect()
        projectConnections[project] = connection
        
        // 监听编辑器切换事件
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                // 当编辑器切换时，重新为新编辑器添加焦点监听
                addFocusListenerToCurrentEditor(project, onFocusChanged)
            }
        })
        
        // 为当前编辑器添加焦点监听
        addFocusListenerToCurrentEditor(project, onFocusChanged)
        
        // 当项目关闭时清理连接
        Disposer.register(project) {
            connection.disconnect()
            projectConnections.remove(project)
        }
    }
    
    /**
     * 为当前编辑器添加焦点监听器
     */
    private fun addFocusListenerToCurrentEditor(project: Project, onFocusChanged: Consumer<Boolean>) {
        if (project.isDisposed) return
        
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor is EditorEx) {
            val contentComponent = editor.contentComponent
            
            // 移除之前的监听器（如果有的话）
            val existingListeners = contentComponent.focusListeners
            for (listener in existingListeners) {
                if (listener is EditorFocusListener) {
                    contentComponent.removeFocusListener(listener)
                }
            }
            
            // 添加新的焦点监听器
            contentComponent.addFocusListener(EditorFocusListener(onFocusChanged))
        }
    }
    
    /**
     * 自定义焦点监听器类，便于识别和管理
     */
    private class EditorFocusListener(private val onFocusChanged: Consumer<Boolean>) : FocusAdapter() {
        override fun focusGained(e: FocusEvent) {
            onFocusChanged.accept(true)
        }

        override fun focusLost(e: FocusEvent) {
            onFocusChanged.accept(false)
        }
    }
    
    /**
     * 清理指定项目的监听器
     */
    fun removeFocusListener(project: Project?) {
        val connection = projectConnections.remove(project)
        connection?.disconnect()
    }
}