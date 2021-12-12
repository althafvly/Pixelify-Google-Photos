package balti.xposed.pixelifygooglephotos

import android.util.Log
import balti.xposed.pixelifygooglephotos.Constants.PACKAGE_NAME_GOOGLE_PHOTOS
import balti.xposed.pixelifygooglephotos.Constants.PREF_DEVICE_TO_SPOOF
import balti.xposed.pixelifygooglephotos.Constants.PREF_DEVICE_TO_SPOOF_GAME
import balti.xposed.pixelifygooglephotos.Constants.PREF_STRICTLY_CHECK_GOOGLE_PHOTOS
import balti.xposed.pixelifygooglephotos.DeviceProps.getGamePackages
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Codenames of pixels:
 * https://oneandroid.net/all-google-pixel-codenames-from-sailfish-to-redfin/
 *
 * Device properties stored in [DeviceProps].
 */
class DeviceSpoofer: IXposedHookLoadPackage {

    /**
     * Simple message to log messages in lsposed log as well as android log.
     */
    private fun log(message: String){
        XposedBridge.log("PixelifyGooglePhotos: $message")
        Log.d("PixelifyGooglePhotos", message)
    }

    /**
     * To read preference of user.
     */
    private val pref by lazy {
        XSharedPreferences(BuildConfig.APPLICATION_ID, Constants.SHARED_PREF_FILE_NAME)
    }

    /**
     * This is the final device to spoof.
     * By default use Pixel 5.
     */
    private val finalDeviceToSpoof by lazy {
        val deviceName = pref.getString(PREF_DEVICE_TO_SPOOF, DeviceProps.defaultDeviceName)
        log("Device spoof: $deviceName")
        DeviceProps.getDeviceProps(deviceName)
    }

    private val finalDeviceToSpoofGame by lazy {
        val deviceNameGame = pref.getString(PREF_DEVICE_TO_SPOOF_GAME, DeviceProps.defaultDeviceNameGame)
        log("Device spoof for games: $deviceNameGame")
        DeviceProps.getDevicePropsGames(deviceNameGame)
    }

    /**
     * Inspired by:
     * https://github.com/itsuki-t/FakeDeviceData/blob/master/src/jp/rmitkt/xposed/fakedevicedata/FakeDeviceData.java
     */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {

        val deviceName = finalDeviceToSpoofGame?.deviceName
        val gamePackages = getGamePackages()
        log("Loaded DeviceSpoofer for ${lpparam?.packageName}")

        /**
         * If user selects to never use this on any other app other than Google photos,
         * then check package name and return if necessary.
         */
        if (!pref.getBoolean(PREF_STRICTLY_CHECK_GOOGLE_PHOTOS, true) ||
            lpparam?.packageName == PACKAGE_NAME_GOOGLE_PHOTOS) {
            log("Device spoof: ${finalDeviceToSpoof?.deviceName}")
            finalDeviceToSpoof?.props?.run {
                if (keys.isEmpty()) return
                val classLoader = lpparam?.classLoader ?: return

                val classBuild = XposedHelpers.findClass("android.os.Build", classLoader)
                keys.forEach {
                    XposedHelpers.setStaticObjectField(classBuild, it, this[it])
                }
            }
        }

        /**
         * If user selected any non-pixel, check packages and apply props for those only.
         */

        for (element in gamePackages) {
            if (lpparam?.packageName == element && (deviceName == "OnePlus 7 Pro" || deviceName == "Sony Xperia 5 II")) {
                log("Device spoof for games: ${finalDeviceToSpoofGame?.deviceName}")

                finalDeviceToSpoofGame?.props?.run {
                    if (keys.isEmpty()) return
                    val classLoader = lpparam.classLoader ?: return

                    val classBuild = XposedHelpers.findClass("android.os.Build", classLoader)
                    keys.forEach {
                        XposedHelpers.setStaticObjectField(classBuild, it, this[it])
                    }
                }
            }
        }
    }
}