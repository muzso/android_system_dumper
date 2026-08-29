package hu.muzso.android_system_dumper.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.muzso.android_system_dumper.network.upload.DefaultFilebinGateway
import hu.muzso.android_system_dumper.network.upload.DefaultGofileGateway
import hu.muzso.android_system_dumper.network.upload.FilebinApi
import hu.muzso.android_system_dumper.network.upload.GofileApi
import hu.muzso.android_system_dumper.network.upload.HttpClientProvider
import hu.muzso.android_system_dumper.network.upload.gateway.FilebinGateway
import hu.muzso.android_system_dumper.network.upload.gateway.GofileGateway
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(httpClientProvider: HttpClientProvider): OkHttpClient {
        return httpClientProvider.getClient()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofitBuilder(moshi: Moshi): Retrofit.Builder {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, retrofitBuilder: Retrofit.Builder): Retrofit {
        return retrofitBuilder
            .baseUrl("https://example.com/")
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideGofileApi(retrofit: Retrofit): GofileApi {
        return retrofit.newBuilder()
            .baseUrl("https://upload.gofile.io/")
            .build()
            .create(GofileApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFilebinApi(retrofit: Retrofit): FilebinApi {
        return retrofit.newBuilder()
            .baseUrl("https://filebin.net/")
            .build()
            .create(FilebinApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFilebinGateway(gateway: DefaultFilebinGateway): FilebinGateway = gateway

    @Provides
    @Singleton
    fun provideGofileGateway(gateway: DefaultGofileGateway): GofileGateway = gateway
}
