package cn.edu.teamtoolbox.transfer;

import org.springframework.core.io.Resource;

public record TransferDownload(String fileName, long sizeBytes, Resource resource) {}
