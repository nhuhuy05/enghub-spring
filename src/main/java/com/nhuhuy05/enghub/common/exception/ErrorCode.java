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
    TEST_COLLECTION_NOT_EXISTED(1015, "Test collection not existed", HttpStatus.NOT_FOUND),
    TEST_COLLECTION_EXISTED(1016, "Test collection existed", HttpStatus.BAD_REQUEST),
    TEST_NUMBER_EXISTED(1017, "Test number existed in collection", HttpStatus.BAD_REQUEST),
    INVALID_TEST_COLLECTION_NUMBER(1018, "Collection and test number must be provided together", HttpStatus.BAD_REQUEST),
    MEDIA_ASSET_EXISTED(1019, "Media asset existed", HttpStatus.BAD_REQUEST),
    INVALID_MEDIA_TYPE(1020, "Media type is invalid", HttpStatus.BAD_REQUEST),
    CLOUDINARY_UPLOAD_FAILED(1021, "Cloudinary upload failed", HttpStatus.BAD_REQUEST),
    TEST_ALREADY_IMPORTED(1022, "Test already has imported questions", HttpStatus.BAD_REQUEST),
    QUESTION_GROUP_NOT_EXISTED(1023, "Question group not existed", HttpStatus.NOT_FOUND),
    MEDIA_ASSET_NOT_EXISTED(1024, "Media asset not existed", HttpStatus.NOT_FOUND),
    TEST_HAS_ATTEMPTS(1025, "Test already has attempts", HttpStatus.BAD_REQUEST),
    PASSAGE_NOT_EXISTED(1026, "Passage not existed", HttpStatus.NOT_FOUND),
    FILE_TOO_LARGE(1027, "Uploaded file is too large", HttpStatus.PAYLOAD_TOO_LARGE),
    MEDIA_ASSET_IN_USE(1028, "Media asset is being used", HttpStatus.BAD_REQUEST),
    GEMINI_DISABLED(1029, "Gemini integration is disabled", HttpStatus.BAD_REQUEST),
    GEMINI_API_KEY_MISSING(1030, "Gemini API key is missing", HttpStatus.BAD_REQUEST),
    GEMINI_UPLOAD_FAILED(1031, "Gemini file upload failed", HttpStatus.BAD_REQUEST),
    GEMINI_GENERATION_FAILED(1032, "Gemini generation failed", HttpStatus.BAD_REQUEST),
    GEMINI_INVALID_RESPONSE(1033, "Gemini response is invalid", HttpStatus.BAD_REQUEST),
    AI_AUDIO_NOT_EXISTED(1034, "Question group audio not existed", HttpStatus.BAD_REQUEST),
    AUDIO_DOWNLOAD_FAILED(1035, "Audio download failed", HttpStatus.BAD_REQUEST),
    VISUAL_DOWNLOAD_FAILED(1036, "Visual media download failed", HttpStatus.BAD_REQUEST),
    AI_MISSING_REQUIRED_CONTEXT(1037, "AI generation is missing required context", HttpStatus.BAD_REQUEST),
    VOCABULARY_NOT_EXISTED(1038, "Vocabulary not existed", HttpStatus.NOT_FOUND),
    VOCABULARY_EXISTED(1039, "Vocabulary existed", HttpStatus.BAD_REQUEST),
    VOCABULARY_TOPIC_NOT_EXISTED(1040, "Vocabulary topic not existed", HttpStatus.NOT_FOUND),
    VOCABULARY_TOPIC_EXISTED(1041, "Vocabulary topic existed", HttpStatus.BAD_REQUEST),
    VOCABULARY_PROGRESS_NOT_EXISTED(1042, "Vocabulary progress not existed", HttpStatus.NOT_FOUND),
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



