package cn.edu.teamtoolbox.file;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {
    String store(String groupId, InputStream inputStream) throws IOException;
    Resource load(String storageKey);
    void delete(String storageKey) throws IOException;
}
