package com.nhuhuy05.enghub.test.controller;

import com.nhuhuy05.enghub.common.response.ApiResponse;
import com.nhuhuy05.enghub.test.dto.PublishedTestCollectionResponse;
import com.nhuhuy05.enghub.test.dto.PublishedTestResponse;
import com.nhuhuy05.enghub.test.service.TestCatalogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestCatalogController {
    TestCatalogService testCatalogService;

    @GetMapping("/test-collections")
    ApiResponse<List<PublishedTestCollectionResponse>> getPublishedCollections() {
        return ApiResponse.<List<PublishedTestCollectionResponse>>builder()
                .result(testCatalogService.getPublishedCollections())
                .build();
    }

    @GetMapping("/test-collections/{collectionId}/tests")
    ApiResponse<List<PublishedTestResponse>> getPublishedTestsByCollection(@PathVariable Long collectionId) {
        return ApiResponse.<List<PublishedTestResponse>>builder()
                .result(testCatalogService.getPublishedTests(collectionId))
                .build();
    }

    @GetMapping("/tests")
    ApiResponse<List<PublishedTestResponse>> getPublishedTests(
            @RequestParam(required = false) Long collectionId
    ) {
        return ApiResponse.<List<PublishedTestResponse>>builder()
                .result(testCatalogService.getPublishedTests(collectionId))
                .build();
    }

    @GetMapping("/tests/{testId}")
    ApiResponse<PublishedTestResponse> getPublishedTest(@PathVariable Long testId) {
        return ApiResponse.<PublishedTestResponse>builder()
                .result(testCatalogService.getPublishedTest(testId))
                .build();
    }
}
