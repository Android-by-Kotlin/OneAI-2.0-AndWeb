package max.ohm.oneai.videogeneration.modelslab

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ModelsLabApiClient {
    private const val BASE_URL = "https://modelslab.com/"
    
    // API Key - You can store this securely or retrieve from a secure source
    const val API_KEY = "TYKbhmIv99CmDFruaavFUEDfHzWiMUWCyNMVluUqPlgKnx3a6oisBpwlhAc9"
    
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        // Custom logging to better track API calls
        android.util.Log.d("ModelsLabAPI", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(90, TimeUnit.SECONDS)  // Increased from 60
        .readTimeout(90, TimeUnit.SECONDS)     // Increased from 60
        .writeTimeout(90, TimeUnit.SECONDS)    // Increased from 60
        .retryOnConnectionFailure(true)        // Enable automatic retry
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ModelsLabApiService = retrofit.create(ModelsLabApiService::class.java)
}
