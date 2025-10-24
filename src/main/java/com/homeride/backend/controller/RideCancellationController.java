// backend/src/main/java/com/homeride/backend/controller/RideCancellationController.java
package com.homeride.backend.controller;

import com.homeride.backend.dto.RideCancellationDTO;
import com.homeride.backend.service.RideCancellationService;
import com.homeride.backend.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/rides")
public class RideCancellationController {

    private final RideCancellationService rideCancellationService;
    private final EmployeeService employeeService;

    @Autowired
    public RideCancellationController(
            RideCancellationService rideCancellationService,
            EmployeeService employeeService) {
        this.rideCancellationService = rideCancellationService;
        this.employeeService = employeeService;
    }

    /**
     * Cancel ride as a passenger (leave the ride)
     * POST /api/rides/{rideId}/cancel-passenger
     */
    @PostMapping("/{rideId}/cancel-passenger")
    public ResponseEntity<?> cancelRideAsPassenger(
            @PathVariable Long rideId,
            @RequestBody RideCancellationDTO cancellationDTO,
            Principal principal) {
        try {
            String reason = cancellationDTO.getReason() != null ? cancellationDTO.getReason() : "No reason provided";
            rideCancellationService.cancelRideAsPassenger(rideId, principal.getName(), reason);
            return ResponseEntity.ok(Map.of("message", "You have successfully left the ride"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cancel ride as a driver (cancel the entire ride)
     * POST /api/rides/{rideId}/cancel-driver
     */
    @PostMapping("/{rideId}/cancel-driver")
    public ResponseEntity<?> cancelRideAsDriver(
            @PathVariable Long rideId,
            Principal principal) {
        try {
            rideCancellationService.cancelRideAsDriver(rideId, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Ride has been cancelled successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}