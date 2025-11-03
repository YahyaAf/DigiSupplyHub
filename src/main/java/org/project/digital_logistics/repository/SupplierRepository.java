package org.project.digital_logistics.repository;

import org.project.digital_logistics.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    boolean existsByMatricule(String matricule);
}
