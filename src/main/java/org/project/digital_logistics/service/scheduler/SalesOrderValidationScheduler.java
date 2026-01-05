package org.project.digital_logistics.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.digital_logistics.config.BusinessRulesConfig;
import org.project.digital_logistics.model.Inventory;
import org.project.digital_logistics.model.SalesOrder;
import org.project.digital_logistics.model.SalesOrderLine;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.repository.InventoryRepository;
import org.project.digital_logistics.repository.SalesOrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesOrderValidationScheduler {

    private final SalesOrderRepository salesOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final BusinessRulesConfig businessRulesConfig;

    /**
     * Tâche planifiée : Annuler automatiquement les réservations expirées
     * Exécutée toutes les heures
     */
    @Scheduled(cron = "0 0 * * * ?") // Toutes les heures à la minute 0
    @Transactional
    public void cancelExpiredReservations() {
        log.info("🔍 Début de la vérification des réservations expirées...");

        LocalDateTime expirationTime = LocalDateTime.now()
                .minusHours(businessRulesConfig.getReservationTtlHours());

        // Trouver toutes les Sales Orders RESERVED qui ont dépassé le TTL
        List<SalesOrder> expiredOrders = salesOrderRepository.findAll()
                .stream()
                .filter(order -> order.getStatus() == OrderStatus.RESERVED)
                .filter(order -> order.getReservedAt() != null)
                .filter(order -> order.getReservedAt().isBefore(expirationTime))
                .toList();

        if (expiredOrders.isEmpty()) {
            log.info("Aucune réservation expirée trouvée.");
            return;
        }

        log.info("réservation(s) expirée(s) trouvée(s). Annulation en cours...", expiredOrders.size());

        int canceledCount = 0;
        for (SalesOrder order : expiredOrders) {
            try {
                cancelExpiredReservation(order);
                canceledCount++;
                log.info("Sales Order #{} annulée (réservée depuis {} heures)",
                        order.getId(),
                        java.time.Duration.between(order.getReservedAt(), LocalDateTime.now()).toHours());
            } catch (Exception e) {
                log.error("Erreur lors de l'annulation de la Sales Order #{}: {}",
                        order.getId(), e.getMessage());
            }
        }

        log.info("Résultat : {} réservation(s) expirée(s) annulée(s) sur {} trouvée(s)",
                canceledCount, expiredOrders.size());
    }

    /**
     * Annule une réservation expirée et libère le stock
     */
    private void cancelExpiredReservation(SalesOrder order) {
        // Libérer les quantités réservées dans les inventaires
        for (SalesOrderLine line : order.getOrderLines()) {
            Inventory inventory = inventoryRepository
                    .findByWarehouseIdAndProductId(
                            line.getWarehouse().getId(),
                            line.getProduct().getId()
                    )
                    .orElse(null);

            if (inventory != null) {
                int previousReserved = inventory.getQtyReserved();
                inventory.setQtyReserved(inventory.getQtyReserved() - line.getQuantity());
                inventoryRepository.save(inventory);

                log.debug("Stock libéré - Produit: {}, Warehouse: {}, Quantité: {} (réservé: {} → {})",
                        line.getProduct().getName(),
                        line.getWarehouse().getName(),
                        line.getQuantity(),
                        previousReserved,
                        inventory.getQtyReserved());
            }
        }

        // Changer le statut à CANCELED
        order.setStatus(OrderStatus.CANCELED);
        salesOrderRepository.save(order);
    }

    /**
     * Tâche planifiée : Vérifier et nettoyer les anciennes commandes annulées
     * Exécutée tous les jours à 2h du matin
     */
    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    @Transactional
    public void cleanupOldCanceledOrders() {
        log.info("Début du nettoyage des anciennes commandes annulées...");

        LocalDateTime cleanupThreshold = LocalDateTime.now().minusDays(30);

        List<SalesOrder> oldCanceledOrders = salesOrderRepository.findAll()
                .stream()
                .filter(order -> order.getStatus() == OrderStatus.CANCELED)
                .filter(order -> order.getCreatedAt() != null)
                .filter(order -> order.getCreatedAt().isBefore(cleanupThreshold))
                .toList();

        if (!oldCanceledOrders.isEmpty()) {
            log.info("{} commande(s) annulée(s) de plus de 30 jours trouvée(s) (peuvent être archivées)",
                    oldCanceledOrders.size());
            // Ici vous pouvez ajouter une logique d'archivage si nécessaire
        } else {
            log.info("Aucune ancienne commande annulée à nettoyer.");
        }
    }

    /**
     * Tâche de monitoring : Afficher les statistiques des réservations
     * Exécutée tous les jours à 9h du matin
     */
    @Scheduled(cron = "0 0 9 * * ?") // Tous les jours à 9h
    public void reportReservationStatistics() {
        log.info("Rapport des réservations en cours...");

        List<SalesOrder> reservedOrders = salesOrderRepository.findByStatus(OrderStatus.RESERVED);

        if (reservedOrders.isEmpty()) {
            log.info("Aucune réservation en cours.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningThreshold = now.minusHours(businessRulesConfig.getReservationTtlHours() - 2);

        long nearExpiration = reservedOrders.stream()
                .filter(order -> order.getReservedAt() != null)
                .filter(order -> order.getReservedAt().isBefore(warningThreshold))
                .count();

        log.info("Statistiques des réservations:");
        log.info("   - Total réservées: {}", reservedOrders.size());
        log.info("   - Proche de l'expiration (< 2h): {}", nearExpiration);

        if (nearExpiration > 0) {
            log.warn("Attention: {} commande(s) vont expirer dans moins de 2h!", nearExpiration);
        }
    }
}

