package cn.edu.teamtoolbox.file;

import cn.edu.teamtoolbox.common.error.BusinessException;
import cn.edu.teamtoolbox.common.error.ErrorCode;
import cn.edu.teamtoolbox.group.GroupAuthorizationService;
import cn.edu.teamtoolbox.permission.PermissionType;
import cn.edu.teamtoolbox.user.UserEntity;
import cn.edu.teamtoolbox.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GroupFileService {
    private final GroupFileRepository fileRepository;
    private final FileStorageService storageService;
    private final GroupAuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final Set<String> allowedExtensions;

    public GroupFileService(
            GroupFileRepository fileRepository,
            FileStorageService storageService,
            GroupAuthorizationService authorizationService,
            UserRepository userRepository,
            @Value("${app.storage.allowed-extensions}") String allowedExtensions
    ) {
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.allowedExtensions = Arrays.stream(allowedExtensions.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public FileView upload(String groupId, String userId, MultipartFile multipartFile) {
        authorizationService.requireMember(groupId, userId);
        if (multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不能上传空文件");
        }
        String originalName = safeName(multipartFile.getOriginalFilename());
        validateExtension(originalName);
        String storageKey = null;
        try {
            storageKey = storageService.store(groupId, multipartFile.getInputStream());
            GroupFileEntity entity = fileRepository.save(new GroupFileEntity(
                    groupId, originalName, storageKey, multipartFile.getContentType(),
                    multipartFile.getSize(), userId));
            return toView(entity, userId, Map.of(userId, requireUser(userId)));
        } catch (IOException exception) {
            cleanup(storageKey);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
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
    public FileDownload download(String fileId, String userId) {
        GroupFileEntity file = requireActive(fileId);
        authorizationService.requireMember(file.getGroupId(), userId);
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
    public void delete(String fileId, String userId) {
        GroupFileEntity file = requireActive(fileId);
        requireManage(file, userId);
        file.softDelete();
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
                file.getCreatedAt(), canManage(file, requesterId));
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
