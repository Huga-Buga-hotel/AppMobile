package com.example.hugabugahotel

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.hugabugahotel.util.aplicarBarrasDoSistema

class HugaBugaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Uma regra só para todas as telas: o conteúdo nunca fica embaixo da status bar,
        // da barra de navegação do sistema ou do teclado.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) =
                activity.aplicarBarrasDoSistema()
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
