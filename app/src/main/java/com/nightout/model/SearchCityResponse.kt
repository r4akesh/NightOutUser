package com.nightout.model

data class SearchCityResponse(
    val `data`: MutableList<Data>,
    val message: String,
    val status_code: Int
) {

    data class Data(
        val created_at: String,
        val id: String,
        val slug: String,
        val status: String,
        val city_lattitude: String,
        val city_longitude: String,
        val country: String,
        val title: String,
        val updated_at: String
    )
}