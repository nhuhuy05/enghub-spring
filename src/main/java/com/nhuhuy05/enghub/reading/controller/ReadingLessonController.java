package com.nhuhuy05.enghub.reading.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.reading.dto.ReadingLessonListItemResponse;
import com.nhuhuy05.enghub.reading.dto.ReadingLessonResponse;
import com.nhuhuy05.enghub.reading.enums.ReadingLessonType;
import com.nhuhuy05.enghub.reading.service.ReadingLessonService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reading-lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReadingLessonController {
    ReadingLessonService readingLessonService;

    @GetMapping
    ApiResponse<List<ReadingLessonListItemResponse>> getPublishedLessons(
            @RequestParam(name = "reading_type", required = false) ReadingLessonType readingType
    ) {
        return ApiResponse.<List<ReadingLessonListItemResponse>>builder()
                .result(readingLessonService.getPublishedLessons(readingType))
                .build();
    }

    @GetMapping("/{lessonId}")
    ApiResponse<ReadingLessonResponse> getPublishedLesson(@PathVariable Long lessonId) {
        return ApiResponse.<ReadingLessonResponse>builder()
                .result(readingLessonService.getPublishedLesson(lessonId))
                .build();
    }
}
