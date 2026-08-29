package hu.muzso.android_system_dumper.network.upload

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class TorCheckerResponse(
    @Json(name = "IsTor") val isTor: Boolean
)

interface TorCheckerApi {
    @GET
    suspend fun checkTor(@Url url: String): TorCheckerResponse
}
