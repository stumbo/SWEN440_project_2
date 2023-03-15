import io.ktor.client.call.body
import io.ktor.client.call.body
import io.ktor.client.call.body
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.io.File

object OCRService {
    @Serializable
    data class OCROutput(val _text: String)

    @Serializable
    data class AsyncOCROutput(
        val _inputFile: String,
        val _inputFileLength: Int,
        val _outputFilePath: String
    )

    @Serializable
    data class ProcessedFile(
        val _fileData: String,
        val _fileReady: Boolean,
        val _fileName: String
    )

    /**
     * Perform Optical Character Recognition on an image file.
     * File is processed synchronously, the results are returned
     * once processing is completed.
     */
    fun processSync(file: File, ocrLib: String = "std"): File {
        var responseText: OCROutput
        runBlocking {
            val result = client.post() {
                url {
                    protocol = URLProtocol.HTTP
                    host = APP_SERVER
                    appendPathSegments("OCRService", "api", "ProcessFile")
                    parameters.append("ocrLib", ocrLib)
                }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("image",
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
            responseText = result.body()
        }
        val file = File.createTempFile("file", ".txt")
        file.writeText(responseText._text)
        return file
    }

    fun processAsync(file: File, ocrLib: String = "std"): String {
        var asynchResponse: AsyncOCROutput
        runBlocking {
            val result = client.post() {
                url {
                    protocol = URLProtocol.HTTP
                    host = APP_SERVER
                    appendPathSegments("OCRService", "api", "ProcessFileAsync")
                    parameters.append("ocrLib", ocrLib)
                }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("image",
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
            asynchResponse = result.body()
        }
        return asynchResponse._outputFilePath
    }

    fun getProcessedData(fileName: String): File {
        var processedFile: ProcessedFile
        runBlocking {
            do {
                delay(1000)
                val result = client.get() {
                    url {
                        protocol = URLProtocol.HTTP
                        host = APP_SERVER
                        appendPathSegments("OCRService", "api", "GetProcessedFile")
                        parameters.append("fileName", fileName)
                    }
                }
                processedFile = result.body()
            } while (!processedFile._fileReady)
        }

        val file = File.createTempFile("file", ".txt")
        file.writeText(processedFile._fileData)
        println("Path = ${file.absolutePath}")

        return file
    }
}