package com.salvarez.miboveda.presentation.common

class FakeSensitiveClipboard : SensitiveClipboard {
    val copies = mutableListOf<Pair<String, String>>()
    var clears = 0
        private set

    override fun copy(label: String, text: String) {
        copies += label to text
    }

    override fun clear() {
        clears++
    }
}
