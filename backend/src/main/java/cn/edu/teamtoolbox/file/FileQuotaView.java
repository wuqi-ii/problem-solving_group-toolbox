package cn.edu.teamtoolbox.file;

public record FileQuotaView(long usedBytes, long limitBytes, long remainingBytes) {}
