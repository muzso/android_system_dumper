package hu.muzso.android_system_dumper.upload.network

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface FilebinApi {
    @POST("{bin}/{filename}")
    suspend fun upload(
        @Path("bin") bin: String,
        @Path("filename") filename: String,
        @Header("filename") filenameHeader: String,
        @Body body: RequestBody
    ): Response<Unit>
}
