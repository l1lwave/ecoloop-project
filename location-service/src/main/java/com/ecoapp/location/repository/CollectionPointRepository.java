package com.ecoapp.location.repository;

import com.ecoapp.location.model.CollectionPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollectionPointRepository extends JpaRepository<CollectionPoint, Long> {

    @Query("SELECT c FROM CollectionPoint c WHERE c.chain = :chain")
    List<CollectionPoint> findByChain(@Param("chain") String chain);

    @Query("SELECT c FROM CollectionPoint c WHERE c.acceptedMaterials LIKE %:material%")
    List<CollectionPoint> findByAcceptedMaterialsContaining(@Param("material") String material);
}
