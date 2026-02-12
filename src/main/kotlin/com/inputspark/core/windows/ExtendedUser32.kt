package com.inputspark.core.windows

import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.win32.StdCallLibrary

/**
 * 扩展 User32 接口，添加键盘布局相关 API
 */
interface ExtendedUser32 : StdCallLibrary {
    
    fun GetForegroundWindow(): WinDef.HWND
    
    /**
     * 加载键盘布局
     * @param pwszKLID 键盘布局标识符 (例如 "00000409" 表示英文)
     * @param Flags 标志位
     */
    fun LoadKeyboardLayoutA(pwszKLID: String, Flags: Int): WinDef.HKL
    
    /**
     * 激活键盘布局
     * @param hkl 键盘布局句柄
     * @param Flags 标志位
     */
    fun ActivateKeyboardLayout(hkl: WinDef.HKL, Flags: Int): WinDef.HKL
    
    /**
     * 获取当前键盘布局
     */
    fun GetKeyboardLayout(idThread: Int): WinDef.HKL

    companion object {
        val INSTANCE: ExtendedUser32 by lazy { 
            // 尝试直接加载，不依赖 classpath 查找
            Native.load("user32", ExtendedUser32::class.java)
        }
        
        const val KLF_ACTIVATE = 0x00000001
        const val KLF_SETFORPROCESS = 0x00000100
        
        // 键盘布局 ID
        const val LAYOUT_US_ENGLISH = "00000409" // 美式英语
        const val LAYOUT_CHINESE_SIMPLIFIED = "00000804" // 简体中文
    }
}