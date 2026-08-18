package kr.lastdish.common.storage.s3;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/** 버킷, 리전, URL 만료 시간, 최대 업로드 크기와 선택적 S3 호환 endpoint 설정을 바인딩합니다. */
@ConfigurationProperties("storage.s3")
public record S3StorageProperties(
    @DefaultValue("false") boolean enabled,
    String bucket,
    @DefaultValue("ap-northeast-2") String region,
    @DefaultValue("5m") Duration presignedUrlExpiration,
    @DefaultValue("10MB") DataSize maxUploadSize,
    URI endpoint) {}
