package com.inputspark.core.macos

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference

/**
 * macOS Carbon/TIS JNA 接口定义
 * 映射 macOS Carbon 框架中的 TIS (Text Input Source) 函数
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
interface Carbon : Library {
    
    /**
     * 获取-当前输入源
     * 获取当前选中的文本输入源
     *
     * @return 输入源引用
     */
    fun TISCopyCurrentKeyboardInputSource(): Pointer
    
    /**
     * 选择-输入源
     * 切换到指定的文本输入源
     *
     * @param inputSource 输入源引用
     * @return 状态码
     */
    fun TISSelectInputSource(inputSource: Pointer): Int
    
    /**
     * 获取-输入源属性
     * 获取指定输入源的属性值
     *
     * @param inputSource 输入源引用
     * @param propertyKey 属性键
     * @return 属性值引用
     */
    fun TISGetInputSourceProperty(inputSource: Pointer, propertyKey: Pointer): Pointer
    
    /**
     * 获取-ASCIICapable输入源
     * 获取当前启用的 ASCII 兼容输入源（通常是英文）
     *
     * @return 输入源引用
     */
    fun TISCopyCurrentASCIICapableKeyboardInputSource(): Pointer

    companion object {
        val INSTANCE: Carbon = Native.load("Carbon", Carbon::class.java)
        
        // 常量定义 (需要根据 macOS 头文件定义)
        // 这里简化处理，实际开发中需要准确的 CFStringRef
    }
}
