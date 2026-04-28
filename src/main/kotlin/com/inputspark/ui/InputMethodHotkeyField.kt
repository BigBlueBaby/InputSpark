package com.inputspark.ui

import com.intellij.ui.components.JBTextField
import com.inputspark.core.windows.InputMethodToggleHotkeyParser
import java.awt.KeyboardFocusManager
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.KeyStroke

/**
 * 输入法切换热键录入框
 * 点击聚焦后直接捕获用户按下的单键或组合键，并转换为标准配置文本
 *
 * @author 林龙祥
 * @since 2026-04-28
 */
class InputMethodHotkeyField(columns: Int) : JBTextField(columns) {

    /** 当前处于按下状态的修饰键集合 */
    private val pressedModifierKeyCodeSet = linkedSetOf<Int>()

    /** 热键事件分发器 */
    private val hotkeyDispatcher = java.awt.KeyEventDispatcher { event ->
        if (!isFocusOwner) {
            return@KeyEventDispatcher false
        }
        when (event.id) {
            KeyEvent.KEY_PRESSED -> {
                updateModifierPressedState(event, true)
                handleHotkeyEvent(event)
                true
            }
            KeyEvent.KEY_RELEASED -> {
                updateModifierPressedState(event, false)
                false
            }
            KeyEvent.KEY_TYPED -> {
                if (event.keyChar == KeyEvent.CHAR_UNDEFINED) {
                    return@KeyEventDispatcher false
                }
                if (event.keyChar != ' ') {
                    return@KeyEventDispatcher false
                }
                handleHotkeyEvent(event)
                true
            }
            else -> false
        }
    }

    init {
        isEditable = false
        focusTraversalKeysEnabled = false
        toolTipText = "点击后直接按下中英文切换快捷键"
        registerCommonShortcutActions()

        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(hotkeyDispatcher)
            }

            override fun focusLost(e: FocusEvent) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(hotkeyDispatcher)
            }
        })
    }

    override fun removeNotify() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(hotkeyDispatcher)
        pressedModifierKeyCodeSet.clear()
        super.removeNotify()
    }

    /**
     * 处理-热键事件
     * 聚焦期间优先拦截按键事件，避免 Ctrl+Space 之类的组合键被上层快捷键系统吞掉
     *
     * @param event 当前按键事件
     */
    private fun handleHotkeyEvent(event: KeyEvent) {
        if (!shouldCaptureHotkeyEvent(event)) {
            return
        }
        event.consume()
        text = captureHotkeyText(event)
        selectAll()
    }

    /**
     * 判断是否需要捕获当前热键事件
     * 仅在主键就绪时写入文本，避免按下 Ctrl/Shift 后又被空格组合覆盖前出现异常抖动
     *
     * @param event 当前按键事件
     * @return 是否应当捕获
     */
    private fun shouldCaptureHotkeyEvent(event: KeyEvent): Boolean {
        if (event.id == KeyEvent.KEY_TYPED) {
            return event.keyChar == ' '
        }
        val keyCode = event.keyCode
        if (isModifierKey(keyCode)) {
            return true
        }
        return keyCode != KeyEvent.VK_UNDEFINED
    }

    /**
     * 注册-常见快捷键动作
     * 显式为容易被 IDE 或系统抢占的常用组合键建立聚焦态动作绑定，优先提升录入成功率
     */
    private fun registerCommonShortcutActions() {
        registerPresetShortcutAction(
            actionKey = "inputspark.hotkey.ctrl.space",
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK),
            hotkeyText = "Ctrl + Space"
        )
    }

    /**
     * 注册-预设快捷键动作
     * 当组件聚焦时直接用 Swing ActionMap 处理指定快捷键，避免其落入默认补全等上层动作
     *
     * @param actionKey 动作键名
     * @param keyStroke 快捷键描述
     * @param hotkeyText 目标热键文本
     */
    private fun registerPresetShortcutAction(actionKey: String, keyStroke: KeyStroke, hotkeyText: String) {
        getInputMap(WHEN_FOCUSED).put(keyStroke, actionKey)
        actionMap.put(actionKey, object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                text = InputMethodToggleHotkeyParser.parse(hotkeyText).displayText
                selectAll()
            }
        })
    }

    /**
     * 捕获-热键文本
     * 将当前键盘事件转换为标准化的热键配置文本
     *
     * @param event 当前按键事件
     * @return 标准化后的热键配置文本
     */
    private fun captureHotkeyText(event: KeyEvent): String {
        // 热键片段列表：用于按标准顺序组装组合键文本
        val hotkeyTokenList = mutableListOf<String>()
        appendModifierTokenIfPressed(hotkeyTokenList, KeyEvent.VK_CONTROL, "Ctrl", event)
        appendModifierTokenIfPressed(hotkeyTokenList, KeyEvent.VK_ALT, "Alt", event)
        appendModifierTokenIfPressed(hotkeyTokenList, KeyEvent.VK_SHIFT, "Shift", event)
        appendModifierTokenIfPressed(hotkeyTokenList, KeyEvent.VK_META, "Meta", event)
        appendModifierTokenIfPressed(hotkeyTokenList, KeyEvent.VK_WINDOWS, "Meta", event)

        // 当前主键文本：针对空格键兼容 KEY_TYPED 场景，其余按键沿用 KeyEvent 的按键名
        val primaryKeyText = resolvePrimaryKeyText(event)
        if (primaryKeyText != null && !hotkeyTokenList.contains(primaryKeyText)) {
            hotkeyTokenList.add(primaryKeyText)
        }

        // 原始热键文本：交给统一解析器做标准化与合法性校验
        val rawHotkeyText = hotkeyTokenList.joinToString("+")
        return InputMethodToggleHotkeyParser.parse(rawHotkeyText).displayText
    }

    /**
     * 追加已按下的修饰键文本
     */
    private fun appendModifierTokenIfPressed(
        hotkeyTokenList: MutableList<String>,
        modifierKeyCode: Int,
        modifierText: String,
        event: KeyEvent
    ) {
        if (pressedModifierKeyCodeSet.contains(modifierKeyCode) || event.keyCode == modifierKeyCode) {
            if (!hotkeyTokenList.contains(modifierText)) {
                hotkeyTokenList.add(modifierText)
            }
        }
    }

    /**
     * 解析主键文本
     */
    private fun resolvePrimaryKeyText(event: KeyEvent): String? {
        if (event.id == KeyEvent.KEY_TYPED && event.keyChar == ' ') {
            return "Space"
        }
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.VK_SPACE) {
            return "Space"
        }
        if (keyCode == KeyEvent.VK_UNDEFINED) {
            return null
        }
        return KeyEvent.getKeyText(keyCode)
    }

    /**
     * 更新修饰键按下状态
     */
    private fun updateModifierPressedState(event: KeyEvent, isPressed: Boolean) {
        val keyCode = event.keyCode
        if (!isModifierKey(keyCode)) {
            return
        }
        if (isPressed) {
            pressedModifierKeyCodeSet.add(keyCode)
        } else {
            pressedModifierKeyCodeSet.remove(keyCode)
        }
    }

    /**
     * 判断-是否修饰键
     * 用于区分纯修饰键录入和组合键录入，避免重复拼接
     *
     * @param keyCode 当前按键编码
     * @return 是否为修饰键
     */
    private fun isModifierKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.VK_SHIFT ||
            keyCode == KeyEvent.VK_CONTROL ||
            keyCode == KeyEvent.VK_ALT ||
            keyCode == KeyEvent.VK_META ||
            keyCode == KeyEvent.VK_WINDOWS
    }
}
