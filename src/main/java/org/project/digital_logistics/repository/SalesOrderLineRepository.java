package org.project.digital_logistics.repository;

import org.project.digital_logistics.model.Product;
import org.project.digital_logistics.model.SalesOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, Long> {

    boolean existsByProduct(Product product);
    SalesOrderLine findByProduct(Product product);
}
