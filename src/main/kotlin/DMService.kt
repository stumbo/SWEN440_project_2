import io.ktor.client.call.body
import io.ktor.client.call.body
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.runBlocking
import java.io.File

object DMService {
    /**
     * Get the list of available files from the DM Service
     */
    fun getList(): List<DMFile> {
        var rVal: List<DMFile>
        runBlocking {
            val result = client.get() {
                url {
                    protocol = URLProtocol.HTTP
                    host = APP_SERVER
                    appendPathSegments("DMService", "api", "ListFiles")
                }
            }
            rVal = result.body()
        }
        return rVal
    }

    /**
     * Retrieve a file from the DM Service
     */
    fun getFile(fileName: String): File {
        var responseBody: ByteArray
        runBlocking {
            val result = client.get() {
                url {
                    protocol = URLProtocol.HTTP
                    host = APP_SERVER
                    appendPathSegments("DMService", "api", "DownloadFIle")
                    parameters.append("fileName", fileName)
                }
            }
            responseBody = result.body()
        }

        // We're assuming the suffix is always 3 characters
        val suffix = fileName.takeLast(3)
        val file = File.createTempFile("file", ".$suffix")
        file.writeBytes(responseBody)
        println("Path = ${file.absolutePath}")

        return file
    }


}