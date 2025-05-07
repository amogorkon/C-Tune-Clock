package com.kiefner.c_tune_clock.bridge

import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python

/**
 * A singleton bridging Kotlin and Python code using Chaquopy.
 * Modified so that:
 * • getCTUTime returns a Triple<Int, Int, Int> (hour, minute, second)
 * • dawn_dusk returns a Pair of Triple<Int, Int, Int> (dawn, dusk)
 */
object PythonBridge {
    private val python = Python.getInstance()
    private val ctuModule = python.getModule("ctu")

    /**
     * Retrieves the current CTU time as a triple of ints: (hour, minute, second).
     */
    fun getCTUTime(longitude: Float): Triple<Int, Int, Int>? {
        return try {
            val timePy = ctuModule.callAttr("now", longitude)
            timePy?.let {
                val hour = it.get("hour")?.toJava(Int::class.java) ?: 0
                val minute = it.get("minute")?.toJava(Int::class.java) ?: 0
                val second = it.get("second")?.toJava(Int::class.java) ?: 0
                Triple(hour, minute, second)
            }
        } catch (e: Exception) {
            Log.e("PythonBridge", "Error getting CTU time", e)
            null
        }
    }

    /**
     * Retrieves astronomical data for dawn and dusk.
     * Returns a Pair:
     *   First: Triple<Int, Int, Int> for dawn (hour, minute, second)
     *   Second: Triple<Int, Int, Int> for dusk (hour, minute, second)
     */
    fun dawn_dusk(latitude: Float, longitude: Float): Pair<Triple<Int, Int, Int>, Triple<Int, Int, Int>>? {
        return try {
            val now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
            // Build a Python datetime.datetime object using Chaquopy's Python API.
            val pyDatetime = python.getModule("datetime")
            val datetimeClass = pyDatetime.get("datetime")!!
            val timezoneObj = pyDatetime.get("timezone")!!
            val utcObj = timezoneObj.get("utc")!!
            val pyDate = datetimeClass.call(
                now.year,
                now.monthValue,
                now.dayOfMonth,
                now.hour,
                now.minute,
                now.second,
                now.nano / 1_000_000,
                utcObj
            )
            // Call the Python function 'dawn_dusk_ctu' with the datetime object.
            val astroPy = ctuModule.callAttr("dawn_dusk_ctu", latitude, longitude, pyDate)
            val astroList = astroPy?.asList()
            if (astroList != null && astroList.size >= 2) {
                val dawnPy = astroList[0] as? PyObject
                val duskPy = astroList[1] as? PyObject
                if (dawnPy != null && duskPy != null) {
                    val dawnTriple = Triple(
                        (dawnPy.get("hour")?.toJava(Int::class.java)) ?: 0,
                        (dawnPy.get("minute")?.toJava(Int::class.java)) ?: 0,
                        (dawnPy.get("second")?.toJava(Int::class.java)) ?: 0
                    )
                    val duskTriple = Triple(
                        (duskPy.get("hour")?.toJava(Int::class.java)) ?: 0,
                        (duskPy.get("minute")?.toJava(Int::class.java)) ?: 0,
                        (duskPy.get("second")?.toJava(Int::class.java)) ?: 0
                    )
                    Pair(dawnTriple, duskTriple)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("PythonBridge", "Error getting astro data", e)
            null
        }
    }
}
