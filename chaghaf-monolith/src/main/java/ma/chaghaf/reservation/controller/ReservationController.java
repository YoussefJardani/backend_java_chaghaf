package ma.chaghaf.reservation.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.reservation.dto.ReservationDtos.*;
import ma.chaghaf.reservation.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService service;

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> myReservations(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        return ResponseEntity.ok(service.findByUser(userId));
    }

    @GetMapping("/salles")
    public ResponseEntity<List<SalleInfo>> salles() {
        return ResponseEntity.ok(service.listSalles());
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(HttpServletRequest req,
                                                       @RequestBody Map<String, Object> body) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        return ResponseEntity.ok(service.create(userId, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> cancel(HttpServletRequest req,
                                                       @PathVariable Long id) {
        Long userId = (Long) req.getAttribute("X-User-Id");
        service.cancel(userId, id);
        return ResponseEntity.ok(Map.of("message", "Réservation annulée"));
    }
}
