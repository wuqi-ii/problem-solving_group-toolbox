package cn.edu.teamtoolbox.transfer;

import cn.edu.teamtoolbox.common.error.BusinessException;
import cn.edu.teamtoolbox.common.error.ErrorCode;
import cn.edu.teamtoolbox.file.FileStorageService;
import cn.edu.teamtoolbox.device.DeviceItemEntity;
import cn.edu.teamtoolbox.device.DeviceItemRepository;
import cn.edu.teamtoolbox.device.DeviceItemView;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

@Service
public class TemporaryTransferService {
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final TemporaryTransferRepository repository;
    private final FileStorageService storageService;
    private final DeviceItemRepository deviceItemRepository;
    private final SecureRandom random = new SecureRandom();

    public TemporaryTransferService(
            TemporaryTransferRepository repository,
            FileStorageService storageService,
            DeviceItemRepository deviceItemRepository
    ) {
        this.repository = repository;
        this.storageService = storageService;
        this.deviceItemRepository = deviceItemRepository;
    }

    @Transactional
    public TransferView create(String ownerId, MultipartFile file, int validHours, int maxDownloads) {
        if (file.isEmpty()) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不能中转空文件");
        if (validHours < 1 || validHours > 168) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "有效期应为 1 至 168 小时");
        if (maxDownloads < 1 || maxDownloads > 100) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "下载次数应为 1 至 100 次");
        String name = safeName(file.getOriginalFilename());
        String code = newCode();
        String storageKey = null;
        try {
            storageKey = storageService.store("transfer-" + ownerId, file.getInputStream());
            TemporaryTransferEntity entity = repository.save(new TemporaryTransferEntity(
                    ownerId, name, storageKey, file.getContentType(), file.getSize(), hash(code),
                    Instant.now().plus(validHours, ChronoUnit.HOURS), maxDownloads));
            return toView(entity, code);
        } catch (IOException exception) {
            cleanup(storageKey);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "中转文件保存失败");
        } catch (RuntimeException exception) {
            cleanup(storageKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<TransferView> list(String ownerId) {
        return repository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(item -> toView(item, null)).toList();
    }

    @Transactional
    public void cancel(String id, String ownerId) {
        TemporaryTransferEntity entity = repository.findById(id)
                .filter(item -> item.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "中转记录不存在"));
        if (!"ACTIVE".equals(entity.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "该中转已不可取消");
        entity.cancel();
        cleanup(entity.getStorageKey());
    }

    @Transactional
    public PickupView inspect(String code) {
        TemporaryTransferEntity entity = repository.findByPickupCodeHash(hash(normalizeCode(code)))
                .orElseThrow(() -> unavailable());
        requireAvailable(entity);
        return new PickupView(entity.getOriginalName(), entity.getSizeBytes(), entity.getExpiresAt(),
                entity.getMaxDownloads() - entity.getDownloadCount());
    }

    @Transactional
    public TransferDownload download(String code) {
        TemporaryTransferEntity entity = repository.findByCodeForUpdate(hash(normalizeCode(code)))
                .orElseThrow(() -> unavailable());
        requireAvailable(entity);
        Resource resource = storageService.load(entity.getStorageKey());
        if (!resource.exists() || !resource.isReadable()) throw unavailable();
        entity.recordDownload();
        return new TransferDownload(entity.getOriginalName(), entity.getSizeBytes(), resource);
    }

    @Transactional
    public DeviceItemView saveToDevice(String code, String ownerId, String sourceDevice) {
        TemporaryTransferEntity transfer = repository.findByCodeForUpdate(hash(normalizeCode(code)))
                .orElseThrow(() -> unavailable());
        requireAvailable(transfer);
        Resource source = storageService.load(transfer.getStorageKey());
        if (!source.exists() || !source.isReadable()) throw unavailable();
        String copiedStorageKey = null;
        try {
            copiedStorageKey = storageService.store("device-" + ownerId, source.getInputStream());
            DeviceItemEntity item = deviceItemRepository.save(DeviceItemEntity.file(
                    ownerId, transfer.getOriginalName(), copiedStorageKey, transfer.getContentType(),
                    transfer.getSizeBytes(), normalizeDevice(sourceDevice)));
            transfer.recordDownload();
            return new DeviceItemView(item.getId(), item.getType(), item.getTextContent(), item.getFileName(),
                    item.getSizeBytes(), item.getSourceDevice(), item.getCreatedAt());
        } catch (IOException exception) {
            cleanup(copiedStorageKey);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存到设备空间失败");
        } catch (RuntimeException exception) {
            cleanup(copiedStorageKey);
            throw exception;
        }
    }

    @Scheduled(fixedDelayString = "${app.transfer.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanExpired() {
        repository.findAllByStatusAndExpiresAtBefore("ACTIVE", Instant.now()).forEach(entity -> {
            entity.expire();
            cleanup(entity.getStorageKey());
            entity.cleaned();
        });
        repository.findAllByStatus("EXHAUSTED").forEach(entity -> {
            cleanup(entity.getStorageKey());
            entity.cleaned();
        });
    }

    private void requireAvailable(TemporaryTransferEntity entity) {
        if (!"ACTIVE".equals(entity.getStatus())) throw unavailable();
        if (entity.getExpiresAt().isBefore(Instant.now())) {
            entity.expire();
            cleanup(entity.getStorageKey());
            throw unavailable();
        }
        if (entity.getDownloadCount() >= entity.getMaxDownloads()) throw unavailable();
    }

    private BusinessException unavailable() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "取件码无效或文件已失效");
    }

    private TransferView toView(TemporaryTransferEntity item, String code) {
        return new TransferView(item.getId(), item.getOriginalName(), item.getSizeBytes(), code, item.getStatus(),
                item.getExpiresAt(), item.getMaxDownloads(), item.getDownloadCount(), item.getCreatedAt());
    }

    private String newCode() {
        StringBuilder builder = new StringBuilder(10);
        do {
            builder.setLength(0);
            for (int i = 0; i < 10; i++) builder.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]);
        } while (repository.findByPickupCodeHash(hash(builder.toString())).isPresent());
        return builder.toString();
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) throw unavailable();
        return code.trim().toUpperCase();
    }

    private String normalizeDevice(String sourceDevice) {
        return sourceDevice == null || sourceDevice.isBlank() ? "临时取件" : sourceDevice.trim();
    }

    private String safeName(String value) {
        if (value == null || value.isBlank()) return "未命名文件";
        return Path.of(value).getFileName().toString();
    }

    private void cleanup(String storageKey) {
        if (storageKey == null) return;
        try { storageService.delete(storageKey); } catch (IOException ignored) { }
    }
}
