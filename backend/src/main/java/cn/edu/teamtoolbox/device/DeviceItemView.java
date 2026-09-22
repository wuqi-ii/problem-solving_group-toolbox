package cn.edu.teamtoolbox.device;

import java.time.Instant;

public record DeviceItemView(String id, String type, String content, String fileName, Long sizeBytes,
                             String sourceDevice, Instant createdAt) {}
