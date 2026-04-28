package com.inputspark.core.windows

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.inputspark.model.InputMethodType
import com.inputspark.services.ConfigurationManager
import com.inputspark.services.InputMethodSwitcher
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import java.awt.Robot
import java.lang.Thread.sleep

/**
 * Windows 输入法切换器实现
 * 使用 Imm32 + User32 API 实现 Windows 平台的输入法检测与切换
 * 支持微软拼音、搜狗输入法、百度输入法
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
class WindowsInputMethodSwitcher : InputMethodSwitcher {

    // 防抖：最小切换间隔（毫秒）
    private companion object {
        private const val MIN_PRESS_INTERVAL_MS = 100L
        private const val SWITCH_CONFIRM_DELAY_MS = 80L
        private const val FAILURE_NOTIFY_INTERVAL_MS = 3_000L
    }

    // 上次切换时间戳
    private var lastPressTime = 0L

    // 上次失败通知时间戳
    private var lastFailureNotifyTime = 0L

    // 缓存中文键盘布局句柄，避免重复加载
    private var chineseLayout: WinDef.HKL? = null

    override fun switchToEnglish(): Boolean {
        // 当前是否为中文模式：只有中文模式下才需要切换到英文
        val isChineseMode = getChineseModeStatus() ?: return false
        if (!isChineseMode) return false
        if (!canPressToggleHotkey()) return false

        // 当前切换热键：从配置中读取用户自定义热键
        val toggleHotkey = getToggleHotkey()
        return toggleInputMethod(toggleHotkey, InputMethodType.ENGLISH)
    }
    
    override fun switchToChinese(): Boolean {
        // 输入法打开准备：某些输入法即使返回 false，后续切换依然可能成功，因此这里只做尽力而为
        prepareInputMethodForChineseSwitch()

        // 当前是否为中文模式：已经是中文时无需重复切换
        val isChineseMode = getChineseModeStatus() ?: return false
        if (isChineseMode) return false
        if (!canPressToggleHotkey()) return false

        // 当前切换热键：从配置中读取用户自定义热键
        val toggleHotkey = getToggleHotkey()
        return toggleInputMethod(toggleHotkey, InputMethodType.CHINESE)
    }

    override fun getCurrentInputMethod(): InputMethodType {
        return if (isEnglishMode()) {
            InputMethodType.ENGLISH
        } else {
            InputMethodType.CHINESE
        }
    }

    /**
     * 检测当前是否处于英文输入法模式
     */
    private fun isEnglishMode(): Boolean {
        return getChineseModeStatus()?.not() ?: false
    }

    /**
     * 检查是否可以进行切换热键操作（防抖）
     */
    private fun canPressToggleHotkey(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastPressTime < MIN_PRESS_INTERVAL_MS) {
            return false
        }
        lastPressTime = now
        return true
    }

    /**
     * 获取-当前切换热键
     * 从用户配置中读取热键，并在配置为空时回退为默认 Shift
     */
    private fun getToggleHotkey(): InputMethodToggleHotkey {
        // 配置管理器：用于读取设置页中保存的切换按键
        val configurationManager = service<ConfigurationManager>()
        // 插件配置：包含用户当前保存的热键文本
        val pluginConfig = configurationManager.getConfig()
        return InputMethodToggleHotkeyParser.parse(pluginConfig.toggleHotkey)
    }

    /**
     * 获取-中文模式状态
     * 返回 true 表示当前为中文模式，false 表示英文模式，null 表示检测失败
     */
    private fun getChineseModeStatus(): Boolean? {
        return withImc { _, himc ->
            // 转换状态值：用于判断当前是否处于中文原生输入模式
            val conversion = IntByReference()
            // 句子状态值：IMM API 需要的输出参数
            val sentence = IntByReference()
            if (!Imm32.INSTANCE.ImmGetConversionStatus(himc, conversion, sentence)) {
                return@withImc null
            }
            (conversion.value and Imm32.IME_CMODE_NATIVE) != 0
        }
    }

    /**
     * 开启-输入法
     * 在切到中文前先确保输入法本身处于打开状态
     */
    private fun prepareInputMethodForChineseSwitch() {
        withImc { _, himc ->
            Imm32.INSTANCE.ImmSetOpenStatus(himc, true)
        }
    }

    /**
     * 执行-输入法切换
     * 按下用户配置的热键后，等待系统刷新并校验目标输入法是否真正生效
     */
    private fun toggleInputMethod(toggleHotkey: InputMethodToggleHotkey, targetInputMethodType: InputMethodType): Boolean {
        pressToggleHotkey(toggleHotkey)
        sleep(SWITCH_CONFIRM_DELAY_MS)

        // 切换后的输入法：用于确认本次模拟按键是否真正生效
        val currentInputMethodType = getCurrentInputMethod()
        if (currentInputMethodType == targetInputMethodType) {
            return true
        }

        notifyToggleFailure(toggleHotkey, targetInputMethodType)
        return false
    }

    /**
     * 模拟-切换热键
     * 支持单键与组合键，按顺序按下并按逆序释放，贴近真实键盘操作
     */
    private fun pressToggleHotkey(toggleHotkey: InputMethodToggleHotkey) {
        try {
            // 机器人对象：负责将配置热键转换为系统级键盘事件
            val robot = Robot()
            // 按键编码列表：组合键会依次按下每个键位
            val keyCodeList = toggleHotkey.keyCodes
            keyCodeList.forEach { keyCode ->
                robot.keyPress(keyCode)
            }
            keyCodeList.asReversed().forEach { keyCode ->
                robot.keyRelease(keyCode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 提示-切换失败
     * 当模拟按键后输入法状态未变化时，提示用户检查自定义热键配置
     */
    private fun notifyToggleFailure(toggleHotkey: InputMethodToggleHotkey, targetInputMethodType: InputMethodType) {
        // 当前时间戳：用于限制连续失败时的提示频率
        val now = System.currentTimeMillis()
        if (now - lastFailureNotifyTime < FAILURE_NOTIFY_INTERVAL_MS) {
            return
        }
        lastFailureNotifyTime = now

        // 目标输入法名称：用于拼接更直观的用户提示文案
        val targetInputMethodName = if (targetInputMethodType == InputMethodType.CHINESE) "中文" else "英文"
        Notification(
            "InputSpark",
            "InputSpark 输入法切换提示",
            "切换到${targetInputMethodName}失败，当前配置的中英文切换按键为「${toggleHotkey.displayText}」。如果你修改过系统输入法切换热键，请到 设置-Settings > 工具-Tools > InputSpark 中更新“中英文切换按键”配置。",
            NotificationType.WARNING
        ).notify(null)
    }

    private fun <T> withImc(block: (WinDef.HWND, WinNT.HANDLE) -> T): T? {
        val hwnd = ExtendedUser32.INSTANCE.GetForegroundWindow() ?: return null
        val himc = Imm32.INSTANCE.ImmGetContext(hwnd) ?: return null
        
        try {
            return block(hwnd, himc)
        } finally {
            Imm32.INSTANCE.ImmReleaseContext(hwnd, himc)
        }
    }

    override fun isAvailable(): Boolean {
        return System.getProperty("os.name").lowercase().contains("windows")
    }
}
