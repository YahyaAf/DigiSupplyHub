-- =====================================================
-- Migration SQL - Ajout du statut BACKORDER
-- Date: 2026-01-05
-- Description: Mise à jour de la contrainte sales_orders_status_check
--              pour inclure le nouveau statut BACKORDER
-- =====================================================

-- Étape 1: Supprimer l'ancienne contrainte
ALTER TABLE sales_orders
DROP CONSTRAINT IF EXISTS sales_orders_status_check;

-- Étape 2: Ajouter la nouvelle contrainte avec BACKORDER
ALTER TABLE sales_orders
ADD CONSTRAINT sales_orders_status_check
CHECK (status IN ('CREATED', 'BACKORDER', 'RESERVED', 'SHIPPED', 'DELIVERED', 'CANCELED'));

-- Étape 3: Afficher un message de confirmation
DO $$
BEGIN
    RAISE NOTICE 'Contrainte sales_orders_status_check mise à jour avec succès!';
    RAISE NOTICE 'Le statut BACKORDER est maintenant autorisé.';
END $$;

-- =====================================================
-- Vérification (optionnel)
-- =====================================================

-- Afficher la contrainte mise à jour
SELECT
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conname = 'sales_orders_status_check';

-- =====================================================
-- Fin de la migration
-- =====================================================

