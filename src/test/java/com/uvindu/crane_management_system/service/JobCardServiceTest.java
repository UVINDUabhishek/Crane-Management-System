package com.uvindu.crane_management_system.service;

import com.uvindu.crane_management_system.BaseUnitTest;
import com.uvindu.crane_management_system.dto.CreateJobCardRequest;
import com.uvindu.crane_management_system.dto.UpdateJobCardRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.uvindu.crane_management_system.dto.InvoiceResponse;
import com.uvindu.crane_management_system.dto.JobCardResponse;
import com.uvindu.crane_management_system.model.*;
import com.uvindu.crane_management_system.repository.*;
import com.uvindu.crane_management_system.util.JobCardTestDataBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobCardServiceTest extends BaseUnitTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private JobCardRepository jobCardRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private CraneRepository craneRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private MaintenanceService maintenanceService;

    @Mock
    private CraneService craneService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private SchedulingService schedulingService;

    @InjectMocks
    private JobCardService jobCardService;

    private void mockAuthenticatedSalesUser() {

        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);

        when(authentication.getName()).thenReturn("sales@example.com");
    }

    // ============================================================
    // CREATE JOB CARD
    // ============================================================

    @Test
    void shouldCreateJobCardSuccessfully() {

        // Arrange
        mockAuthenticatedSalesUser();

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        Quotation quotation = new Quotation();
        quotation.setId(1L);
        quotation.setQuotationNumber("QT-0001");
        quotation.setCustomerId(customer);
        quotation.setType("001");
        quotation.setValidFrom(LocalDate.of(2020, 1, 1));
        quotation.setValidTo(LocalDate.of(2030, 12, 31));

        Crane crane = new Crane();
        crane.setId(1L);
        crane.setCraneNumber("CRMOBILE001");
        crane.setStatus(CraneStatus.AVAILABLE);
        crane.setType("001");

        Operator operator = new Operator();
        operator.setId(1L);
        operator.setOperatorName("John Doe");

        User salesPerson = new User();
        salesPerson.setId(1L);
        salesPerson.setUsername("salesuser");
        salesPerson.setEmail("sales@example.com");

        CreateJobCardRequest request = JobCardTestDataBuilder.createJobCardRequest();

        JobCard savedJobCard =
                JobCardTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        JobCardStatus.PENDING
                );

        savedJobCard.setCustomerId(customer);
        savedJobCard.setQuotationId(quotation);
        savedJobCard.setCraneId(crane);
        savedJobCard.setOperatorId(operator);
        savedJobCard.setSalesPersonId(salesPerson);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quotation));
        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(salesPerson));
        when(maintenanceService.isCraneUnderMaintenance(any(), any(), any())).thenReturn(false);
        when(jobCardRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(jobCardRepository.save(any(JobCard.class))).thenReturn(savedJobCard);

        // Act
        JobCardResponse response = jobCardService.createJobCard(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getJobCardNumber()).isEqualTo("JC-0001");
        assertThat(response.getCustomer()).isEqualTo("ABC Construction");
        assertThat(response.getStatus()).isEqualTo(JobCardStatus.PENDING);

        ArgumentCaptor<JobCard> captor = ArgumentCaptor.forClass(JobCard.class);

        verify(jobCardRepository).save(captor.capture());

        JobCard saved = captor.getValue();

        assertThat(saved.getCustomerId()).isEqualTo(customer);
        assertThat(saved.getQuotationId()).isEqualTo(quotation);
        assertThat(saved.getCraneId()).isEqualTo(crane);
        assertThat(saved.getOperatorId()).isEqualTo(operator);
        assertThat(saved.getSalesPersonId()).isEqualTo(salesPerson);
        assertThat(saved.getStatus()).isEqualTo(JobCardStatus.PENDING);
    }

    @Test
    void shouldThrowExceptionWhenCreatingJobCardWithNonExistentCustomer() {

        // Arrange
        CreateJobCardRequest request = JobCardTestDataBuilder.createJobCardRequest();
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobCardService.createJobCard(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Customer not found");

        verify(jobCardRepository, never())
                .save(any(JobCard.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingJobCardWithNonExistentQuotation() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        CreateJobCardRequest request = JobCardTestDataBuilder.createJobCardRequest();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        when(quotationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobCardService.createJobCard(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Quotation not found");

        verify(jobCardRepository, never()).save(any(JobCard.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingJobCardWithNonExistentCrane() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Quotation quotation = new Quotation();
        quotation.setId(1L);

        CreateJobCardRequest request = JobCardTestDataBuilder.createJobCardRequest();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quotation));
        when(craneRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobCardService.createJobCard(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Crane not found");

        verify(jobCardRepository, never()).save(any(JobCard.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingJobCardWithNonAvailableCrane() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Quotation quotation = new Quotation();
        quotation.setId(1L);

        Crane crane = new Crane();
        crane.setId(1L);
        crane.setStatus(CraneStatus.IN_USE);

        CreateJobCardRequest request = JobCardTestDataBuilder.createJobCardRequest();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quotation));
        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));

        // Act & Assert
        assertThatThrownBy(() -> jobCardService.createJobCard(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Crane is not available");

        verify(maintenanceService, never()).isCraneUnderMaintenance(any(), any(), any());
        verify(jobCardRepository, never()).save(any(JobCard.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingJobCardDuringCraneMaintenance() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Quotation quotation = new Quotation();
        quotation.setId(1L);

        Crane crane = new Crane();
        crane.setId(1L);
        crane.setStatus(CraneStatus.AVAILABLE);

        CreateJobCardRequest request = JobCardTestDataBuilder.createJobCardRequest();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quotation));
        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));
        when(maintenanceService.isCraneUnderMaintenance(any(), any(), any())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> jobCardService.createJobCard(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Crane is under maintenance during the selected period"
                );

        verify(jobCardRepository, never()).save(any(JobCard.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingJobCardWithNonExistentOperator() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Quotation quotation = new Quotation();
        quotation.setId(1L);

        Crane crane = new Crane();
        crane.setId(1L);
        crane.setStatus(CraneStatus.AVAILABLE);

        CreateJobCardRequest request = JobCardTestDataBuilder.createJobCardRequest();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(quotation));
        when(craneRepository.findById(1L)).thenReturn(Optional.of(crane));
        when(maintenanceService.isCraneUnderMaintenance(any(), any(), any())).thenReturn(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobCardService.createJobCard(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Operator not found");

        verify(jobCardRepository, never()).save(any(JobCard.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingJobCardWithNonExistentSalesPerson() {

        // Arrange
        mockAuthenticatedSalesUser();

        Customer customer = new Customer();
        customer.setId(1L);

        Quotation quotation = new Quotation();
        quotation.setId(1L);

        Crane crane = new Crane();
        crane.setId(1L);
        crane.setStatus(CraneStatus.AVAILABLE);

        Operator operator = new Operator();
        operator.setId(1L);

        CreateJobCardRequest request =
                JobCardTestDataBuilder.createJobCardRequest();

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(quotationRepository.findById(1L))
                .thenReturn(Optional.of(quotation));

        when(craneRepository.findById(1L))
                .thenReturn(Optional.of(crane));

        when(maintenanceService.isCraneUnderMaintenance(
                any(),
                any(),
                any()
        )).thenReturn(false);

        when(operatorRepository.findById(1L))
                .thenReturn(Optional.of(operator));

        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                jobCardService.createJobCard(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(jobCardRepository, never())
                .save(any(JobCard.class));
    }

    @Test
    void shouldThrowExceptionWhenEndDateIsBeforeStartDate() {

        CreateJobCardRequest request = JobCardTestDataBuilder.createJobCardRequest();
        request.setStartDate(LocalDate.now().plusDays(5));
        request.setEndDate(LocalDate.now());
        assertThatThrownBy(() -> jobCardService.createJobCard(request))
                .isInstanceOfSatisfying(com.uvindu.crane_management_system.exception.BusinessException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(schedulingService, customerRepository, craneRepository);

        verify(jobCardRepository, never()).save(any(JobCard.class));
    }

    // ============================================================
    // GET JOB CARDS
    // ============================================================

    @Test
    void shouldGetAllJobCards() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        Quotation quotation = new Quotation();
        quotation.setId(1L);
        quotation.setQuotationNumber("QT-0001");

        Crane crane = new Crane();
        crane.setId(1L);
        crane.setCraneNumber("CR012T01");

        User salesPerson = new User();
        salesPerson.setId(1L);
        salesPerson.setUsername("salesuser");

        JobCard jobCard1 =
                JobCardTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        JobCardStatus.PENDING
                );

        jobCard1.setCustomerId(customer);
        jobCard1.setQuotationId(quotation);
        jobCard1.setCraneId(crane);
        jobCard1.setSalesPersonId(salesPerson);

        JobCard jobCard2 =
                JobCardTestDataBuilder.createJobCard(
                        2L,
                        "JC-0002",
                        JobCardStatus.COMPLETED
                );

        jobCard2.setCustomerId(customer);
        jobCard2.setQuotationId(quotation);
        jobCard2.setCraneId(crane);
        jobCard2.setSalesPersonId(salesPerson);

        when(jobCardRepository.findAll())
                .thenReturn(Arrays.asList(jobCard1, jobCard2));

        // Act
        List<JobCardResponse> responses =
                jobCardService.getAllJobCards();

        // Assert
        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).getJobCardNumber())
                .isEqualTo("JC-0001");

        assertThat(responses.get(1).getJobCardNumber())
                .isEqualTo("JC-0002");

        verify(jobCardRepository).findAll();
    }

    @Test
    void shouldGetJobCardById() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        Quotation quotation = new Quotation();
        quotation.setId(1L);
        quotation.setQuotationNumber("QT-0001");

        Crane crane = new Crane();
        crane.setId(1L);
        crane.setCraneNumber("CR012T01");

        User salesPerson = new User();
        salesPerson.setId(1L);
        salesPerson.setUsername("salesuser");

        JobCard jobCard =
                JobCardTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        JobCardStatus.PENDING
                );

        jobCard.setCustomerId(customer);
        jobCard.setQuotationId(quotation);
        jobCard.setCraneId(crane);
        jobCard.setSalesPersonId(salesPerson);

        when(jobCardRepository.findById(1L))
                .thenReturn(Optional.of(jobCard));

        // Act
        JobCardResponse response =
                jobCardService.getJobCardById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getJobCardNumber())
                .isEqualTo("JC-0001");
        assertThat(response.getCustomer())
                .isEqualTo("ABC Construction");
        assertThat(response.getQuotation())
                .isEqualTo("QT-0001");
        assertThat(response.getCrane())
                .isEqualTo("CR012T01");
        assertThat(response.getSalesPerson())
                .isEqualTo("salesuser");

        verify(jobCardRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenGettingNonExistentJobCard() {

        // Arrange
        when(jobCardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jobCardService.getJobCardById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Job Card not found");
    }

    // ============================================================
    // COMPLETE JOB CARD
    // ============================================================

    @Test
    void shouldCompleteJobCardSuccessfullyAndCreateInvoice() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        Quotation quotation = new Quotation();
        quotation.setId(1L);
        quotation.setQuotationNumber("QT-0001");
        quotation.setMinHours(new BigDecimal("6"));

        Crane crane = new Crane();
        crane.setId(1L);
        crane.setCraneNumber("CRMOBILE001");
        crane.setStatus(CraneStatus.IN_USE);

        User salesPerson = new User();
        salesPerson.setId(1L);
        salesPerson.setUsername("salesuser");

        JobCard jobCard =
                JobCardTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        JobCardStatus.PENDING
                );

        jobCard.setCustomerId(customer);
        jobCard.setQuotationId(quotation);
        jobCard.setCraneId(crane);
        jobCard.setSalesPersonId(salesPerson);

        Timesheet timesheet1 = new Timesheet();
        timesheet1.setJobCard(jobCard);
        timesheet1.setWorkedHours(new BigDecimal("4"));

        Timesheet timesheet2 = new Timesheet();
        timesheet2.setJobCard(jobCard);
        timesheet2.setWorkedHours(new BigDecimal("8"));

        InvoiceResponse invoiceResponse = new InvoiceResponse();

        when(jobCardRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(jobCard));

        when(timesheetRepository.findByJobCard(jobCard))
                .thenReturn(Arrays.asList(timesheet1, timesheet2));

        when(jobCardRepository.save(any(JobCard.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        doNothing()
                .when(craneService)
                .markAsAvailable(1L);

        when(invoiceService.createInvoiceFromJobCard(any(JobCard.class)))
                .thenReturn(invoiceResponse);

        // Act
        JobCardResponse response =
                jobCardService.completeJobCard(1L);

        // Assert
        assertThat(response).isNotNull();

        assertThat(response.getStatus())
                .isEqualTo(JobCardStatus.COMPLETED);

        // 4 + 8 = 12 actual worked hours
        assertThat(response.getTotalWorkedHours())
                .isEqualByComparingTo(new BigDecimal("12"));

        // max(4, 6) + max(8, 6)
        // = 6 + 8
        // = 14 billable hours
        assertThat(response.getTotalBillableHours())
                .isEqualByComparingTo(new BigDecimal("14"));

        assertThat(jobCard.getStatus())
                .isEqualTo(JobCardStatus.COMPLETED);

        assertThat(jobCard.getTotalWorkedHours())
                .isEqualByComparingTo(new BigDecimal("12"));

        assertThat(jobCard.getTotalBillableHours())
                .isEqualByComparingTo(new BigDecimal("14"));

        verify(craneService)
                .markAsAvailable(1L);

        verify(jobCardRepository)
                .save(jobCard);

        verify(invoiceService)
                .createInvoiceFromJobCard(jobCard);
    }


    @Test
    void shouldThrowExceptionWhenCompletingNonExistentJobCard() {

        // Arrange
        when(jobCardRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                jobCardService.completeJobCard(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Job card not found");

        verify(timesheetRepository, never())
                .findByJobCard(any(JobCard.class));

        verify(invoiceService, never())
                .createInvoiceFromJobCard(any(JobCard.class));
    }


    @Test
    void shouldThrowExceptionWhenCompletingAlreadyCompletedJobCard() {

        // Arrange
        JobCard jobCard =
                JobCardTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        JobCardStatus.COMPLETED
                );

        when(jobCardRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(jobCard));

        // Act & Assert
        assertThatThrownBy(() ->
                jobCardService.completeJobCard(1L))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> {
                            assertThat(exception.getStatusCode())
                                    .isEqualTo(HttpStatus.CONFLICT);

                            assertThat(exception.getReason())
                                    .isEqualTo("Only pending job cards can be completed");
                        }
                );

        verify(timesheetRepository, never())
                .findByJobCard(any(JobCard.class));

        verify(invoiceService, never())
                .createInvoiceFromJobCard(any(JobCard.class));
    }


    @Test
    void shouldThrowExceptionWhenCompletingCancelledJobCard() {

        // Arrange
        JobCard jobCard =
                JobCardTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        JobCardStatus.CANCELED
                );

        when(jobCardRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(jobCard));

        // Act & Assert
        assertThatThrownBy(() ->
                jobCardService.completeJobCard(1L))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> {
                            assertThat(exception.getStatusCode())
                                    .isEqualTo(HttpStatus.CONFLICT);

                            assertThat(exception.getReason())
                                    .isEqualTo("Only pending job cards can be completed");
                        }
                );

        verify(timesheetRepository, never())
                .findByJobCard(any(JobCard.class));

        verify(invoiceService, never())
                .createInvoiceFromJobCard(any(JobCard.class));
    }


    @Test
    void shouldThrowExceptionWhenCompletingJobCardWithoutTimesheets() {

        // Arrange
        JobCard jobCard =
                JobCardTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        JobCardStatus.PENDING
                );

        when(jobCardRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(jobCard));

        when(timesheetRepository.findByJobCard(jobCard))
                .thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() ->
                jobCardService.completeJobCard(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Job card cannot be completed without timesheets"
                );

        verify(jobCardRepository, never())
                .save(any(JobCard.class));

        verify(craneService, never())
                .markAsAvailable(anyLong());

        verify(invoiceService, never())
                .createInvoiceFromJobCard(any(JobCard.class));
    }


    @Test
    void shouldThrowExceptionWhenQuotationMinimumHoursAreNotDefined() {

        // Arrange
        Quotation quotation = new Quotation();
        quotation.setId(1L);
        quotation.setQuotationNumber("QT-0001");
        quotation.setMinHours(null);

        JobCard jobCard =
                JobCardTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        JobCardStatus.PENDING
                );

        jobCard.setQuotationId(quotation);

        Timesheet timesheet = new Timesheet();
        timesheet.setJobCard(jobCard);
        timesheet.setWorkedHours(new BigDecimal("8"));

        when(jobCardRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(jobCard));

        when(timesheetRepository.findByJobCard(jobCard))
                .thenReturn(List.of(timesheet));

        // Act & Assert
        assertThatThrownBy(() ->
                jobCardService.completeJobCard(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Minimum hours are not defined in the quotation"
                );

        verify(jobCardRepository, never())
                .save(any(JobCard.class));

        verify(craneService, never())
                .markAsAvailable(anyLong());

        verify(invoiceService, never())
                .createInvoiceFromJobCard(any(JobCard.class));
    }

    // ============================================================
    // CANCEL JOB CARD
    // ============================================================

    @Test
    void shouldCancelJobCard() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCompanyName("ABC Construction");

        Quotation quotation = new Quotation();
        quotation.setId(1L);
        quotation.setQuotationNumber("QT-0001");

        Crane crane = new Crane();
        crane.setId(1L);
        crane.setCraneNumber("CR012T01");

        User salesPerson = new User();
        salesPerson.setId(1L);
        salesPerson.setUsername("salesuser");

        JobCard jobCard =
                JobCardTestDataBuilder.createJobCard(
                        1L,
                        "JC-0001",
                        JobCardStatus.PENDING
                );

        jobCard.setCustomerId(customer);
        jobCard.setQuotationId(quotation);
        jobCard.setCraneId(crane);
        jobCard.setSalesPersonId(salesPerson);
        jobCard.setCraneId(crane);

        when(jobCardRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(jobCard));

        when(jobCardRepository.save(any(JobCard.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        doNothing()
                .when(craneService)
                .markAsAvailable(1L);

        // Act
        JobCardResponse response =
                jobCardService.cancelJobCard(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus())
                .isEqualTo(JobCardStatus.CANCELED);

        assertThat(jobCard.getStatus())
                .isEqualTo(JobCardStatus.CANCELED);

        verify(craneService)
                .markAsAvailable(1L);

        verify(jobCardRepository)
                .save(jobCard);

        verify(invoiceService, never())
                .createInvoiceFromJobCard(any(JobCard.class));
    }

    @Test
    void shouldThrowExceptionWhenCancellingNonExistentJobCard() {

        // Arrange
        when(jobCardRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                jobCardService.cancelJobCard(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Job card not found");

        verify(jobCardRepository, never())
                .save(any(JobCard.class));

        verify(craneService, never())
                .markAsAvailable(anyLong());
    }

    @Test
    void shouldThrowExceptionWhenCancellingCompletedJobCard() {

        // Arrange
        JobCard jobCard = JobCardTestDataBuilder.createJobCard(1L, "JC-0001", JobCardStatus.COMPLETED);
        when(jobCardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(jobCard));

        // Act & Assert
        assertThatThrownBy(() -> jobCardService.cancelJobCard(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Completed job card cannot be cancelled");

        verify(jobCardRepository, never())
                .save(any(JobCard.class));

        verify(craneService, never())
                .markAsAvailable(anyLong());
    }

    @Test
    void shouldThrowExceptionWhenCancellingAlreadyCancelledJobCard() {

        // Arrange
        JobCard jobCard = JobCardTestDataBuilder.createJobCard(1L, "JC-0001", JobCardStatus.CANCELED);
        when(jobCardRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(jobCard));

        // Act & Assert
        assertThatThrownBy(() -> jobCardService.cancelJobCard(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Job card is already cancelled");

        verify(jobCardRepository, never())
                .save(any(JobCard.class));

        verify(craneService, never())
                .markAsAvailable(anyLong());
    }

    @ParameterizedTest
    @EnumSource(JobCardStatus.class)
    void shouldReassignValidatedCraneOnlyForPendingJobs(JobCardStatus status) {
        JobCard job = updateFixture(status);
        Crane original = job.getCraneId();
        Crane replacement = updateCrane(2L, CraneStatus.AVAILABLE);
        UpdateJobCardRequest request = updateRequest(job, replacement.getId());
        when(jobCardRepository.findByIdForUpdate(job.getId())).thenReturn(Optional.of(job));
        if (status != JobCardStatus.PENDING) {
            assertThatThrownBy(() -> jobCardService.updateJobCard(job.getId(), request))
                    .hasMessage("Only pending job cards can be updated");
            verify(jobCardRepository, never()).save(any());
            return;
        }
        when(craneRepository.findById(replacement.getId())).thenReturn(Optional.of(replacement));
        when(jobCardRepository.save(any(JobCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobCardResponse response = jobCardService.updateJobCard(job.getId(), request);

        Crane expected = status == JobCardStatus.PENDING ? replacement : original;
        ArgumentCaptor<JobCard> saved = ArgumentCaptor.forClass(JobCard.class);
        verify(jobCardRepository).save(saved.capture());
        assertThat(saved.getValue().getCraneId()).isSameAs(expected);
        assertThat(response.getCraneId()).isEqualTo(expected.getId());
        assertThat(response.getCrane()).isEqualTo(expected.getCraneNumber());
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getLocation()).isEqualTo("Updated location");
        verify(maintenanceService).isCraneUnderMaintenance(replacement.getId(), request.getStartDate(), request.getEndDate());
        verifyNoInteractions(craneService, invoiceService);
    }

    @ParameterizedTest
    @EnumSource(value = CraneStatus.class, names = {"IN_USE", "MAINTENANCE"})
    void shouldNotReassignOrSaveWhenRequestedCraneIsUnavailable(CraneStatus status) {
        JobCard job = updateFixture(JobCardStatus.PENDING);
        Crane original = job.getCraneId();
        Crane replacement = updateCrane(2L, status);
        when(jobCardRepository.findByIdForUpdate(job.getId())).thenReturn(Optional.of(job));
        when(craneRepository.findById(replacement.getId())).thenReturn(Optional.of(replacement));

        assertThatThrownBy(() -> jobCardService.updateJobCard(job.getId(), updateRequest(job, replacement.getId())))
                .hasMessage("Crane is not available");
        assertThat(job.getCraneId()).isSameAs(original);
        verify(jobCardRepository, never()).save(any());
        verifyNoInteractions(maintenanceService);
    }

    @Test
    void shouldNotReassignOrSaveWhenRequestedCraneHasMaintenanceConflict() {
        JobCard job = updateFixture(JobCardStatus.PENDING);
        Crane original = job.getCraneId();
        Crane replacement = updateCrane(2L, CraneStatus.AVAILABLE);
        UpdateJobCardRequest request = updateRequest(job, replacement.getId());
        when(jobCardRepository.findByIdForUpdate(job.getId())).thenReturn(Optional.of(job));
        when(craneRepository.findById(replacement.getId())).thenReturn(Optional.of(replacement));
        when(maintenanceService.isCraneUnderMaintenance(replacement.getId(), request.getStartDate(), request.getEndDate()))
                .thenReturn(true);

        assertThatThrownBy(() -> jobCardService.updateJobCard(job.getId(), request))
                .hasMessage("Crane is under maintenance during the selected period");
        assertThat(job.getCraneId()).isSameAs(original);
        verify(jobCardRepository, never()).save(any());
    }

    private JobCard updateFixture(JobCardStatus status) {
        JobCard job = JobCardTestDataBuilder.createJobCard(1L, "JC-0001", status);
        Customer customer = new Customer();
        customer.setId(1L);
        job.setCustomerId(customer);
        Quotation quote = new Quotation();
        quote.setCustomerId(customer);
        quote.setType("001");
        quote.setValidFrom(LocalDate.of(2020, 1, 1));
        quote.setValidTo(LocalDate.of(2030, 12, 31));
        job.setQuotationId(quote);
        job.setSalesPersonId(new User());
        job.setCraneId(updateCrane(1L, CraneStatus.AVAILABLE));
        return job;
    }

    private Crane updateCrane(Long id, CraneStatus status) {
        Crane crane = new Crane();
        crane.setId(id);
        crane.setCraneNumber("CR-" + id);
        crane.setType("001");
        crane.setStatus(status);
        return crane;
    }

    private UpdateJobCardRequest updateRequest(JobCard job, Long craneId) {
        UpdateJobCardRequest request = new UpdateJobCardRequest();
        request.setCraneId(craneId);
        request.setStartDate(job.getStartDate());
        request.setEndDate(job.getEndDate());
        request.setLocation("Updated location");
        return request;
    }
}
