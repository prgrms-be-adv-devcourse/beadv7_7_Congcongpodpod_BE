package kr.lastdish.common.storage;

import java.net.URL;
import java.time.Instant;

/** 조회 대상 Object Key와 Presigned GET URL, 만료 시각을 전달하는 값 객체입니다. */
public record PresignedDownloadUrl(String objectKey, URL url, Instant expiresAt) {}
