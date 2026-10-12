package cn.edu.teamtoolbox.transfer;

import cn.edu.teamtoolbox.common.api.ApiResponse;
import cn.edu.teamtoolbox.security.CurrentUserPrincipal;
import cn.edu.teamtoolbox.device.DeviceItemView;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/api/v1")
public class TemporaryTransferController {
    private final TemporaryTransferService service;
    private final String publicBaseUrl;

    public TemporaryTransferController(
            TemporaryTransferService service,
            @Value("${app.public-base-url:http://localhost:5173}") String publicBaseUrl
    ) { this.service = service; this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", ""); }

    @PostMapping(value = "/transfers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TransferView> create(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                            @RequestPart("file") MultipartFile file,
                                            @RequestParam(defaultValue = "24") int validHours,
                                            @RequestParam(defaultValue = "5") int maxDownloads) {
        return ApiResponse.ok(service.create(principal.id(), file, validHours, maxDownloads));
    }

    @GetMapping("/transfers")
    public ApiResponse<List<TransferView>> list(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.list(principal.id()));
    }

    @DeleteMapping("/transfers/{id}")
    public ApiResponse<Void> cancel(@PathVariable String id, @AuthenticationPrincipal CurrentUserPrincipal principal) {
        service.cancel(id, principal.id());
        return ApiResponse.ok(null);
    }

    @GetMapping("/pickup/{code}")
    public ApiResponse<PickupView> inspect(@PathVariable String code) { return ApiResponse.ok(service.inspect(code)); }

    @GetMapping("/pickup/{code}/download")
    public ResponseEntity<Resource> download(@PathVariable String code) {
        TransferDownload download = service.download(code);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(download.resource());
    }

    @GetMapping(value = "/pickup/{code}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(@PathVariable String code) throws Exception {
        service.inspect(code);
        var matrix = new QRCodeWriter().encode(publicBaseUrl + "/pickup/" + code,
                BarcodeFormat.QR_CODE, 280, 280);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", output);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(output.toByteArray());
    }

    @PostMapping("/transfer-imports/{code}/device")
    public ApiResponse<DeviceItemView> saveToDevice(
            @PathVariable String code,
            @RequestParam(required = false) String sourceDevice,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.ok(service.saveToDevice(code, principal.id(), sourceDevice));
    }
}
