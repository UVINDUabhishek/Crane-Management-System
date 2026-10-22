package com.uvindu.crane_management_system.controller;

import com.uvindu.crane_management_system.dto.CraneIdleResponse;
import com.uvindu.crane_management_system.dto.DashboardResponse;
import com.uvindu.crane_management_system.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Get dashboard summary
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard() {

        return ResponseEntity.ok(
                dashboardService.getDashboard()
        );
    }

    // Get crane idle statistics for a selected date range
    @GetMapping("/crane-idle")
    public ResponseEntity<List<CraneIdleResponse>> getCraneIdleStatistics(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {

        return ResponseEntity.ok(
                dashboardService.getCraneIdleStatistics(
                        fromDate,
                        toDate
                )
        );
    }
}
