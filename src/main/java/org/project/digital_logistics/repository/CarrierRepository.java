package org.project.digital_logistics.repository;

import org.project.digital_logistics.model.enums.CarrierStatus;
import org.project.digital_logistics.model.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, Long> {

    Optional<Carrier> findByCode(String code);
    boolean existsByCode(String code);
    List<Carrier> findByStatus(CarrierStatus status);
    @Query("SELECT c FROM Carrier c WHERE c.status = 'ACTIVE' AND c.currentDailyShipments < c.maxDailyCapacity")
    List<Carrier> findAvailableCarriers();
}