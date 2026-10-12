package cn.edu.teamtoolbox.file;

import cn.edu.teamtoolbox.common.error.BusinessException;
import cn.edu.teamtoolbox.common.error.ErrorCode;
import cn.edu.teamtoolbox.group.GroupAuthorizationService;
import cn.edu.teamtoolbox.permission.PermissionType;
import cn.edu.teamtoolbox.task.TaskSubmissionRepository;
import cn.edu.teamtoolbox.task.TaskRepository;
import cn.edu.teamtoolbox.task.TaskStatus;
import cn.edu.teamtoolbox.device.DeviceItemEntity;
import cn.edu.teamtoolbox.device.DeviceItemRepository;
import cn.edu.teamtoolbox.user.UserEntity;
import cn.edu.teamtoolbox.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class GroupFileService {
    private final GroupFileRepository fileRepository;
    private final FileStorageService storageService;
    private final GroupAuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final TaskRepository taskRepository;
    private final DeviceItemRepository deviceItemRepository;
    private final Set<String> allowedExtensions;
    private final long maxGroupBytes;

    public GroupFileService(
            GroupFileRepository fileRepository,
            FileStorageService storageService,
            GroupAuthorizationService authorizationService,
            UserRepository userRepository,
            TaskSubmissionRepository taskSubmissionRepository,
            TaskRepository taskRepository,
            DeviceItemRepository deviceItemRepository,
            @Value("${app.storage.allowed-extensions}") String allowedExtensions,
            @Value("${app.storage.max-group-bytes:524288000}") long maxGroupBytes
    ) {
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.taskRepository = taskRepository;
        this.deviceItemRepository = deviceItemRepository;
        this.allowedExtensions = Arrays.stream(allowedExtensions.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.maxGroupBytes = maxGroupBytes;
    }

    @Transactional(readOnly = true)
    public TaskResultArchive downloadTaskResults(String groupId, String userId) {
        authorizationService.requireMember(groupId, userId);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int count = 0;
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            for (var task : taskRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId)) {
                if (task.getStatus() != TaskStatus.COMPLETED) continue;
                var submission = taskSubmissionRepository.findTopByTaskIdOrderByVersionNoDesc(task.getId()).orElse(null);
                if (submission == null || submission.getAttachmentFileId() == null) continue;
                var file = fileRepository.findById(submission.getAttachmentFileId()).orElse(null);
                if (file == null || !"ACTIVE".equals(file.getStatus())) continue;
                zip.putNextEntry(new ZipEntry(safeArchiveName(task.getTitle()) + "-" + task.getId().substring(0, 8)
                        + "-v" + submission.getVersionNo()
                        + extensionOf(file.getDisplayName())));
                try (var input = storageService.load(file.getStorageKey()).getInputStream()) {
                    input.transferTo(zip);
                }
                zip.closeEntry();
                count++;
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "任务成果打包失败");
        }
        return new TaskResultArchive(buffer.toByteArray(), count);
    }

    private String safeArchiveName(String value) {
        String safe = value == null ? "task-result" : value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return safe.isBlank() ? "task-result" : safe;
    }

    private String extensionOf(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot);
    }

    @Transactional
    public FileView upload(String groupId, String userId, MultipartFile multipartFile) {
        authorizationService.requireMember(groupId, userId);
        if (multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不能上传空文件");
        }
        String originalName = safeName(multipartFile.getOriginalFilename());
        validateExtension(originalName);
        requireCapacity(groupId, multipartFile.getSize());
        String storageKey = null;
        try {
            storageKey = storageService.store(groupId, multipartFile.getInputStream());
            GroupFileEntity entity = fileRepository.save(new GroupFileEntity(
                    groupId, originalName, storageKey, multipartFile.getContentType(),
                    multipartFile.getSize(), userId, nextVersion(groupId, originalName)));
            return toView(entity, userId, Map.of(userId, requireUser(userId)));
        } catch (IOException exception) {
            cleanup(storageKey);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        } catch (RuntimeException exception) {
            cleanup(storageKey);
            throw exception;
        }
    }

    @Transactional
    public FileView importDeviceFile(String groupId, String deviceItemId, String userId) {
        authorizationService.requireMember(groupId, userId);
        DeviceItemEntity item = deviceItemRepository.findById(deviceItemId)
                .filter(value -> value.getOwnerId().equals(userId)
                        && "ACTIVE".equals(value.getStatus()) && "FILE".equals(value.getType()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "设备文件不存在"));
        String name = safeName(item.getFileName());
        validateExtension(name);
        requireCapacity(groupId, item.getSizeBytes());
        Resource source = storageService.load(item.getStorageKey());
        if (!source.exists() || !source.isReadable()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "设备文件内容不存在");
        }
        String storageKey = null;
        try {
            storageKey = storageService.store(groupId, source.getInputStream());
            GroupFileEntity entity = fileRepository.save(new GroupFileEntity(
                    groupId, name, storageKey, item.getContentType(), item.getSizeBytes(), userId,
                    nextVersion(groupId, name)));
            return toView(entity, userId, Map.of(userId, requireUser(userId)));
        } catch (IOException exception) {
            cleanup(storageKey);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "设备文件导入失败");
        } catch (RuntimeException exception) {
            cleanup(storageKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<FileView> list(String groupId, String userId) {
        authorizationService.requireMember(groupId, userId);
        List<GroupFileEntity> files = fileRepository.findAllByGroupIdAndStatusOrderByCreatedAtDesc(groupId, "ACTIVE");
        Map<String, UserEntity> users = userRepository.findAllById(
                        files.stream().map(GroupFileEntity::getUploadedBy).distinct().toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        return files.stream().map(file -> toView(file, userId, users)).toList();
    }

    @Transactional(readOnly = true)
    public List<FileView> listTrash(String groupId, String userId) {
        authorizationService.requireMember(groupId, userId);
        List<GroupFileEntity> files = fileRepository.findAllByGroupIdAndStatusOrderByCreatedAtDesc(groupId, "DELETED");
        Map<String, UserEntity> users = userRepository.findAllById(
                        files.stream().map(GroupFileEntity::getUploadedBy).distinct().toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        return files.stream().map(file -> toView(file, userId, users)).toList();
    }

    @Transactional(readOnly = true)
    public FileQuotaView quota(String groupId, String userId) {
        authorizationService.requireMember(groupId, userId);
        long used = usedBytes(groupId);
        return new FileQuotaView(used, maxGroupBytes, Math.max(0, maxGroupBytes - used));
    }

    @Transactional(readOnly = true)
    public FileDownload download(String fileId, String userId) {
        GroupFileEntity file = requireActive(fileId);
        authorizationService.requireMember(file.getGroupId(), userId);
        Resource resource = storageService.load(file.getStorageKey());
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件内容不存在");
        }
        return new FileDownload(file, resource);
    }

    @Transactional(readOnly = true)
    public FileDownload preview(String fileId, String userId) {
        GroupFileEntity file = requireActive(fileId);
        authorizationService.requireMember(file.getGroupId(), userId);
        if (!isPreviewable(file)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "该文件类型暂不支持预览");
        }
        Resource resource = storageService.load(file.getStorageKey());
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件内容不存在");
        }
        return new FileDownload(file, resource);
    }

    @Transactional
    public FileView rename(String fileId, String userId, RenameFileRequest request) {
        GroupFileEntity file = requireActive(fileId);
        requireManage(file, userId);
        file.rename(safeName(request.name()));
        return toView(file, userId, Map.of(file.getUploadedBy(), requireUser(file.getUploadedBy())));
    }

    @Transactional
    public FileView updateCategory(String fileId, String userId, UpdateFileCategoryRequest request) {
        GroupFileEntity file = requireActive(fileId);
        requireManage(file, userId);
        file.setCategory(request.category());
        return toView(file, userId, Map.of(file.getUploadedBy(), requireUser(file.getUploadedBy())));
    }

    @Transactional
    public void delete(String fileId, String userId) {
        GroupFileEntity file = requireActive(fileId);
        requireManage(file, userId);
        if (taskSubmissionRepository.existsByAttachmentFileId(fileId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该文件已作为任务成果附件，不能删除");
        }
        file.softDelete();
    }

    @Transactional
    public FileView restore(String fileId, String userId) {
        GroupFileEntity file = requireDeleted(fileId);
        requireManage(file, userId);
        requireCapacity(file.getGroupId(), file.getSizeBytes());
        file.restore();
        return toView(file, userId, Map.of(file.getUploadedBy(), requireUser(file.getUploadedBy())));
    }

    @Transactional
    public void purge(String fileId, String userId) {
        GroupFileEntity file = requireDeleted(fileId);
        requireManage(file, userId);
        if (taskSubmissionRepository.existsByAttachmentFileId(fileId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "任务成果引用的文件不能永久删除");
        }
        try {
            storageService.delete(file.getStorageKey());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件物理删除失败");
        }
        fileRepository.delete(file);
    }

    public GroupFileEntity requireUsableInGroup(String fileId, String groupId) {
        GroupFileEntity file = requireActive(fileId);
        if (!file.getGroupId().equals(groupId)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "附件不属于当前小组");
        }
        return file;
    }

    private FileView toView(GroupFileEntity file, String requesterId, Map<String, UserEntity> users) {
        UserEntity uploader = users.get(file.getUploadedBy());
        return new FileView(
                file.getId(), file.getGroupId(), file.getDisplayName(), file.getContentType(),
                file.getSizeBytes(), file.getUploadedBy(), uploader == null ? "未知成员" : uploader.getNickname(),
                file.getCreatedAt(), file.getCategory(), file.getVersionNo(), file.getStatus(), file.getDeletedAt(),
                isPreviewable(file), canManage(file, requesterId));
    }

    private boolean canManage(GroupFileEntity file, String userId) {
        return file.getUploadedBy().equals(userId)
                || authorizationService.isLeaderOrHas(file.getGroupId(), userId, PermissionType.FILE_MANAGE);
    }

    private void requireManage(GroupFileEntity file, String userId) {
        authorizationService.requireMember(file.getGroupId(), userId);
        if (!canManage(file, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能管理自己上传的文件");
        }
    }

    private GroupFileEntity requireActive(String fileId) {
        return fileRepository.findById(fileId)
                .filter(file -> "ACTIVE".equals(file.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件不存在"));
    }

    private GroupFileEntity requireDeleted(String fileId) {
        return fileRepository.findById(fileId)
                .filter(file -> "DELETED".equals(file.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "回收站文件不存在"));
    }

    private int nextVersion(String groupId, String originalName) {
        return fileRepository.findTopByGroupIdAndOriginalNameOrderByVersionNoDesc(groupId, originalName)
                .map(file -> file.getVersionNo() + 1).orElse(1);
    }

    private long usedBytes(String groupId) {
        return fileRepository.findAllByGroupIdAndStatusOrderByCreatedAtDesc(groupId, "ACTIVE").stream()
                .mapToLong(GroupFileEntity::getSizeBytes).sum();
    }

    private void requireCapacity(String groupId, long newBytes) {
        if (usedBytes(groupId) + newBytes > maxGroupBytes) {
            throw new BusinessException(ErrorCode.CONFLICT, "小组资料库容量不足");
        }
    }

    private boolean isPreviewable(GroupFileEntity file) {
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String name = file.getDisplayName().toLowerCase(Locale.ROOT);
        return type.startsWith("image/") || type.startsWith("text/") || type.equals("application/pdf")
                || name.endsWith(".md") || name.endsWith(".csv") || name.endsWith(".txt");
    }

    private UserEntity requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
    }

    private String safeName(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "文件名不能为空");
        }
        String name = Path.of(value).getFileName().toString().trim();
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "文件名不正确");
        }
        return name;
    }

    private void validateExtension(String name) {
        int separator = name.lastIndexOf('.');
        String extension = separator < 0 ? "" : name.substring(separator + 1).toLowerCase(Locale.ROOT);
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "暂不支持该文件类型");
        }
    }

    private void cleanup(String storageKey) {
        if (storageKey == null) return;
        try { storageService.delete(storageKey); } catch (IOException ignored) { }
    }
}
