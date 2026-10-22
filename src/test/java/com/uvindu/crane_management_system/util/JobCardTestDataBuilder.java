package com.uvindu.crane_management_system.util;

import com.uvindu.crane_management_system.dto.CreateJobCardRequest;
import com.uvindu.crane_management_system.dto.UpdateJobCardRequest;
import com.uvindu.crane_management_system.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class JobCardTestDataBuilder {

    public static JobCard createJobCard(Long id, String jobCardNumber, JobCardStatus status) {
        JobCard jobCard = new JobCard();
        jobCard.setId(id);
        jobCard.setJobCardNumber(jobCardNumber);
        jobCard.setJobDate(LocalDate.now());
        jobCard.setStartDate(LocalDate.now());
        jobCard.setEndDate(LocalDate.now().plusDays(7));
        jobCard.setLocation("Colombo");
        jobCard.setDescription("Test job card");
        jobCard.setStatus(status);
        jobCard.setTotalWorkedHours(BigDecimal.ZERO);
        jobCard.setTotalBillableHours(BigDecimal.ZERO);
        return jobCard;
    }

    public static CreateJobCardRequest createJobCardRequest() {
        CreateJobCardRequest request = new CreateJobCardRequest();
        request.setCustomerId(1L);
        request.setQuotationId(1L);
        request.setCraneId(1L);
        request.setOperatorId(1L);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(7));
        request.setLocation("Colombo");
        request.setDescription("Test job card");
        return request;
    }

    public static UpdateJobCardRequest createUpdateJobCardRequest() {
        UpdateJobCardRequest request = new UpdateJobCardRequest();
        request.setQuotationId(2L);
        request.setCraneId(2L);
        request.setOperatorId(2L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(8));
        request.setLocation("Kandy");
        request.setDescription("Updated job card");
        return request;
    }
}
