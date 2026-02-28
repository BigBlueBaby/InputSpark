package com.inputspark.core.windows

import com.inputspark.model.InputMethodType
import com.inputspark.services.InputMethodSwitcher
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import java.awt.Robot
import java.awt.event.KeyEvent

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
    }

    // 上次切换时间戳
    private var lastPressTime = 0L

    // 缓存中文键盘布局句柄，避免重复加载
    private var chineseLayout: WinDef.HKL? = null

    override fun switchToEnglish(): Boolean {
        // 防抖检查
        if (!canPressShift()) return false

        return withImc { _, himc ->
            // 1. 获取当前状态
            val conversion = IntByReference()
            val sentence = IntByReference()
            if (!Imm32.INSTANCE.ImmGetConversionStatus(himc, conversion, sentence)) {
                return@withImc false
            }
            
            val isChineseMode = (conversion.value and Imm32.IME_CMODE_NATIVE) != 0
            
            // 2. 如果当前是中文模式，则模拟按键切换
            if (isChineseMode) {
                pressShift()
                return@withImc true
            } else {
                // 已经是英文模式，无需切换
                return@withImc false 
            }
        } ?: false
    }
    
    override fun switchToChinese(): Boolean {
        // 防抖检查
        if (!canPressShift()) return false

        return withImc { _, himc ->
            // 1. 确保输入法是打开的
            Imm32.INSTANCE.ImmSetOpenStatus(himc, true)
            
            // 2. 获取当前状态
            val conversion = IntByReference()
            val sentence = IntByReference()
            if (!Imm32.INSTANCE.ImmGetConversionStatus(himc, conversion, sentence)) {
                return@withImc false
            }
            
            val isChineseMode = (conversion.value and Imm32.IME_CMODE_NATIVE) != 0
            
            // 3. 如果当前是英文模式，则模拟按键切换
            if (!isChineseMode) {
                pressShift()
                return@withImc true
            } else {
                // 已经是中文模式，无需切换
                return@withImc false
            }
        } ?: false
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
        return withImc { _, himc ->
            val conversion = IntByReference()
            val sentence = IntByReference()
            if (!Imm32.INSTANCE.ImmGetConversionStatus(himc, conversion, sentence)) {
                return@withImc false
            }
            // IME_CMODE_NATIVE 为 0 表示英文模式
            (conversion.value and Imm32.IME_CMODE_NATIVE) == 0
        } ?: false
    }

    /**
     * 检查是否可以进行 Shift 按键切换（防抖）
     */
    private fun canPressShift(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastPressTime < MIN_PRESS_INTERVAL_MS) {
            return false
        }
        lastPressTime = now
        return true
    }

    /**
     * 模拟按下 Shift 键切换输入法
     */
    private fun pressShift() {
        try {
            val robot = Robot()
            // 按下 Shift
            robot.keyPress(KeyEvent.VK_SHIFT)
            // 释放 Shift
            robot.keyRelease(KeyEvent.VK_SHIFT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
