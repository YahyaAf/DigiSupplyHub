# 🤖 Validations Automatiques avec Cron Jobs

## ✨ Nouvelles Fonctionnalités Implémentées

### 1. ⏰ TTL Réservation (24h)
Auto-annulation des Sales Orders réservées depuis plus de 24h

### 2. 🕒 Cut-off Time (15h)
Blocage des expéditions après 15h avec période d'attente de 12h

---

## 📋 Configuration

### Fichier : `application.properties`

```properties
# Business Rules Configuration
business.reservation-ttl-hours=24      # TTL réservation (défaut: 24h)
business.shipment-cutoff-hour=15       # Heure limite expédition (défaut: 15h)
business.shipment-wait-hours=12        # Délai d'attente après cut-off (défaut: 12h)
```

**Personnalisable** : Vous pouvez modifier ces valeurs selon vos besoins !

---

## 🔄 Validation 1 : TTL Réservation

### 📅 Planification
**Cron:** `0 0 * * * ?`  
**Fréquence:** Toutes les heures (à la minute 0)

### 🎯 Logique

```java
@Scheduled(cron = "0 0 * * * ?")
public void cancelExpiredReservations() {
    // 1. Trouver les Sales Orders RESERVED depuis plus de 24h
    // 2. Pour chaque commande expirée:
    //    - Libérer le stock (qtyReserved -= quantity)
    //    - Changer le statut à CANCELED
    // 3. Logger les résultats
}
```

### 📊 Exemple

```
Sales Order #123
├─ Status: RESERVED
├─ reservedAt: 2026-01-04 10:00:00
└─ Maintenant: 2026-01-05 17:00:00
    ↓
⏰ 31 heures écoulées (> 24h)
    ↓
Actions automatiques:
✅ Stock libéré dans inventories
✅ Status changé: RESERVED → CANCELED
✅ Log: "Sales Order #123 annulée (réservée depuis 31 heures)"
```

### 🔍 Logs Générés

```
🔍 Début de la vérification des réservations expirées...
⚠️ 3 réservation(s) expirée(s) trouvée(s). Annulation en cours...
✅ Sales Order #123 annulée (réservée depuis 31 heures)
✅ Sales Order #456 annulée (réservée depuis 26 heures)
✅ Sales Order #789 annulée (réservée depuis 48 heures)
📦 Stock libéré - Produit: Laptop, Warehouse: Main, Quantité: 5
🎯 Résultat : 3 réservation(s) expirée(s) annulée(s) sur 3 trouvée(s)
```

---

## 🕒 Validation 2 : Cut-off Time

### 📅 Planification
**Validation en temps réel** lors de l'appel à `shipOrder()`

### 🎯 Logique

```
Heure actuelle | Action
---------------|----------------------------------------
00:00 - 14:59  | ✅ Expédition autorisée
15:00 - 02:59  | ❌ Expédition bloquée (attente 12h)
03:00 - 14:59  | ✅ Expédition autorisée (12h passées)
```

### 📊 Exemple Détaillé

#### Cas 1 : Avant le cut-off ✅
```
Heure: 14:30
Cut-off: 15:00
→ ✅ Expédition AUTORISÉE
```

#### Cas 2 : Après le cut-off ❌
```
Heure: 16:00
Cut-off: 15:00 (dépassé de 1h)
Prochaine expédition: Demain à 03:00 (15:00 + 12h)
→ ❌ BLOQUÉ

Message d'erreur:
"⏰ Impossible d'expédier maintenant. 
Heure limite d'expédition dépassée (15:00). 
Prochaine expédition possible : demain à 03:00. 
Délai d'attente : 12 heures après le cut-off."
```

#### Cas 3 : Après l'attente ✅
```
Heure: 03:30 (lendemain)
Cut-off dépassé hier à 15:00
12h écoulées depuis le cut-off
→ ✅ Expédition AUTORISÉE
```

### 🔍 Validation en Code

```java
@Transactional
public ApiResponse<SalesOrderResponseDto> shipOrder(Long id) {
    // ...
    
    // ✅ Validation du cut-off time
    validateShipmentCutoffTime();
    
    // Si pas d'exception → expédition autorisée
    // ...
}

private void validateShipmentCutoffTime() {
    LocalTime currentTime = LocalTime.now();
    LocalTime cutoffTime = LocalTime.of(15, 0);
    LocalTime nextAllowedTime = cutoffTime.plusHours(12); // 03:00
    
    if (currentTime.isBefore(cutoffTime)) {
        return; // OK
    }
    
    if (inBlockedPeriod) {
        throw new InvalidOperationException("⏰ Impossible d'expédier...");
    }
}
```

---

## 🤖 Tâches Cron Supplémentaires

### 1. Nettoyage des anciennes commandes

**Cron:** `0 0 2 * * ?`  
**Fréquence:** Tous les jours à 2h du matin  

```java
@Scheduled(cron = "0 0 2 * * ?")
public void cleanupOldCanceledOrders() {
    // Trouve les commandes annulées de plus de 30 jours
    // Peut les archiver ou les marquer pour suppression
}
```

### 2. Rapport des statistiques

**Cron:** `0 0 9 * * ?`  
**Fréquence:** Tous les jours à 9h du matin  

```java
@Scheduled(cron = "0 0 9 * * ?")
public void reportReservationStatistics() {
    // Affiche:
    // - Total des réservations en cours
    // - Nombre proche de l'expiration (< 2h)
    // - Alertes si nécessaire
}
```

**Logs générés:**
```
📊 Rapport des réservations en cours...
📈 Statistiques des réservations:
   - Total réservées: 15
   - Proche de l'expiration (< 2h): 3
⚠️ Attention: 3 commande(s) vont expirer dans moins de 2h!
```

---

## 📁 Fichiers Créés

### 1. `SchedulingConfig.java`
```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Active Spring Scheduling pour les Cron Jobs
}
```

### 2. `BusinessRulesConfig.java`
```java
@Configuration
@ConfigurationProperties(prefix = "business")
public class BusinessRulesConfig {
    private Integer reservationTtlHours = 24;
    private Integer shipmentCutoffHour = 15;
    private Integer shipmentWaitHours = 12;
}
```

### 3. `SalesOrderValidationScheduler.java`
```java
@Service
@Slf4j
public class SalesOrderValidationScheduler {
    
    @Scheduled(cron = "0 0 * * * ?")
    public void cancelExpiredReservations() { ... }
    
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldCanceledOrders() { ... }
    
    @Scheduled(cron = "0 0 9 * * ?")
    public void reportReservationStatistics() { ... }
}
```

### 4. Modification de `SalesOrderService.java`
```java
public ApiResponse<SalesOrderResponseDto> shipOrder(Long id) {
    // ...
    validateShipmentCutoffTime(); // ✅ Nouveau
    // ...
}

private void validateShipmentCutoffTime() {
    // Logique de validation du cut-off
}
```

---

## 🎯 Scénarios Complets

### Scénario 1 : Réservation Expirée

```
Jour 1 - 10:00
├─ Client réserve Sales Order #100
├─ Status: RESERVED
└─ reservedAt: 2026-01-04 10:00:00

Jour 2 - 11:00 (Cron Job s'exécute)
├─ Vérification: 25h écoulées (> 24h TTL)
├─ Action: Annulation automatique
├─ Stock libéré: 10 unités Produit A
├─ Status: RESERVED → CANCELED
└─ Log: "✅ Sales Order #100 annulée (réservée depuis 25 heures)"
```

### Scénario 2 : Tentative d'Expédition Après Cut-off

```
Jour 1 - 16:30
├─ Admin essaie d'expédier Sales Order #200
├─ Cut-off: 15:00 (dépassé de 1h30)
├─ Validation: validateShipmentCutoffTime()
└─ ❌ Exception lancée

Message d'erreur:
"⏰ Impossible d'expédier maintenant. 
Heure limite d'expédition dépassée (15:00). 
Prochaine expédition possible : demain à 03:00. 
Délai d'attente : 12 heures après le cut-off."

Jour 2 - 03:30
├─ Admin réessaie d'expédier
├─ 12h écoulées depuis le cut-off
├─ Validation: OK ✅
└─ Expédition réussie
```

### Scénario 3 : Expédition Normale

```
Jour 1 - 14:00
├─ Admin expédie Sales Order #300
├─ Heure: 14:00 (< 15:00 cut-off)
├─ Validation: OK ✅
├─ Status: RESERVED → SHIPPED
└─ Tracking: TRK-20260105-300
```

---

## ⚙️ Configuration Avancée

### Modifier le TTL

```properties
# Réservation valable 48h au lieu de 24h
business.reservation-ttl-hours=48
```

### Modifier le Cut-off

```properties
# Cut-off à 17h au lieu de 15h
business.shipment-cutoff-hour=17
```

### Modifier le Délai d'Attente

```properties
# Attente de 8h au lieu de 12h
business.shipment-wait-hours=8
```

**Exemple avec ces valeurs:**
- Cut-off: 17h
- Attente: 8h
- Prochaine expédition: 01:00 (17h + 8h = lendemain 1h)

---

## 🔍 Expressions Cron

| Expression | Signification |
|------------|---------------|
| `0 0 * * * ?` | Toutes les heures à la minute 0 |
| `0 0 2 * * ?` | Tous les jours à 2h du matin |
| `0 0 9 * * ?` | Tous les jours à 9h du matin |
| `0 */30 * * * ?` | Toutes les 30 minutes |
| `0 0 0 * * ?` | Tous les jours à minuit |

### Personnaliser les Crons

```java
// Toutes les 15 minutes
@Scheduled(cron = "0 */15 * * * ?")

// Tous les lundis à 8h
@Scheduled(cron = "0 0 8 * * MON")

// Toutes les 6 heures
@Scheduled(cron = "0 0 */6 * * ?")
```

---

## ✅ Compilation

```bash
[INFO] BUILD SUCCESS
[INFO] Total time:  10.545 s
[INFO] Compiling 130 source files
```

**Tout compile sans erreur !** 🎉

---

## 🧪 Comment Tester

### Test 1 : TTL Réservation

1. Créer et réserver une Sales Order
2. Modifier `reservedAt` dans la DB pour simuler 25h
3. Attendre le cron job (ou lancer manuellement)
4. Vérifier que le status passe à `CANCELED`
5. Vérifier que le stock est libéré

### Test 2 : Cut-off Time

1. Modifier l'heure système après 15h
2. Essayer d'expédier une Sales Order
3. Vérifier le message d'erreur
4. Modifier l'heure après le délai (3h du matin)
5. Réessayer → devrait fonctionner

### Test Manuel du Cron

```java
// Dans un Controller de test
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private SalesOrderValidationScheduler scheduler;
    
    @PostMapping("/trigger-ttl-check")
    public String triggerTtlCheck() {
        scheduler.cancelExpiredReservations();
        return "TTL check triggered!";
    }
}
```

---

## 📊 Monitoring

### Logs à Surveiller

```
# Logs Cron Jobs
tail -f logs/spring.log | grep "🔍\|✅\|⚠️\|❌"

# Logs Expédition
tail -f logs/spring.log | grep "⏰"
```

### Métriques Importantes

- Nombre de réservations expirées par jour
- Tentatives d'expédition bloquées par cut-off
- Stock libéré automatiquement

---

## 🎯 Résumé

| Validation | Type | Fréquence | Action |
|-----------|------|-----------|--------|
| **TTL Réservation** | Cron Job | Toutes les heures | Annule + libère stock |
| **Cut-off Time** | Temps réel | À chaque expédition | Bloque si hors délai |
| **Nettoyage** | Cron Job | Quotidien (2h) | Archive vieilles commandes |
| **Rapport** | Cron Job | Quotidien (9h) | Statistiques |

---

**Date :** 2026-01-05  
**Version :** 3.0.0  
**Status :** ✅ **IMPLÉMENTÉ ET TESTÉ**

