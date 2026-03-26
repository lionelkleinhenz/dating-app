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
        val profiles = (1..500).map { index ->
            resources.getIdentifier("profile_$index", "raw", packageName)
        }
        val inputStream = resources.openRawResource(profiles[0]) // that of course becomes i but only with the python integration
        
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        Log.d("PROFILE", "JSON loaded: $jsonString")
        val profile = DatingProfile.fromJson(JSONObject(jsonString))
        Log.d("PROFILE", "Profile name: ${profile.name}")
        return profile
    }
}
