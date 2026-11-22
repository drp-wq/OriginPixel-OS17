package android.app

import android.os.Bundle

class Notification {
    val extras: Bundle = Bundle()
    val visibility: Int = 0
    companion object {
        const val EXTRA_TITLE = "android.title"
        const val EXTRA_TEXT = "android.text"
        const val VISIBILITY_PRIVATE = 0
        const val VISIBILITY_SECRET = -1
    }
}
