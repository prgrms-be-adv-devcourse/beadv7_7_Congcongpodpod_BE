package kr.lastdish.common.api.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCodeSpec {

  HttpStatus getStatus();

  String getCode();

  String getMessage();
}
