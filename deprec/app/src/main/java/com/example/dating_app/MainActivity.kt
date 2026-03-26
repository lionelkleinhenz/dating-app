package com.example.dating_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import com.example.dating_app.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val profile = loadProfileFromJson()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment.Companion.newInstance(profile))
                .commit()
        }
    }

    private fun loadProfileFromJson(): DatingProfile {
        val inputStream = resources.openRawResource(R.raw.profile)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return DatingProfile.Companion.fromJson(JSONObject(jsonString))
    }
}
