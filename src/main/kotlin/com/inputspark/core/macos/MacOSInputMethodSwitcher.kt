package com.inputspark.core.macos

import com.inputspark.model.InputMethodType
import com.inputspark.services.InputMethodSwitcher
import com.sun.jna.Pointer

/**
 * macOS 输入法切换器实现
 * 使用 Carbon TIS API 实现 macOS 平台的输入法切换
 *
 * @author 林龙祥
 * @since 2026-02-04
 */
class MacOSInputMethodSwitcher : InputMethodSwitcher {

    override fun switchToEnglish(): Boolean {
        val inputSource = Carbon.INSTANCE.TISCopyCurrentASCIICapableKeyboardInputSource()
        if (inputSource != Pointer.NULL) {
            val result = Carbon.INSTANCE.TISSelectInputSource(inputSource)
            return result == 0
        }
        return false
    }

    override fun switchToChinese(): Boolean {
        // macOS 下切换到中文比较复杂，通常需要遍历所有输入源找到中文输入源
        // 这里简化实现，实际需要更复杂的逻辑来查找非 ASCII 输入源
        // 暂时只实现切换回英文的功能作为 MVP
        return false 
    }

    override fun getCurrentInputMethod(): InputMethodType {
        val currentSource = Carbon.INSTANCE.TISCopyCurrentKeyboardInputSource()
        val asciiSource = Carbon.INSTANCE.TISCopyCurrentASCIICapableKeyboardInputSource()
        
        if (currentSource != Pointer.NULL && asciiSource != Pointer.NULL) {
            // 比较指针地址是否相同（简化判断）
            if (currentSource == asciiSource) {
                return InputMethodType.ENGLISH
            }
        }
        return InputMethodType.CHINESE // 假设非 ASCII 即为中文（或其它）
    }

    override fun isAvailable(): Boolean {
        return System.getProperty("os.name").lowercase().contains("mac")
    }
}
