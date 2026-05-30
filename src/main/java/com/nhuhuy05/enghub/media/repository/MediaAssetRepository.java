package com.nhuhuy05.enghub.media.repository;

import com.nhuhuy05.enghub.media.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    boolean existsByTestIdAndLabelAndMediaType(Long testId, String label, String mediaType);

    Optional<MediaAsset> findByTestIdAndLabelAndMediaType(Long testId, String label, String mediaType);

    Optional<MediaAsset> findFirstByTestIdAndMediaTypeAndLabelStartingWithOrderByLabelAsc(
            Long testId,
            String mediaType,
            String labelPrefix
    );

    Optional<MediaAsset> findByIdAndTestId(Long id, Long testId);

    List<MediaAsset> findAllByTestIdOrderByCreatedAtAsc(Long testId);
}
