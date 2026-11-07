package com.ecoapp.scan.repository;

import com.ecoapp.scan.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {}
