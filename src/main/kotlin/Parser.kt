import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

const val XML_Reply = "text/xml"
const val Json_Reply = "application/json"

val logging = HttpLoggingInterceptor()
    .setLevel(HttpLoggingInterceptor.Level.BODY)

/**
 * Parser
 *
 * Define the RESTful interface
 */
interface Parser {
    @Multipart
    @POST("ParserService/api/ReadForm")
    fun readForm(
        @Part file: MultipartBody.Part
    ): Call<AllFields>
}

/**
 * Create the parser service instance
 */
fun createParser(service: String, returnContentType : String): Parser {
    val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("Connection", "keep-alive")
                .header("Accept", returnContentType)
            val request = builder.build()
            chain.proceed(request)
        }
        .addInterceptor(logging)
    .build()

    val converter = if (returnContentType == Json_Reply) {
        val mapper: ObjectMapper = jacksonObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        JacksonConverterFactory.create(mapper)
    } else {
        val mapper = XmlMapper(JacksonXmlModule().apply {
            setDefaultUseWrapper(false)
        }).registerKotlinModule()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        JacksonConverterFactory.create(mapper)
    }

    val retrofit = Retrofit.Builder()
        .baseUrl("https://$service")
        .addConverterFactory(converter)
        .client(httpClient)
        .build()
    return retrofit.create(Parser::class.java)
}

/**
 * Use the Parser Service to process the provided text file returning
 * a list of name value pairs representing the data.
 */
fun processForm(service: Parser, file: File): AllFields? {
    val mBody = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody())
    return service.readForm(mBody)
        .execute()
        .body()
}