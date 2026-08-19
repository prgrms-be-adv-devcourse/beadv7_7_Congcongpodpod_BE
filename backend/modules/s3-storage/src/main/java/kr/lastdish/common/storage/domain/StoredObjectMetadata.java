package kr.lastdish.common.storage.domain;

/** 업로드된 객체를 발급 이력과 대조할 때 사용하는 Content-Type과 파일 크기입니다. */
public record StoredObjectMetadata(String contentType, long contentLength) {}
