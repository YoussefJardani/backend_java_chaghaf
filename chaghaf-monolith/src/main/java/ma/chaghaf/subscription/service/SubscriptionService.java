package ma.chaghaf.subscription.service;

import lombok.RequiredArgsConstructor;
import ma.chaghaf.subscription.dto.SubscriptionDtos.*;
import ma.chaghaf.subscription.entity.DayAccess;
import ma.chaghaf.subscription.entity.Subscription;
import ma.chaghaf.subscription.repository.DayAccessRepository;
import ma.chaghaf.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository repo;
    private final DayAccessRepository dayAccessRepo;

    public SubscriptionResponse getActiveForUser(Long userId) {
        return repo.findByUserIdAndStatus(userId, Subscription.Status.ACTIVE)
            .map(this::toDto)
            .orElse(null);
    }

    public List<SubscriptionResponse> getHistory(Long userId) {
        return repo.findByUserId(userId).stream().map(this::toDto).toList();
    }

    public List<PackInfo> listPacks() {
        return List.of(
            new PackInfo("BASIC", "Basique", "Accès aux espaces de coworking",
                new BigDecimal("500.00"),
                new String[]{"Wifi haut débit", "Café illimité", "Accès 5j/7"}),
            new PackInfo("PREMIUM", "Premium", "Coworking + salles de réunion",
                new BigDecimal("1200.00"),
                new String[]{"Tout du Basique", "10h salle de réunion", "Imprimante"}),
            new PackInfo("VIP", "VIP", "Tout inclus + bureaux privés",
                new BigDecimal("2500.00"),
                new String[]{"Tout du Premium", "Bureau privé", "Salles illimitées"}),
            new PackInfo("STUDENT", "Étudiant", "Tarif réduit pour étudiants",
                new BigDecimal("250.00"),
                new String[]{"Wifi", "Café", "Accès 3j/semaine"})
        );
    }

    @Transactional
    public SubscriptionResponse subscribe(Long userId, Map<String, Object> body) {
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");

        String packTypeStr = (String) body.get("packType");
        String durationStr = (String) body.get("duration");
        if (packTypeStr == null || durationStr == null) {
            throw new IllegalArgumentException("packType et duration sont requis");
        }

        Subscription.PackType packType;
        try { packType = Subscription.PackType.valueOf(packTypeStr.toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Pack invalide: " + packTypeStr); }

        Subscription.Duration duration = parseDuration(durationStr);

        // Désactiver l'abonnement actif courant si présent
        repo.findByUserIdAndStatus(userId, Subscription.Status.ACTIVE).ifPresent(old -> {
            old.setStatus(Subscription.Status.EXPIRED);
            repo.save(old);
        });

        LocalDate start = LocalDate.now();
        LocalDate end = computeEndDate(start, duration);
        BigDecimal price = priceFor(packType, duration);

        Subscription s = Subscription.builder()
            .userId(userId)
            .packType(packType)
            .duration(duration)
            .startDate(start)
            .endDate(end)
            .price(price)
            .status(Subscription.Status.ACTIVE)
            .build();
        return toDto(repo.save(s));
    }

    @Transactional
    public SubscriptionResponse renew(Long userId) {
        Subscription current = repo.findByUserIdAndStatus(userId, Subscription.Status.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Aucun abonnement actif à renouveler"));

        LocalDate start = current.getEndDate().isAfter(LocalDate.now()) ? current.getEndDate() : LocalDate.now();
        LocalDate end = computeEndDate(start, current.getDuration());
        current.setStartDate(start);
        current.setEndDate(end);
        return toDto(repo.save(current));
    }

    @Transactional
    public Map<String, Object> purchaseDayAccess(Long userId, Map<String, Object> body) {
        if (userId == null) throw new IllegalArgumentException("Utilisateur non authentifié");

        String accessTypeStr = body.get("accessType") != null ? body.get("accessType").toString() : "DAY_PASS";
        DayAccess.AccessType accessType;
        try { accessType = DayAccess.AccessType.valueOf(accessTypeStr.toUpperCase()); }
        catch (IllegalArgumentException e) { accessType = DayAccess.AccessType.DAY_PASS; }

        DayAccess da = DayAccess.builder()
            .userId(userId)
            .qrToken(UUID.randomUUID().toString())
            .accessDate(LocalDate.now())
            .accessType(accessType)
            .used(false)
            .build();
        da = dayAccessRepo.save(da);

        Map<String, Object> response = new HashMap<>();
        response.put("id", da.getId());
        response.put("qrToken", da.getQrToken());
        response.put("accessDate", da.getAccessDate().toString());
        response.put("accessType", da.getAccessType().name());
        response.put("price", priceForAccess(accessType));
        return response;
    }

    private Subscription.Duration parseDuration(String s) {
        String upper = s.toUpperCase();
        return switch (upper) {
            case "WEEKLY", "WEEK"        -> Subscription.Duration.WEEK;
            case "MONTHLY", "MONTH"      -> Subscription.Duration.MONTH;
            case "QUARTERLY", "QUARTER"  -> Subscription.Duration.QUARTER;
            case "ANNUAL", "YEARLY", "YEAR" -> Subscription.Duration.YEAR;
            default -> {
                try { yield Subscription.Duration.valueOf(upper); }
                catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Durée invalide: " + s);
                }
            }
        };
    }

    private LocalDate computeEndDate(LocalDate start, Subscription.Duration d) {
        return switch (d) {
            case WEEK    -> start.plusWeeks(1);
            case MONTH   -> start.plusMonths(1);
            case QUARTER -> start.plusMonths(3);
            case YEAR    -> start.plusYears(1);
        };
    }

    private BigDecimal priceFor(Subscription.PackType pack, Subscription.Duration duration) {
        BigDecimal base = switch (pack) {
            case BASIC   -> new BigDecimal("500.00");
            case PREMIUM -> new BigDecimal("1200.00");
            case VIP     -> new BigDecimal("2500.00");
            case STUDENT -> new BigDecimal("250.00");
        };
        BigDecimal multiplier = switch (duration) {
            case WEEK    -> new BigDecimal("0.30");
            case MONTH   -> BigDecimal.ONE;
            case QUARTER -> new BigDecimal("2.85");
            case YEAR    -> new BigDecimal("11.00");
        };
        return base.multiply(multiplier);
    }

    private BigDecimal priceForAccess(DayAccess.AccessType type) {
        return switch (type) {
            case DAY_PASS -> new BigDecimal("80.00");
            case GUEST    -> new BigDecimal("0.00");
            default       -> new BigDecimal("80.00");
        };
    }

    private SubscriptionResponse toDto(Subscription s) {
        return new SubscriptionResponse(
            s.getId(), s.getUserId(),
            s.getPackType().name(), s.getDuration().name(),
            s.getStartDate(), s.getEndDate(), s.getPrice(),
            s.getStatus().name(), s.getDaysLeft()
        );
    }
}
