-- =====================================================
-- Migration SQL - Sales Order Logic Update
-- Date: 2026-01-05
-- Description: Ajout du champ related_sales_order_id
--              dans la table purchase_orders
-- =====================================================

-- Vérifier si la colonne existe déjà
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'purchase_orders'
        AND column_name = 'related_sales_order_id'
    ) THEN
        -- Ajouter la colonne si elle n'existe pas
        ALTER TABLE purchase_orders
        ADD COLUMN related_sales_order_id BIGINT;

        RAISE NOTICE 'Colonne related_sales_order_id ajoutée avec succès';
    ELSE
        RAISE NOTICE 'Colonne related_sales_order_id existe déjà';
    END IF;
END $$;

-- Créer un index pour améliorer les performances des recherches
CREATE INDEX IF NOT EXISTS idx_purchase_orders_related_sales_order
ON purchase_orders(related_sales_order_id);

-- Ajouter un commentaire pour documenter le champ
COMMENT ON COLUMN purchase_orders.related_sales_order_id IS
'ID de la Sales Order liée (pour les Purchase Orders créés automatiquement en cas de rupture de stock)';

-- =====================================================
-- Fin de la migration
-- =====================================================

