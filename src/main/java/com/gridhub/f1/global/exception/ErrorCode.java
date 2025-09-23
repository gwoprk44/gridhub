package com.gridhub.f1.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid Input Value"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "Method Not Allowed"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "Server Error"),

    // User
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U001", "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U002", "이미 사용 중인 닉네임입니다."),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "U003", "닉네임은 2~10자의 영문, 숫자, 한글만 사용 가능합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U004", "해당 사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U005", "비밀번호가 일치하지 않습니다."),

    //Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "해당 게시글을 찾을 수 없습니다."),
    POST_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "P002", "게시글을 수정할 권한이 없습니다."),
    POST_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "P003", "게시글을 삭제할 권한이 없습니다.");
    // TODO: 앞으로 필요한 에러 코드를 여기에 추가

    private final HttpStatus status;
    private final String code;
    private final String message;
}