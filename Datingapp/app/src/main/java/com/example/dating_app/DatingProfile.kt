package com.example.dating_app

import org.json.JSONObject

data class DatingProfile(
    val id: String,
    val name: String,
    val age: Int,
    val occupation: String,
    val location: String,
    val distanceKm: Int,
    val bio: String,
    val img: String,
    val gallery: List<String>,
    val heightCm: Int,
    val eyeColor: String,
    val hairColor: String,
    val bodyType: String,
    val ethnicity: String,
    val languages: List<String>,
    val education: String,
    val relationshipGoal: String,
    val zodiac: String,
    val diet: String,
    val drinking: String,
    val smoking: String,
    val hasPets: Boolean,
    val pets: List<String>,
    val interests: List<String>,
    val personalityTags: List<String>,
    val loveLangauges: List<String>,
    val verified: Boolean,
    val lastActive: String,
    val compatibilityScore: Int
) {
    companion object {
        fun fromJson(json: JSONObject): DatingProfile {
            fun jsonArrayToList(key: String): List<String> {
                val arr = json.optJSONArray(key) ?: return emptyList()
                return (0 until arr.length()).map { arr.getString(it) }
            }

            return DatingProfile(
                id = json.optString("id"),
                name = json.optString("name"),
                age = json.optInt("age"),
                occupation = json.optString("occupation"),
                location = json.optString("location"),
                distanceKm = json.optInt("distance_km"),
                bio = json.optString("bio"),
                img = json.optString("img"),
                gallery = jsonArrayToList("gallery"),
                heightCm = json.optInt("height_cm"),
                eyeColor = json.optString("eye_color"),
                hairColor = json.optString("hair_color"),
                bodyType = json.optString("body_type"),
                ethnicity = json.optString("ethnicity"),
                languages = jsonArrayToList("languages"),
                education = json.optString("education"),
                relationshipGoal = json.optString("relationship_goal"),
                zodiac = json.optString("zodiac"),
                diet = json.optString("diet"),
                drinking = json.optString("drinking"),
                smoking = json.optString("smoking"),
                hasPets = json.optBoolean("has_pets"),
                pets = jsonArrayToList("pets"),
                interests = jsonArrayToList("interests"),
                personalityTags = jsonArrayToList("personality_tags"),
                loveLangauges = jsonArrayToList("love_languages"),
                verified = json.optBoolean("verified"),
                lastActive = json.optString("last_active"),
                compatibilityScore = json.optInt("compatibility_score")
            )
        }
    }

    val heightFormatted: String
        get() {
            val totalInches = (heightCm / 2.54).toInt()
            val feet = totalInches / 12
            val inches = totalInches % 12
            return "$heightCm cm  ($feet′$inches″)"
        }
}
