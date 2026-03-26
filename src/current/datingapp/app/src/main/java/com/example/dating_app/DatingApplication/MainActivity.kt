package com.example.dating_app

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        copyRawJsonsToInternalStorage()
        setContentView(R.layout.activity_main)

        val profile = loadProfileFromJson(this, (0..499).random())

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment.Companion.newInstance(profile))
                .commit()
        }
    }

    companion object {
        fun loadProfileFromJson(context: Context, num: Int): DatingProfile {
            val resId = context.resources.getIdentifier(
                "profile_${num + 1}",
                "raw",
                context.packageName
            )

            val inputStream = context.resources.openRawResource(resId)
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            return DatingProfile.fromJson(JSONObject(jsonString))
        }
    }

    private fun copyRawJsonsToInternalStorage() {
        val rawDir = File(filesDir, "raw").also { it.mkdirs() }

        val rawClass = R.raw::class.java
        for (field in rawClass.fields) {
            try {
                val resId = field.getInt(null)
                val outFile = File(rawDir, "${field.name}.json")
                if (!outFile.exists()) {
                    resources.openRawResource(resId).use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
