package ma.chaghaf.reservation.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ma.chaghaf.reservation.dto.ReservationDtos.*;
import ma.chaghaf.reservation.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
