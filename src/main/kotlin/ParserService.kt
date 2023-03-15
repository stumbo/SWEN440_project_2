import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.runBlocking
import java.io.File

object ParserService {
    fun processForm(file: File): AllFields {
        var rVal: AllFields
        runBlocking {
            val result = client.post() {
                url {
                    protocol = URLProtocol.HTTP
                    host = APP_SERVER
                    appendPathSegments("ParserService", "api", "ReadForm")
                }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("text",
                                file.readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentType, "multipart/form-data")
                                    append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                                    append(HttpHeaders.Connection, "keep-alive")
                                }
                            )
                        },
                        boundary = "WebAppBoundary"
                    )
                )
            }
            rVal = result.body()
        }
        return rVal
    }


}