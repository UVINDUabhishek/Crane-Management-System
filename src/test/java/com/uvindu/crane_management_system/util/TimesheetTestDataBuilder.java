package com.uvindu.crane_management_system.util;

import com.uvindu.crane_management_system.dto.CreateTimesheetRequest;
import com.uvindu.crane_management_system.dto.UpdateTimesheetRequest;
import com.uvindu.crane_management_system.model.JobCard;
import com.uvindu.crane_management_system.model.JobCardStatus;
import com.uvindu.crane_management_system.model.Timesheet;
import com.uvindu.crane_management_system.model.Customer;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TimesheetTestDataBuilder {

    public static Timesheet createTimesheet(Long id, String timesheetNumber, JobCard jobCard) {
        Timesheet timesheet = new Timesheet();
        timesheet.setId(id);
        timesheet.setTimesheetNumber(timesheetNumber);
        timesheet.setJobCard(jobCard);
        timesheet.setDateOfWork(LocalDate.now());
        timesheet.setWorkedHours(new BigDecimal("8"));
        timesheet.setDescription("Test timesheet");
        return timesheet;
    }

    public static CreateTimesheetRequest createTimesheetRequest(Long jobCardId) {
        CreateTimesheetRequest request = new CreateTimesheetRequest();
        request.setJobCardId(jobCardId);
        request.setDateOfWork(LocalDate.now());
        request.setWorkedHours(new BigDecimal("8"));
        request.setDescription("Test timesheet");
        return request;
    }

    public static UpdateTimesheetRequest createUpdateTimesheetRequest() {
        UpdateTimesheetRequest request = new UpdateTimesheetRequest();
        request.setDateOfWork(LocalDate.now().minusDays(1));
        request.setWorkedHours(new BigDecimal("10"));
        request.setDescription("Updated timesheet");
        return request;
    }

    public static JobCard createPendingJobCard(Long id) {
        JobCard jobCard = new JobCard();
        jobCard.setId(id);
        jobCard.setJobCardNumber("JC-0001");
        jobCard.setStatus(JobCardStatus.PENDING);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("Test Customer");

        jobCard.setCustomerId(customer);

        return jobCard;
    }

    public static JobCard createCanceledJobCard(Long id) {
        JobCard jobCard = new JobCard();
        jobCard.setId(id);
        jobCard.setJobCardNumber("JC-0001");
        jobCard.setStatus(JobCardStatus.CANCELED);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("Test Customer");

        jobCard.setCustomerId(customer);

        return jobCard;
    }

    public static JobCard createCompletedJobCard(Long id) {
        JobCard jobCard = new JobCard();
        jobCard.setId(id);
        jobCard.setJobCardNumber("JC-0001");
        jobCard.setStatus(JobCardStatus.COMPLETED);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("Test Customer");

        jobCard.setCustomerId(customer);

        return jobCard;
    }
}
