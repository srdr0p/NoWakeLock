package com.js.nowakelock.xposedhook

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.js.nowakelock.xposedhook.model.Model
import com.js.nowakelock.xposedhook.model.XPM
import com.js.nowakelock.xposedhook.model.mModel
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.*


class AlarmHook {
    companion object {

        // model
        private val model: Model = mModel(XPM.alarm)

        // update Setting interval
        private var updateSetting: Long = 60000 //Save every minutes
        private var updateSettingTime: Long = 0

        fun hookAlarm(lpparam: XC_LoadPackage.LoadPackageParam) {

            when (Build.VERSION.SDK_INT) {
                //Try for alarm hooks for API levels >= 29 (Q or higher)
                Build.VERSION_CODES.Q -> alarmHook29(
                    lpparam
                )
                //Try for alarm hooks for API levels < 29 > 24.(N ~ P)
                in Build.VERSION_CODES.N..Build.VERSION_CODES.P -> alarmHook24to28(
                    lpparam
                )
            }
        }

        private fun alarmHook29(lpparam: XC_LoadPackage.LoadPackageParam) {
            XposedHelpers.findAndHookMethod("com.android.server.AlarmManagerService",
                lpparam.classLoader,
                "triggerAlarmsLocked",
                ArrayList::class.java,
                Long::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val triggerList = param.args[0] as ArrayList<Any>
                        val nowELAPSED = param.args[1] as Long
                        val context = XposedHelpers.getObjectField(
                            param.thisObject,
                            "mContext"
                        ) as Context
//                            log("Alarm Q ${triggerList.size} $nowELAPSED")
                        hookAlarmsLocked(
                            param,
                            triggerList,
                            context
                        )
                    }
                })
        }

        private fun alarmHook24to28(lpparam: XC_LoadPackage.LoadPackageParam) {
            XposedHelpers.findAndHookMethod("com.android.server.AlarmManagerService",
                lpparam.classLoader,
                "triggerAlarmsLocked",
                ArrayList::class.java,
                Long::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val triggerList = param.args[0] as ArrayList<Any>
                        val nowELAPSED = param.args[1] as Long
                        val nowRTC = param.args[2] as Long
//                            log("Alarm N ${triggerList.size} $nowELAPSED $nowRTC")
                        val context = XposedHelpers.getObjectField(
                            param.thisObject,
                            "mContext"
                        ) as Context
                        hookAlarmsLocked(
                            param,
                            triggerList,
                            context
                        )
                    }
                })
        }

        // handle alarm
        private fun hookAlarmsLocked(
            param: XC_MethodHook.MethodHookParam,
            triggerList: ArrayList<Any>,
            context: Context
        ) {
//            val now = SystemClock.elapsedRealtime() //real time
            var alarmName = ""
            var packageName = ""

            XpUtil.log(" alarmlist: ${triggerList.size};$triggerList")
            for (i in 0 until triggerList.size) {

                try {
                    val tmp = triggerList[i]
                    val tmp2 = tmp.javaClass.getDeclaredField("statsTag").get(tmp) as String
                    alarmName = tmp2.replace(Regex("\\*.*\\*:"), "")
                    packageName = tmp.javaClass.getDeclaredField("packageName").get(tmp) as String
                } catch (e: Exception) {
                    XpUtil.log(" alarm: hookAlarmsLocked err:$e")
                    continue
                }

                // block or not
                val flag = flag(alarmName, packageName)

                if (flag) {
                    //allow alarm
                    XpUtil.log("$packageName alarm: $alarmName allow")
                    model.upCount(alarmName, packageName)
                } else {
                    //block alarm
                    XpUtil.log("$packageName alarm: $alarmName block")
                    triggerList.removeAt(i)
                    model.upBlockCount(alarmName, packageName)
                }
            }
            model.handleTimer(context)
        }

        // get weather alarm should block or not
        private fun flag(aN: String, packageName: String): Boolean {
            return model.flag(aN) && model.re(aN, packageName)
        }


        private fun getIntent(pi: PendingIntent): Intent? {
            try {
                return XposedHelpers.callMethod(pi, "getIntent") as Intent
            } catch (e: NoSuchMethodError) {
                try {
                    XposedHelpers.getObjectField(pi, "mTarget")?.let { mTarget ->
                        XposedHelpers.getObjectField(mTarget, "key")?.let { pIRecordKey ->
                            val requestIntent =
                                XposedHelpers.getObjectField(pIRecordKey, "requestIntent")
                            return requestIntent as Intent
                        }
                    }
                } catch (e: Exception) {
                    XpUtil.log("getIntent failed2 err:$e")
                }
                XpUtil.log("getIntent failed1 err:$e")
            }
            return null
        }

        //alarm name
        private fun getName(intent: Intent): String? {
            if (intent.action != null) {
                return intent.action
            } else if (intent.component != null) {
                return intent.component?.flattenToShortString()
            }
            return null
        }
    }
}