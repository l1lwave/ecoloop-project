package com.ecoapp.scan.repository;

import com.ecoapp.scan.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, String> {}
