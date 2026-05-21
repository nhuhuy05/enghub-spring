package com.nhuhuy05.enghub.test.service;

import com.nhuhuy05.enghub.common.exception.AppException;
import com.nhuhuy05.enghub.common.exception.ErrorCode;
import com.nhuhuy05.enghub.test.dto.*;
import com.nhuhuy05.enghub.test.entity.Test;
import com.nhuhuy05.enghub.test.entity.TestCollection;
import com.nhuhuy05.enghub.test.entity.TestPart;
import com.nhuhuy05.enghub.test.repository.TestCollectionRepository;
import com.nhuhuy05.enghub.test.repository.TestPartRepository;
import com.nhuhuy05.enghub.test.repository.TestRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestManagementService {
    TestCollectionRepository testCollectionRepository;
    TestRepository testRepository;
    TestPartRepository testPartRepository;

    private static final List<PartSeed> TOEIC_PARTS = List.of(
            new PartSeed(1, "Photographs"),
            new PartSeed(2, "Question-Response"),
            new PartSeed(3, "Short Conversations"),
            new PartSeed(4, "Short Talks"),
            new PartSeed(5, "Incomplete Sentences"),
            new PartSeed(6, "Text Completion"),
            new PartSeed(7, "Reading Comprehension")
    );

    @Transactional
    public TestCollectionResponse createCollection(TestCollectionRequest request) {
        if (testCollectionRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.TEST_COLLECTION_EXISTED);
        }

        TestCollection collection = TestCollection.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return toCollectionResponse(testCollectionRepository.save(collection));
    }

    @Transactional(readOnly = true)
    public List<TestCollectionResponse> getCollections() {
        return testCollectionRepository.findAll().stream()
                .sorted(Comparator.comparing(TestCollection::getName))
                .map(this::toCollectionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TestResponse> getTestsByCollection(Long collectionId) {
        TestCollection collection = testCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_COLLECTION_NOT_EXISTED));

        return testRepository.findAllByCollectionIdOrderByTestNumberAsc(collection.getId()).stream()
                .map(this::toTestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TestResponse getTestById(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        return toTestResponse(test);
    }

    @Transactional
    public TestResponse createTest(TestCreationRequest request) {
        validateCollectionNumberPair(request.getCollectionId(), request.getTestNumber());

        TestCollection collection = null;
        if (request.getCollectionId() != null) {
            collection = testCollectionRepository.findById(request.getCollectionId())
                    .orElseThrow(() -> new AppException(ErrorCode.TEST_COLLECTION_NOT_EXISTED));

            if (testRepository.existsByCollectionIdAndTestNumber(request.getCollectionId(), request.getTestNumber())) {
                throw new AppException(ErrorCode.TEST_NUMBER_EXISTED);
            }
        }

        Test test = Test.builder()
                .collection(collection)
                .testNumber(request.getTestNumber())
                .title(request.getTitle())
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes())
                .totalQuestions(request.getTotalQuestions() == null ? 200 : request.getTotalQuestions())
                .published(false)
                .build();

        Test savedTest = testRepository.save(test);
        initDefaultParts(savedTest);
        return toTestResponse(savedTest);
    }

    @Transactional
    public List<TestPartResponse> initDefaultParts(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_EXISTED));

        initDefaultParts(test);
        return testPartRepository.findAllByTestIdOrderByPartNumberAsc(testId).stream()
                .map(this::toPartResponse)
                .toList();
    }

    private void initDefaultParts(Test test) {
        for (PartSeed partSeed : TOEIC_PARTS) {
            if (!testPartRepository.existsByTestIdAndPartNumber(test.getId(), partSeed.partNumber())) {
                testPartRepository.save(TestPart.builder()
                        .test(test)
                        .partNumber(partSeed.partNumber())
                        .title(partSeed.title())
                        .build());
            }
        }
    }

    private void validateCollectionNumberPair(Long collectionId, Integer testNumber) {
        boolean hasCollection = collectionId != null;
        boolean hasTestNumber = testNumber != null;
        if (hasCollection != hasTestNumber) {
            throw new AppException(ErrorCode.INVALID_TEST_COLLECTION_NUMBER);
        }
    }

    private TestCollectionResponse toCollectionResponse(TestCollection collection) {
        return TestCollectionResponse.builder()
                .id(collection.getId())
                .name(collection.getName())
                .description(collection.getDescription())
                .createdAt(collection.getCreatedAt())
                .build();
    }

    private TestResponse toTestResponse(Test test) {
        return TestResponse.builder()
                .id(test.getId())
                .collectionId(test.getCollection() == null ? null : test.getCollection().getId())
                .collectionName(test.getCollection() == null ? null : test.getCollection().getName())
                .testNumber(test.getTestNumber())
                .title(test.getTitle())
                .description(test.getDescription())
                .totalQuestions(test.getTotalQuestions())
                .durationMinutes(test.getDurationMinutes())
                .published(test.getPublished())
                .createdAt(test.getCreatedAt())
                .build();
    }

    private TestPartResponse toPartResponse(TestPart testPart) {
        return TestPartResponse.builder()
                .id(testPart.getId())
                .testId(testPart.getTest().getId())
                .partNumber(testPart.getPartNumber())
                .title(testPart.getTitle())
                .build();
    }

    private record PartSeed(int partNumber, String title) {
    }
}
