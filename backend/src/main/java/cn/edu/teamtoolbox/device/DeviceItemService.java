package cn.edu.teamtoolbox.device;

import cn.edu.teamtoolbox.common.error.BusinessException;
import cn.edu.teamtoolbox.common.error.ErrorCode;
import cn.edu.teamtoolbox.file.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class DeviceItemService {
    private final DeviceItemRepository repository;
    private final FileStorageService storageService;

    public DeviceItemService(DeviceItemRepository repository, FileStorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    @Transactional
    public DeviceItemView createText(String ownerId, CreateDeviceTextRequest request) {
        DeviceItemEntity entity = repository.save(DeviceItemEntity.text(ownerId, request.type(),
                request.content().trim(), normalize(request.sourceDevice())));
        return toView(entity);
    }

    @Transactional
    public DeviceItemView createFile(String ownerId, MultipartFile file, String sourceDevice) {
        if (file.isEmpty()) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不能保存空文件");
        String fileName = safeName(file.getOriginalFilename());
        String storageKey = null;
        try {
            storageKey = storageService.store("device-" + ownerId, file.getInputStream());
            return toView(repository.save(DeviceItemEntity.file(ownerId, fileName, storageKey,
                    file.getContentType(), file.getSize(), normalize(sourceDevice))));
        } catch (IOException exception) {
            cleanup(storageKey);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "设备文件保存失败");
        } catch (RuntimeException exception) {
            cleanup(storageKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<DeviceItemView> list(String ownerId) {
        return repository.findAllByOwnerIdAndStatusOrderByCreatedAtDesc(ownerId, "ACTIVE").stream()
                .map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public DeviceFileDownload download(String id, String ownerId) {
        DeviceItemEntity entity = requireOwned(id, ownerId);
        if (!"FILE".equals(entity.getType())) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "该内容不是文件");
        Resource resource = storageService.load(entity.getStorageKey());
        if (!resource.exists() || !resource.isReadable()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件内容不存在");
        return new DeviceFileDownload(entity.getFileName(), entity.getSizeBytes(), resource);
    }

    @Transactional
    public void delete(String id, String ownerId) {
        DeviceItemEntity entity = requireOwned(id, ownerId);
        if (entity.getStorageKey() != null) cleanup(entity.getStorageKey());
        entity.delete();
    }

    private DeviceItemEntity requireOwned(String id, String ownerId) {
        return repository.findById(id)
                .filter(item -> item.getOwnerId().equals(ownerId) && "ACTIVE".equals(item.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "设备内容不存在"));
    }

    private DeviceItemView toView(DeviceItemEntity item) {
        return new DeviceItemView(item.getId(), item.getType(), item.getTextContent(), item.getFileName(),
                item.getSizeBytes(), item.getSourceDevice(), item.getCreatedAt());
    }

    private String safeName(String value) {
        return value == null || value.isBlank() ? "未命名文件" : Path.of(value).getFileName().toString();
    }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private void cleanup(String storageKey) {
        if (storageKey == null) return;
        try { storageService.delete(storageKey); } catch (IOException ignored) { }
    }
}
