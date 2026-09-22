package cn.edu.teamtoolbox.file;

import cn.edu.teamtoolbox.common.api.ApiResponse;
import cn.edu.teamtoolbox.security.CurrentUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class GroupFileController {
    private final GroupFileService fileService;

    public GroupFileController(GroupFileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "/groups/{groupId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileView> upload(
            @PathVariable String groupId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(fileService.upload(groupId, principal.id(), file));
    }

    @GetMapping("/groups/{groupId}/files")
    public ApiResponse<List<FileView>> list(
            @PathVariable String groupId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.ok(fileService.list(groupId, principal.id()));
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable String fileId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        FileDownload download = fileService.download(fileId, principal.id());
        String disposition = ContentDisposition.attachment()
                .filename(download.metadata().getDisplayName(), StandardCharsets.UTF_8).build().toString();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(download.metadata().getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(download.resource());
    }

    @PatchMapping("/files/{fileId}")
    public ApiResponse<FileView> rename(
            @PathVariable String fileId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody RenameFileRequest request
    ) {
        return ApiResponse.ok(fileService.rename(fileId, principal.id(), request));
    }

    @DeleteMapping("/files/{fileId}")
    public ApiResponse<Void> delete(
            @PathVariable String fileId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        fileService.delete(fileId, principal.id());
        return ApiResponse.ok(null);
    }
}
