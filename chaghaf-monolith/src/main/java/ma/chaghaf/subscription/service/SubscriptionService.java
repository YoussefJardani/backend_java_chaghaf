package ma.chaghaf.subscription.service;

import lombok.RequiredArgsConstructor;
import ma.chaghaf.subscription.dto.SubscriptionDtos.*;
import ma.chaghaf.subscription.entity.Subscription;
import ma.chaghaf.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository repo;

    public SubscriptionResponse getActiveForUser(Long userId) {
        return repo.findByUserIdAndStatus(userId, Subscription.Status.ACTIVE)
            .map(this::toDto)
            .orElse(null);
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

    private SubscriptionResponse toDto(Subscription s) {
        return new SubscriptionResponse(
            s.getId(), s.getUserId(),
            s.getPackType().name(), s.getDuration().name(),
            s.getStartDate(), s.getEndDate(), s.getPrice(),
            s.getStatus().name(), s.getDaysLeft()
        );
    }
}
