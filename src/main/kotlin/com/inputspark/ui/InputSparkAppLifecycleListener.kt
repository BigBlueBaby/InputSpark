package com.inputspark.ui

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFrame
import com.intellij.util.concurrency.AppExecutorUtil
import com.inputspark.model.CaretState
import com.inputspark.model.PluginConfig
import com.inputspark.model.SceneType
import com.inputspark.services.ConfigurationManager
import com.inputspark.services.ContextAnalyzer
import com.inputspark.services.InputMethodSwitcher
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

/**
 * 应用生命周期监听器
 * 负责在 IDE 启动时注册全局光标监听器和应用激活监听器
 *
 * @author 林龙祥
 * @since 2026-02-12
 */
class InputSparkAppLifecycleListener : AppLifecycleListener {

    private companion object {
        /**
         * 激活恢复延迟（毫秒）
         * 避免 ALT+TAB 切换窗口时，焦点尚未稳定就提前模拟切换热键
         */
        private const val ACTIVATION_SWITCH_DELAY_MS = 180L
    }

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
                    // IDE 窗口激活时，不再无条件切到英文，而是在焦点恢复稳定后按真实编辑器上下文恢复输入法
                    scheduleRestoreInputMethod(ideFrame.project)
                }

                override fun applicationDeactivated(ideFrame: IdeFrame) {
                    isOutsideIde = true
                    // 离开 IDE 时切换到中文输入法
                    switchToTargetInputMethod(CaretState.OUTSIDE_IDE)
                }
            })
    }

    /**
     * 调度-恢复输入法
     * 在 IDE 从后台切回前台后延迟恢复，避免 ALT+TAB 场景下误触发系统输入法切换热键
     */
    private fun scheduleRestoreInputMethod(project: Project?) {
        // 当前项目：为空时无需继续恢复流程
        val currentProject = project ?: return
        if (currentProject.isDisposed) {
            return
        }

        AppExecutorUtil.getAppScheduledExecutorService().schedule({
            SwingUtilities.invokeLater {
                restoreInputMethodForActiveEditor(currentProject)
            }
        }, ACTIVATION_SWITCH_DELAY_MS, TimeUnit.MILLISECONDS)
    }

    /**
     * 恢复-当前编辑器输入法
     * 仅在焦点真正回到文本编辑器后，才根据光标所在上下文恢复对应输入法
     */
    private fun restoreInputMethodForActiveEditor(project: Project) {
        if (project.isDisposed) {
            return
        }

        // 配置管理器：用于读取插件启用状态与场景开关
        val configurationManager = service<ConfigurationManager>()
        // 插件配置：包含当前场景切换策略
        val pluginConfig = configurationManager.getConfig()
        if (!pluginConfig.enabled) {
            return
        }

        // 当前文本编辑器：只有真正回到编辑器后才允许恢复输入法
        val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        // 当前焦点是否在编辑器内：ALT+TAB 焦点未稳定时直接跳过，避免误按切换热键
        val isFocusInsideEditor = EditorFocusTracker.isFocusInsideEditor(project)
        if (!isFocusInsideEditor) {
            return
        }
        // 当前是否存在选区：Shift 扩选、鼠标拖拽期间不应自动切换输入法
        val hasSelection = currentEditor.selectionModel.hasSelection()
        if (hasSelection) {
            return
        }

        // 上下文分析器：用于识别当前光标所在的真实编辑场景
        val contextAnalyzer = service<ContextAnalyzer>()
        // 当前光标偏移量：作为上下文分析的定位位置
        val caretOffset = currentEditor.caretModel.offset
        // 当前上下文类型：由 PSI 与编辑器信息共同推导
        val contextType = contextAnalyzer.analyzeContext(currentEditor, caretOffset)
        // 当前光标状态：复用既有上下文到状态的映射规则，保证激活恢复与日常光标切换一致
        val currentCaretState = CaretState.fromContextType(contextType)
        restoreInputMethodByCaretState(pluginConfig, currentCaretState)
    }

    /**
     * 恢复-目标输入法
     * 根据当前光标状态与场景开关决定是否恢复中文或英文，避免重新激活 IDE 时一律强制英文
     */
    private fun restoreInputMethodByCaretState(pluginConfig: PluginConfig, currentCaretState: CaretState) {
        when (currentCaretState) {
            CaretState.IN_COMMENT_LINE, CaretState.IN_COMMENT_BLOCK -> {
                // 注释场景是否启用：关闭时不做任何自动恢复
                val isCommentSceneEnabled = pluginConfig.sceneConfig[SceneType.COMMENT.name] == true
                if (isCommentSceneEnabled) {
                    switchToTargetInputMethod(CaretState.IN_COMMENT_LINE)
                }
            }
            CaretState.IN_GIT_COMMIT -> {
                // 提交信息场景是否启用：启用后恢复为中文输入法
                val isGitCommitSceneEnabled = pluginConfig.sceneConfig[SceneType.GIT_COMMIT.name] == true
                if (isGitCommitSceneEnabled) {
                    switchToTargetInputMethod(CaretState.IN_GIT_COMMIT)
                }
            }
            CaretState.IN_STRING_LITERAL -> {
                // 字符串场景是否启用英文切换：启用后恢复为英文输入法
                val isStringSceneEnabled = pluginConfig.sceneConfig[SceneType.STRING_LITERAL.name] == true
                if (isStringSceneEnabled) {
                    switchToTargetInputMethod(CaretState.IN_STRING_LITERAL)
                }
            }
            CaretState.IN_CODE, CaretState.VIM_NORMAL_MODE, CaretState.VIM_INSERT_MODE -> {
                // 默认代码场景是否启用：启用后恢复为英文输入法
                val isDefaultSceneEnabled = pluginConfig.sceneConfig[SceneType.DEFAULT.name] == true
                if (isDefaultSceneEnabled) {
                    switchToTargetInputMethod(CaretState.IN_CODE)
                }
            }
            CaretState.IN_TOOL_WINDOW, CaretState.OUTSIDE_IDE -> {
                return
            }
        }
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
