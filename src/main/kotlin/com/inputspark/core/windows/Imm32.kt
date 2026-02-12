package com.inputspark.core.windows

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.ptr.IntByReference

/**
 * Imm32 JNA 接口定义
 * 映射 Windows Imm32.dll 中的函数
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
interface Imm32 : StdCallLibrary {
    
    /**
     * 获取-默认输入法句柄
     * 获取当前线程的默认输入法上下文句柄
     *
     * @param hWnd 窗口句柄
     * @return 输入法上下文句柄
     */
    fun ImmGetContext(hWnd: WinDef.HWND): WinNT.HANDLE?
    
    /**
     * 释放-输入法句柄
     * 释放之前获取的输入法上下文句柄
     *
     * @param hWnd 窗口句柄
     * @param hImc 输入法上下文句柄
     * @return 是否释放成功
     */
    fun ImmReleaseContext(hWnd: WinDef.HWND, hImc: WinNT.HANDLE?): Boolean
    
    /**
     * 获取-转换状态
     * 获取当前输入法的转换模式和句子模式
     *
     * @param hImc 输入法上下文句柄
     * @param lpfdwConversion 接收转换模式的指针
     * @param lpfdwSentence 接收句子模式的指针
     * @return 是否获取成功
     */
    fun ImmGetConversionStatus(hImc: WinNT.HANDLE?, lpfdwConversion: IntByReference, lpfdwSentence: IntByReference): Boolean
    
    /**
     * 设置-转换状态
     * 设置当前输入法的转换模式和句子模式
     *
     * @param hImc 输入法上下文句柄
     * @param fdwConversion 转换模式
     * @param fdwSentence 句子模式
     * @return 是否设置成功
     */
    fun ImmSetConversionStatus(hImc: WinNT.HANDLE?, fdwConversion: Int, fdwSentence: Int): Boolean
    
    /**
     * 设置-打开状态
     * 打开或关闭输入法
     *
     * @param hImc 输入法上下文句柄
     * @param fOpen 是否打开
     * @return 是否设置成功
     */
    fun ImmSetOpenStatus(hImc: WinNT.HANDLE?, fOpen: Boolean): Boolean
    
    /**
     * 获取-打开状态
     * 获取当前输入法是否处于打开状态
     *
     * @param hImc 输入法上下文句柄
     * @return 是否打开
     */
    fun ImmGetOpenStatus(hImc: WinNT.HANDLE?): Boolean

    companion object {
        val INSTANCE: Imm32 by lazy {
            Native.load("imm32", Imm32::class.java)
        }
        
        // 常量定义
        const val IME_CMODE_ALPHANUMERIC = 0x0000
        const val IME_CMODE_NATIVE = 0x0001
        const val IME_CMODE_CHINESE = IME_CMODE_NATIVE
        const val IME_CMODE_FULLSHAPE = 0x0008
        const val IME_CMODE_SYMBOL = 0x0400
    }
}
