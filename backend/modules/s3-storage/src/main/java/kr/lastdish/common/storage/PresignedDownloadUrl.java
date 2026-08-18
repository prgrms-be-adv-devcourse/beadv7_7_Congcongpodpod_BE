package kr.lastdish.common.storage;

import java.net.URL;
import java.time.Instant;

public record PresignedDownloadUrl(String objectKey, URL url, Instant expiresAt) {}
