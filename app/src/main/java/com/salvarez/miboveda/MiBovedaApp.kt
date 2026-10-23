package com.salvarez.miboveda

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.salvarez.miboveda.presentation.lock.AutoLockController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MiBovedaApp : Application() {
    @Inject
    lateinit var autoLockController: AutoLockController

    override fun onCreate() {
        super.onCreate()
        // The process lifecycle only stops once every activity is gone (with a short delay), not on rotation.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = autoLockController.onAppForegrounded()
            override fun onStop(owner: LifecycleOwner) = autoLockController.onAppBackgrounded()
        })
    }
}
