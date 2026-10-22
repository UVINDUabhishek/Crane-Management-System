package com.uvindu.crane_management_system.dto;

import lombok.Data;

@Data
public class DashboardResponse {

    // Crane Summary
    private long totalCranes;
    private long availableCranes;
    private long inUseCranes;
    private long maintenanceCranes;

    // Job Summary
    private long pendingJobs;
    private long completedJobs;
    private long canceledJobs;

    // Invoice Summary
    private long totalInvoices;
    private long draftInvoices;
    private long completedInvoices;
    private long revisedInvoices;
    private long canceledInvoices;

}
