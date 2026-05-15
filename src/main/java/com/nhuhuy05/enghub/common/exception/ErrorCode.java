package com.nhuhuy05.enghub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1003, "Email is invalid", HttpStatus.BAD_REQUEST),
    EMAIL_IS_REQUIRED(1008, "Email is required", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    TEST_NOT_EXISTED(1009, "Test not existed", HttpStatus.NOT_FOUND),
    QUESTION_NOT_EXISTED(1010, "Question not existed", HttpStatus.NOT_FOUND),
    ANSWER_NOT_EXISTED(1011, "Answer not existed", HttpStatus.NOT_FOUND),
    ATTEMPT_NOT_EXISTED(1012, "Attempt not existed", HttpStatus.NOT_FOUND),
    ATTEMPT_INVALID_STATE(1013, "Attempt is not in valid state", HttpStatus.BAD_REQUEST),
    ANSWER_NOT_BELONG_TO_QUESTION(1014, "Selected answer does not belong to question", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}



