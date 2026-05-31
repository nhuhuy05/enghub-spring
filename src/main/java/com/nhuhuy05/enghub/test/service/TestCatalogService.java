package com.nhuhuy05.enghub.test.service;

import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.test.dto.PublishedTestCollectionResponse;
import com.nhuhuy05.enghub.test.dto.PublishedTestResponse;
import com.nhuhuy05.enghub.test.entity.Test;
import com.nhuhuy05.enghub.test.entity.TestCollection;
import com.nhuhuy05.enghub.test.repository.TestCollectionRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestCatalogService {
    TestCollectionRepository testCollectionRepository;
    TestRepository testRepository;

    @Transactional(readOnly = true)
    public List<PublishedTestCollectionResponse> getPublishedCollections() {
        return testCollectionRepository.findCollectionsWithPublishedTests().stream()
                .map(this::toCollectionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublishedTestResponse> getPublishedTests(Long collectionId) {
        if (collectionId == null) {
            return testRepository.findAllByPublishedTrueOrderByCreatedAtDesc().stream()
                    .map(this::toTestResponse)
                    .toList();
        }

        testCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_COLLECTION_NOT_EXISTED));

        return testRepository.findAllByCollectionIdAndPublishedTrueOrderByTestNumberAsc(collectionId).stream()
                .map(this::toTestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublishedTestResponse getPublishedTest(Long testId) {
        return testRepository.findByIdAndPublishedTrue(testId)
                .map(this::toTestResponse)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));
    }

    private PublishedTestCollectionResponse toCollectionResponse(TestCollection collection) {
        return PublishedTestCollectionResponse.builder()
                .id(collection.getId())
                .name(collection.getName())
                .description(collection.getDescription())
                .createdAt(collection.getCreatedAt())
                .build();
    }

    private PublishedTestResponse toTestResponse(Test test) {
        return PublishedTestResponse.builder()
                .id(test.getId())
                .collectionId(test.getCollection() == null ? null : test.getCollection().getId())
                .collectionName(test.getCollection() == null ? null : test.getCollection().getName())
                .testNumber(test.getTestNumber())
                .title(test.getTitle())
                .description(test.getDescription())
                .totalQuestions(test.getTotalQuestions())
                .durationMinutes(test.getDurationMinutes())
                .createdAt(test.getCreatedAt())
                .build();
    }
}
