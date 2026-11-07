package com.healthcare.repository;

import com.healthcare.entity.Prediction;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    /** Patient-scoped search. `q` must never be null (pass ""). */
    @Query("select p from Prediction p where p.patient.id = :uid "
            + "and lower(p.predictedClass) like lower(concat('%', :q, '%')) "
            + "and p.createdAt >= :from and p.createdAt < :to")
    Page<Prediction> search(@Param("uid") Long patientId, @Param("q") String q, @Param("from") Instant from,
                            @Param("to") Instant to, Pageable pageable);

    /** Ownership is part of the query: another patient's id simply yields no row. */
    Optional<Prediction> findByIdAndPatientId(Long id, Long patientId);

    long countByPatientId(Long patientId);

    long countByCreatedAtAfter(Instant since);

    @Query("select p.createdAt from Prediction p where p.patient.id = :uid and p.createdAt >= :since")
    List<Instant> createdAtSince(@Param("uid") Long patientId, @Param("since") Instant since);

    @Query("select p.createdAt from Prediction p where p.createdAt >= :since")
    List<Instant> createdAtSince(@Param("since") Instant since);

    /** Most selected symptoms; groups with fewer than :min distinct patients are suppressed. */
    @Query("select s.name, count(distinct s.prediction.patient.id) from PredictionSymptom s "
            + "group by s.name having count(distinct s.prediction.patient.id) >= :min "
            + "order by count(distinct s.prediction.patient.id) desc, s.name asc")
    List<Object[]> topSymptoms(@Param("min") long min, Pageable pageable);

    @Query("select p.predictedClass, count(p) from Prediction p group by p.predictedClass "
            + "having count(distinct p.patient.id) >= :min order by count(p) desc, p.predictedClass asc")
    List<Object[]> topPredictedClasses(@Param("min") long min, Pageable pageable);

    @Query("select p.modelName, p.modelVersion, count(p) from Prediction p "
            + "group by p.modelName, p.modelVersion order by count(p) desc")
    List<Object[]> modelUsage();
}
