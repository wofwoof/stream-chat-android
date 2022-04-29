package io.getstream.chat.ui.sample.database

import android.database.Cursor
import android.database.CursorWrapper
import android.util.Log
import io.getstream.chat.ui.sample.database.TestCursor

private const val TAG = "TestCursor"

class TestCursor(c: Cursor?) : CursorWrapper(c) {
    private val mTrace: Throwable = Throwable("Explicit termination method 'close()' not called")
    private var mIsClosed = false

    override fun close() {
        super.close()
        mIsClosed = true
    }

    protected fun finalize() {
        if (!mIsClosed) {
            Log.e(TAG, "[finalize] cursor leaks", mTrace)
        }
    }
}