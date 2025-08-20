package camp.cultr.darakserver.component

import org.apache.tika.Tika
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class TikaWrapper {
    val tika = Tika()

    fun getMimeType(file: Path): String = tika.detect(file)
}