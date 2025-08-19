package camp.cultr.darakserver.controller

import camp.cultr.darakserver.dto.CommonResponse
import camp.cultr.darakserver.dto.FileResponse
import camp.cultr.darakserver.dto.FileShareRequest
import camp.cultr.darakserver.service.FileService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/file")
class FileController(
    private val fileService: FileService,
) {
    @GetMapping(value = ["/p", "/p/{path}"])
    fun listPersonalDirectory(@PathVariable(value = "path", required = false) path: String = "") = fileService.listPersonalDirectory(path)

    @PutMapping(value = [ "/p/{path}"])
    fun mkdir(@PathVariable path: String) = fileService.mkdir(path)

    @PostMapping(value = ["/p", "/p/{path}"])
    fun uploadFile(@PathVariable(value = "path", required = false) path: String = "",
                   file: MultipartFile
    ) = fileService.saveFile(path, file)

    @DeleteMapping("/p/{path}")
    fun deleteFile(@PathVariable path: String) = fileService.deleteFile(path)

    @PostMapping("/s")
    fun shareFiles(@RequestBody request: FileShareRequest) = fileService.shareFile(request)

    @GetMapping("/s/{shareUri}")
    fun listSharedFiles(@PathVariable shareUri: UUID, @RequestParam(required = false, defaultValue = "") password: String = "") = fileService.listSharedFiles(shareUri, password)

    @GetMapping("/s/{shareUri}/download")
    fun downloadSharedFile(
        @PathVariable shareUri: UUID,
        @RequestParam(required = true) fileUuids: List<UUID>,
        @RequestParam(required = false, defaultValue = "") password: String = ""
    ) = fileService.downloadSharedFile(shareUri, fileUuids, password)

    @GetMapping("/d/{shareUri}")
    fun getDirectSharedFile(@PathVariable shareUri: UUID) = fileService.getDirectSharedFile(shareUri)

    @GetMapping("/f/{filePath}")
    fun downloadFile(@PathVariable filePath: String) = fileService.downloadFileForUser(filePath)
}