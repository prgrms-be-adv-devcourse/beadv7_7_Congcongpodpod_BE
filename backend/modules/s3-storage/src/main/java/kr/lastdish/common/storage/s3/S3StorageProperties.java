package kr.lastdish.common.storage.s3;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("storage.s3")
public record S3StorageProperties(
    @DefaultValue("false") boolean enabled,
    String bucket,
    @DefaultValue("ap-northeast-2") String region,
    @DefaultValue("5m") Duration presignedUrlExpiration,
    @DefaultValue("10MB") DataSize maxUploadSize,
    URI endpoint) {}
