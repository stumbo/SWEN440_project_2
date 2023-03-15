import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.system.measureTimeMillis

val APP_SERVER = "seappserver3.rit.edu"

val client: HttpClient =
    HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 600000
            socketTimeoutMillis = 600000
        }
    }

@Serializable
data class DMFile(val fileName: String)

@Serializable
@JsonDeserialize
@JacksonXmlRootElement(localName = "Field")
data class Field(
    @field:JacksonXmlProperty(localName = "fieldName")
    var fieldName: String,

    @field:JacksonXmlProperty()
    var fieldValue: String
)

@Serializable
@JsonDeserialize
@JacksonXmlRootElement(localName = "AllFields")
data class AllFields(
    @JacksonXmlElementWrapper
    var allFields: List<Field>
)

fun main(args: Array<String>) {
    val fileList: MutableList<File> = arrayListOf()
    println("Start DM List Test")

    var result: List<DMFile>
    val elapsedTime = measureTimeMillis {
        result = DMService.getList()
    }
    println("Elapsed Time for DM List call = $elapsedTime milliseconds")

    println("  ---------------------------------- ")
    println("  Get each file")

    // run with a limited subset
    //result = result.subList(0, 5)

    result.forEach { file ->

        if ((file.fileName.endsWith(suffix = "png", ignoreCase = true) ||
                    file.fileName.endsWith(suffix = "jpg", ignoreCase = true))
        ) {
            val imageFile: File
            val elapsedTime = measureTimeMillis {
                imageFile = DMService.getFile(file.fileName)
            }
            println("Retrieved $file.filename in $elapsedTime milliseconds")
            fileList.add(imageFile)
        }
    }

    println(" ---------------------------------- ")
    println(" OCR each file using synchronous OCR")
    val ocrFileList: MutableList<File> = arrayListOf()
    fileList.forEach { file ->
        val ocrFile: File
        val elapsedTime = measureTimeMillis {
            ocrFile = OCRService.processSync(file, "pro")
        }
        ocrFileList.add(ocrFile)
        println("OCR Processed ${file.name} in $elapsedTime")
    }

    val parser = createParser(APP_SERVER, XML_Reply)
    //val parser = createParser(APP_SERVER, Json_Reply)
    println(" ------------------------------------ ")
    println(" Parser Service Method 2 extract name value pairs")
    var vals: AllFields?
    ocrFileList.forEach { file ->
        val elapsedTime = measureTimeMillis {
            vals = processForm(parser, file)
        }
        println("Parsed ${file.name} in $elapsedTime milliseconds")
        println(vals)
    }

    println(" ---------------------------------- ")
    println(" OCR each file using asynchronous OCR")
    // val asynchOCRFileList: MutableList<File> = arrayListOf()
    fileList.forEach { file ->
        val ocrFileName: String
        val elapsedTime = measureTimeMillis {
            ocrFileName = OCRService.processAsync(file, "pro")
            OCRService.getProcessedData(ocrFileName)
        }
        //ocrFileList.add(ocrFile)
        println("OCR Processed ${file.name} in $elapsedTime")
    }
}