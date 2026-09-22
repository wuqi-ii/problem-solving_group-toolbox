package cn.edu.teamtoolbox.transfer;

import java.time.Instant;

public record PickupView(String fileName, long sizeBytes, Instant expiresAt, int remainingDownloads) {}
