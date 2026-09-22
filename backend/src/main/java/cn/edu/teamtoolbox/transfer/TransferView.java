package cn.edu.teamtoolbox.transfer;

import java.time.Instant;

public record TransferView(String id, String fileName, long sizeBytes, String pickupCode, String status,
                           Instant expiresAt, int maxDownloads, int downloadCount, Instant createdAt) {}
