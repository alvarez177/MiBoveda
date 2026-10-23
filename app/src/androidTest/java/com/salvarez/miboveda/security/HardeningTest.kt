package com.salvarez.miboveda.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.salvarez.miboveda.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class HardeningTest {
    @Test
    fun backupIsDisabled() {
        val flags = ApplicationProvider.getApplicationContext<Context>().applicationInfo.flags
        assertEquals(0, flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test
    fun mainWindowBlocksScreenCapture() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val flags = activity.window.attributes.flags
                assertEquals(WindowManager.LayoutParams.FLAG_SECURE, flags and WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
