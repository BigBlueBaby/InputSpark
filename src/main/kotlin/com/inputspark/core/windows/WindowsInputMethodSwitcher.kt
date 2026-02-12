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
 * 使用 User32 KeyboardLayout API 实现 Windows 平台的输入法切换
 * 这种方式对 Windows 10/11 的 TSF 输入法（如微软拼音）更有效
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
class WindowsInputMethodSwitcher : InputMethodSwitcher {

    // 缓存中文键盘布局句柄，避免重复加载
    private var chineseLayout: WinDef.HKL? = null

    override fun switchToEnglish(): Boolean {
        // println("InputSpark: Switching to English (Inline Mode)...")
        
        return withImc { _, himc ->
            // 1. 获取当前状态
            val conversion = IntByReference()
            val sentence = IntByReference()
            if (!Imm32.INSTANCE.ImmGetConversionStatus(himc, conversion, sentence)) {
                // println("InputSpark: Failed to get status")
                return@withImc false
            }
            
            val isChineseMode = (conversion.value and Imm32.IME_CMODE_NATIVE) != 0
            // println("InputSpark: Current mode is Chinese? $isChineseMode (conversion=${conversion.value})")
            
            // 2. 如果当前是中文模式，则模拟按键切换
            if (isChineseMode) {
                // println("InputSpark: Switching to English via Robot (Shift)...")
                try {
                    val robot = Robot()
                    robot.keyPress(KeyEvent.VK_SHIFT)
                    robot.keyRelease(KeyEvent.VK_SHIFT)
                    return@withImc true // 发生了切换
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // 已经是英文模式，无需切换
                return@withImc false 
            }
            false
        } ?: false
    }
    
    override fun switchToChinese(): Boolean {
        // println("InputSpark: Switching to Chinese (Inline Mode)...")
        
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
                // println("InputSpark: Switching to Chinese via Robot (Shift)...")
                try {
                    val robot = Robot()
                    robot.keyPress(KeyEvent.VK_SHIFT)
                    robot.keyRelease(KeyEvent.VK_SHIFT)
                    return@withImc true // 发生了切换
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // 已经是中文模式，无需切换
                return@withImc false
            }
            false
        } ?: false
    }

    override fun getCurrentInputMethod(): InputMethodType {
        // 获取当前线程的键盘布局
        val hkl = ExtendedUser32.INSTANCE.GetKeyboardLayout(0)
        val pointerValue = hkl.pointer.getLong(0)
        
        // 检查低 16 位语言 ID
        // 0x0409 = English, 0x0804 = Chinese Simplified
        val langId = pointerValue and 0xFFFF
        
        return if (langId == 0x0804L) {
             InputMethodType.CHINESE
        } else {
             InputMethodType.ENGLISH
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
