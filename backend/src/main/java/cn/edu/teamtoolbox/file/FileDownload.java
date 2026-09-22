package cn.edu.teamtoolbox.file;

import org.springframework.core.io.Resource;

public record FileDownload(GroupFileEntity metadata, Resource resource) {
}
