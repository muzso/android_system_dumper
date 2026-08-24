package hu.muzso.android_system_dumper.upload.network

import com.squareup.moshi.JsonClass
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GofileApi {
    @POST("uploadfile")
    suspend fun uploadFile(
        @Body body: RequestBody,
        @Header("Authorization") token: String? = null
    ): Response<GofileResponse>
}

@JsonClass(generateAdapter = true)
data class GofileResponse(
    val status: String,
    val data: GofileData? = null
)

@JsonClass(generateAdapter = true)
data class GofileData(
    val downloadPage: String? = null,
    val guestToken: String? = null,
    val parentFolder: String? = null,
    val fileName: String? = null
)
