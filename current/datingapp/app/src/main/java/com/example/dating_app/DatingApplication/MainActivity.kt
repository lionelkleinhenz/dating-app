package com.example.dating_app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

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
        val inputStream = resources.openRawResource(R.raw.profile_1)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        Log.d("PROFILE", "JSON loaded: $jsonString")
        val profile = DatingProfile.fromJson(JSONObject(jsonString))
        Log.d("PROFILE", "Profile name: ${profile.name}")
        return profile
    }
}
