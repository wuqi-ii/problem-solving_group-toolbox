package cn.edu.teamtoolbox.device;

import org.springframework.core.io.Resource;

public record DeviceFileDownload(String fileName, long sizeBytes, Resource resource) {}
