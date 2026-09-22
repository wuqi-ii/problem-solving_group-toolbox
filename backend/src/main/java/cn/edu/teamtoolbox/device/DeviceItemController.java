package cn.edu.teamtoolbox.device;

import cn.edu.teamtoolbox.common.api.ApiResponse;
import cn.edu.teamtoolbox.security.CurrentUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/device-items")
public class DeviceItemController {
    private final DeviceItemService service;

    public DeviceItemController(DeviceItemService service) { this.service = service; }

    @PostMapping("/text")
    public ApiResponse<DeviceItemView> createText(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                  @Valid @RequestBody CreateDeviceTextRequest request) {
        return ApiResponse.ok(service.createText(principal.id(), request));
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DeviceItemView> createFile(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                                  @RequestPart("file") MultipartFile file,
                                                  @RequestParam(required = false) String sourceDevice) {
        return ApiResponse.ok(service.createFile(principal.id(), file, sourceDevice));
    }

    @GetMapping
    public ApiResponse<List<DeviceItemView>> list(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.list(principal.id()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable String id,
                                             @AuthenticationPrincipal CurrentUserPrincipal principal) {
        DeviceFileDownload download = service.download(id, principal.id());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(download.resource());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id, @AuthenticationPrincipal CurrentUserPrincipal principal) {
        service.delete(id, principal.id());
        return ApiResponse.ok(null);
    }
}
