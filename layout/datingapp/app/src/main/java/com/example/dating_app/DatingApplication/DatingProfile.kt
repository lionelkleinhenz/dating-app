package com.example.dating_app

import org.json.JSONObject

data class DatingProfile(
    val id: String,
    val name: String,
    val age: Int,
    val location: String,
    val bio: String,
    val img: String,
    val species: String,
    val furColor: String,
    val tailType: String,
    val gender: String,
    val pronouns: String,
    val activities: List<String>,
    val personalityTraits: List<String>,
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
                location = json.optString("location"),
                bio = json.optString("bio"),
                img = json.optString("img"),
                species = json.optString("species"),
                furColor = json.optString("fur_color"),
                tailType = json.optString("tail_type"),
                gender = json.optString("gender"),
                pronouns = json.optString("pronouns"),
                activities = jsonArrayToList("activities"),
                personalityTraits = jsonArrayToList("personality_traits"),
                compatibilityScore = json.optInt("compatibility_score")
            )
        }
    }

}
