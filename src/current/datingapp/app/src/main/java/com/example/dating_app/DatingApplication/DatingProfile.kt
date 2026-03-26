package com.example.dating_app

import org.json.JSONArray
import org.json.JSONObject

data class DatingProfile(
    val id:                 String,
    val name:               String,
    val age:                Int,
    val location:           String,
    val bio:                String,
    val image:                String,
    val species:            String,
    val furColor:           String,
    val tailType:           String,
    val gender:             String,
    val pronouns:           String,
    val activities:         List<String>,
    val personalityTraits:  List<String>,
    val compatibilityScore: Int
) {
    companion object {

        fun toJson(profile: DatingProfile): JSONObject = JSONObject().apply {
            put("id",                   profile.id)
            put("name",                 profile.name)
            put("age",                  profile.age)
            put("location",             profile.location)
            put("bio",                  profile.bio)
            put("image",                  profile.image)
            put("species",              profile.species)
            put("fur_color",            profile.furColor)
            put("tail_type",            profile.tailType)
            put("gender",               profile.gender)
            put("pronouns",             profile.pronouns)
            put("compatibility_score",  profile.compatibilityScore)
            put("activities",           JSONArray(profile.activities))
            put("personality_traits",   JSONArray(profile.personalityTraits))
        }

        fun fromJson(json: JSONObject): DatingProfile {
            fun jsonArrayToList(key: String): List<String> {
                val arr = json.optJSONArray(key) ?: return emptyList()
                return (0 until arr.length()).map { arr.getString(it) }
            }

            return DatingProfile(
                id                 = json.optString("id"),
                name               = json.optString("name"),
                age                = json.optInt("age"),
                location           = json.optString("location"),
                bio                = json.optString("bio"),
                image              = json.optString("image"),
                species            = json.optString("species"),
                furColor           = json.optString("fur_color"),
                tailType           = json.optString("tail_type"),
                gender             = json.optString("gender"),
                pronouns           = json.optString("pronouns"),
                activities         = jsonArrayToList("activities"),
                personalityTraits  = jsonArrayToList("personality_traits"),
                compatibilityScore = json.optInt("compatibility_score")
            )
        }
    }
}