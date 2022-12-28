package dallasgutauckis.oolong.flipperplugin.sample

import android.app.Application
import android.util.Log
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.soloader.SoLoader
import dallasgutauckis.oolong.flipperplugin.OolongFlipperPlugin

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SoLoader.init(this, false)

        Log.v("DALLAS", "Starting application; DEBUG? ${BuildConfig.DEBUG}")

        if (BuildConfig.DEBUG && FlipperUtils.shouldEnableFlipper(this)) {
            Log.v("DALLAS", "Starting flipper")
            val client = AndroidFlipperClient.getInstance(this)
            client.addPlugin(InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()))
            client.addPlugin(OolongFlipperPlugin)
            client.start()
        }
    }
}
