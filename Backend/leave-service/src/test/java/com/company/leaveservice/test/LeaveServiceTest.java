package com.company.leaveservice.test;

import com.company.leaveservice.client.AuthServiceClient;
import com.company.leaveservice.dto.*;
import com.company.leaveservice.entity.Holiday;
import com.company.leaveservice.entity.LeaveBalance;
import com.company.leaveservice.entity.LeaveRequest;
import com.company.leaveservice.event.EventPublisher;
import com.company.leaveservice.exception.InvalidOperationException;
import com.company.leaveservice.repository.HolidayRepository;
import com.company.leaveservice.repository.LeaveBalanceRepository;
import com.company.leaveservice.repository.LeaveRequestRepository;
import com.company.leaveservice.service.LeaveService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveService Unit Tests")
class LeaveServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private HolidayRepository holidayRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private EventPublisher eventPublisher;   // ✅ FIX: was missing in original

    @InjectMocks
    private LeaveService leaveService;

    // ─── Shared Fixtures ───────────────────────────────────────
    private LeaveRequest mockLeaveRequest;
    private LeaveBalance mockLeaveBalance;
    private LeaveRequestDto leaveRequestDto;
    private UserResponse mockUserResponse;

    @BeforeEach
    void setUp() {
        // April 1-2 2026 are Wednesday & Thursday (working days)
        mockLeaveRequest = LeaveRequest.builder()
                .id(1L)
                .userId(1L)
                .leaveType("CASUAL")
                .fromDate(LocalDate.of(2026, 4, 1))
                .toDate(LocalDate.of(2026, 4, 2))
                .totalDays(2.0)
                .reason("Personal work")
                .status("SUBMITTED")
                .createdAt(LocalDateTime.now())
                .build();

        mockLeaveBalance = LeaveBalance.builder()
                .id(1L)
                .userId(1L)
                .leaveType("CASUAL")
                .totalDays(12.0)
                .usedDays(0.0)
                .remainingDays(12.0)
                .year(2026)
                .build();

        leaveRequestDto = new LeaveRequestDto();
        leaveRequestDto.setLeaveType("CASUAL");
        leaveRequestDto.setFromDate(LocalDate.of(2026, 4, 1));
        leaveRequestDto.setToDate(LocalDate.of(2026, 4, 2));
        leaveRequestDto.setReason("Personal work");

        mockUserResponse = UserResponse.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // APPLY LEAVE
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("applyLeave()")
    class ApplyLeaveTests {

        @Test
        @DisplayName("Success — valid dates, balance available, no overlap")
        void applyLeave_Success() {
            when(leaveRequestRepository.findOverlappingLeave(anyLong(), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(holidayRepository.existsByHolidayDate(any())).thenReturn(false);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeAndYear(anyLong(), anyString(), anyInt()))
                    .thenReturn(Optional.of(mockLeaveBalance));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenReturn(mockLeaveRequest);
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);
            doNothing().when(eventPublisher).publishLeaveApplied(any());

            LeaveResponseDto response = leaveService.applyLeave(1L, leaveRequestDto);

            assertNotNull(response);
            assertEquals("SUBMITTED", response.getStatus());
            assertEquals("CASUAL", response.getLeaveType());
            assertEquals("John Doe", response.getEmployeeName());
            verify(leaveRequestRepository).save(any(LeaveRequest.class));
            verify(eventPublisher).publishLeaveApplied(any());
        }

        @Test
        @DisplayName("Fail — fromDate is after toDate")
        void applyLeave_FromDateAfterToDate() {
            leaveRequestDto.setFromDate(LocalDate.of(2026, 4, 5));
            leaveRequestDto.setToDate(LocalDate.of(2026, 4, 1));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.applyLeave(1L, leaveRequestDto));
            assertEquals("From date cannot be after to date", ex.getMessage());
            verifyNoInteractions(leaveRequestRepository);
        }

        @Test
        @DisplayName("Fail — fromDate is in the past")
        void applyLeave_PastDate() {
            leaveRequestDto.setFromDate(LocalDate.now().minusDays(3));
            leaveRequestDto.setToDate(LocalDate.now().minusDays(1));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.applyLeave(1L, leaveRequestDto));
            assertEquals("Cannot apply leave for past dates", ex.getMessage());
        }

        @Test
        @DisplayName("Fail — overlapping leave request exists")
        void applyLeave_OverlappingLeave() {
            when(leaveRequestRepository.findOverlappingLeave(anyLong(), any(), any()))
                    .thenReturn(List.of(mockLeaveRequest));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.applyLeave(1L, leaveRequestDto));
            assertTrue(ex.getMessage().contains("overlap"));
        }

        @Test
        @DisplayName("Fail — all selected dates are weekends")
        void applyLeave_WeekendDatesOnly() {
            // April 4-5 2026 = Saturday & Sunday
            leaveRequestDto.setFromDate(LocalDate.of(2026, 4, 4));
            leaveRequestDto.setToDate(LocalDate.of(2026, 4, 5));

            when(leaveRequestRepository.findOverlappingLeave(anyLong(), any(), any()))
                    .thenReturn(new ArrayList<>());
           // when(holidayRepository.existsByHolidayDate(any())).thenReturn(false);

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.applyLeave(1L, leaveRequestDto));
            assertTrue(ex.getMessage().contains("no working days"));
        }

        @Test
        @DisplayName("Fail — all selected dates are holidays")
        void applyLeave_HolidayDatesOnly() {
            // April 1-2 are weekdays but marked as holidays
            when(leaveRequestRepository.findOverlappingLeave(anyLong(), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(holidayRepository.existsByHolidayDate(any())).thenReturn(true);

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.applyLeave(1L, leaveRequestDto));
            assertTrue(ex.getMessage().contains("no working days"));
        }

        @Test
        @DisplayName("Fail — no leave balance record found for type/year")
        void applyLeave_NoBalanceRecord() {
            when(leaveRequestRepository.findOverlappingLeave(anyLong(), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(holidayRepository.existsByHolidayDate(any())).thenReturn(false);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeAndYear(anyLong(), anyString(), anyInt()))
                    .thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> leaveService.applyLeave(1L, leaveRequestDto));
            assertTrue(ex.getMessage().contains("No leave balance"));
        }

        @Test
        @DisplayName("Fail — insufficient leave balance")
        void applyLeave_InsufficientBalance() {
            mockLeaveBalance.setRemainingDays(0.5); // needs 2, has 0.5

            when(leaveRequestRepository.findOverlappingLeave(anyLong(), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(holidayRepository.existsByHolidayDate(any())).thenReturn(false);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeAndYear(anyLong(), anyString(), anyInt()))
                    .thenReturn(Optional.of(mockLeaveBalance));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.applyLeave(1L, leaveRequestDto));
            assertTrue(ex.getMessage().contains("Insufficient leave balance"));
        }

        @Test
        @DisplayName("Success — auth service down, response uses fallback name/email")
        void applyLeave_AuthServiceDown_UsesFallback() {
            when(leaveRequestRepository.findOverlappingLeave(anyLong(), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(holidayRepository.existsByHolidayDate(any())).thenReturn(false);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeAndYear(anyLong(), anyString(), anyInt()))
                    .thenReturn(Optional.of(mockLeaveBalance));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenReturn(mockLeaveRequest);
            // Auth service returns null for email (simulates fallback)
            when(authServiceClient.getUserById(anyLong())).thenReturn(
                    UserResponse.builder().id(1L).build());
            doNothing().when(eventPublisher).publishLeaveApplied(any());

            LeaveResponseDto response = leaveService.applyLeave(1L, leaveRequestDto);

            assertEquals("Name information not found", response.getEmployeeName());
            assertEquals("Email information not found", response.getEmployeeEmail());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CANCEL LEAVE
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cancelLeave()")
    class CancelLeaveTests {

        @Test
        @DisplayName("Success — submitted leave cancelled by owner")
        void cancelLeave_Success() {
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));
            when(leaveRequestRepository.save(any())).thenReturn(mockLeaveRequest);
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            LeaveResponseDto response = leaveService.cancelLeave(1L, 1L);

            assertNotNull(response);
            verify(leaveRequestRepository).save(argThat(lr -> "CANCELLED".equals(lr.getStatus())));
        }

        @Test
        @DisplayName("Fail — leave not found")
        void cancelLeave_NotFound() {
            when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> leaveService.cancelLeave(1L, 99L));
        }

        @Test
        @DisplayName("Fail — user tries to cancel someone else's leave")
        void cancelLeave_NotOwner() {
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.cancelLeave(2L, 1L)); // userId=2, owner=1
            assertEquals("You can only cancel your own leave", ex.getMessage());
        }

        @Test
        @DisplayName("Fail — cannot cancel already CANCELLED leave")
        void cancelLeave_AlreadyCancelled() {
            mockLeaveRequest.setStatus("CANCELLED");
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.cancelLeave(1L, 1L));
            assertTrue(ex.getMessage().contains("Cannot cancel a CANCELLED"));
        }

        @Test
        @DisplayName("Fail — cannot cancel already REJECTED leave")
        void cancelLeave_AlreadyRejected() {
            mockLeaveRequest.setStatus("REJECTED");
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.cancelLeave(1L, 1L));
            assertTrue(ex.getMessage().contains("Cannot cancel a REJECTED"));
        }

        @Test
        @DisplayName("Fail — cannot cancel APPROVED leave that has already started")
        void cancelLeave_PastApprovedLeave() {
            mockLeaveRequest.setStatus("APPROVED");
            mockLeaveRequest.setFromDate(LocalDate.now().minusDays(2));
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.cancelLeave(1L, 1L));
            assertTrue(ex.getMessage().contains("already started"));
        }

        @Test
        @DisplayName("Success — APPROVED leave cancelled before start date")
        void cancelLeave_ApprovedBeforeStart() {
            mockLeaveRequest.setStatus("APPROVED");
            mockLeaveRequest.setFromDate(LocalDate.now().plusDays(3)); // future
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));
            when(leaveRequestRepository.save(any())).thenReturn(mockLeaveRequest);
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            assertDoesNotThrow(() -> leaveService.cancelLeave(1L, 1L));
            verify(leaveRequestRepository).save(argThat(lr -> "CANCELLED".equals(lr.getStatus())));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REVIEW LEAVE
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("reviewLeave()")
    class ReviewLeaveTests {

        private LeaveReviewDto approveDto;
        private LeaveReviewDto rejectDto;

        @BeforeEach
        void setUp() {
            approveDto = new LeaveReviewDto();
            approveDto.setAction("APPROVED");
            approveDto.setComment("Looks good");

            rejectDto = new LeaveReviewDto();
            rejectDto.setAction("REJECTED");
            rejectDto.setComment("Not enough reason");
        }

        @Test
        @DisplayName("Success — approve leave and deduct balance")
        void reviewLeave_Approve() {
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));
            when(leaveRequestRepository.save(any())).thenReturn(mockLeaveRequest);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeAndYear(anyLong(), anyString(), anyInt()))
                    .thenReturn(Optional.of(mockLeaveBalance));
            when(leaveBalanceRepository.save(any())).thenReturn(mockLeaveBalance);
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            leaveService.reviewLeave(1L, 2L, approveDto);

            verify(leaveBalanceRepository).save(argThat(b ->
                    b.getUsedDays() == 2.0 && b.getRemainingDays() == 10.0));
        }

        @Test
        @DisplayName("Success — reject leave, balance NOT deducted")
        void reviewLeave_Reject() {
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));
            when(leaveRequestRepository.save(any())).thenReturn(mockLeaveRequest);
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            leaveService.reviewLeave(1L, 2L, rejectDto);

            verify(leaveBalanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Fail — leave not found")
        void reviewLeave_NotFound() {
            when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> leaveService.reviewLeave(99L, 2L, approveDto));
        }

        @Test
        @DisplayName("Fail — leave status is not SUBMITTED")
        void reviewLeave_NotSubmitted() {
            mockLeaveRequest.setStatus("APPROVED");
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.reviewLeave(1L, 2L, approveDto));
            assertTrue(ex.getMessage().contains("Only SUBMITTED"));
        }

        @Test
        @DisplayName("Fail — reject without providing a comment")
        void reviewLeave_RejectWithoutComment() {
            rejectDto.setComment(null);
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.reviewLeave(1L, 2L, rejectDto));
            assertEquals("Comment is mandatory when rejecting", ex.getMessage());
        }

        @Test
        @DisplayName("Fail — reject with blank comment")
        void reviewLeave_RejectWithBlankComment() {
            rejectDto.setComment("   ");
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.reviewLeave(1L, 2L, rejectDto));
            assertEquals("Comment is mandatory when rejecting", ex.getMessage());
        }

        @Test
        @DisplayName("Fail — balance record missing on approval")
        void reviewLeave_BalanceMissingOnApproval() {
            when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(mockLeaveRequest));
            when(leaveRequestRepository.save(any())).thenReturn(mockLeaveRequest);
            when(leaveBalanceRepository.findByUserIdAndLeaveTypeAndYear(anyLong(), anyString(), anyInt()))
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> leaveService.reviewLeave(1L, 2L, approveDto));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LEAVE HISTORY & BALANCE
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getMyLeaveHistory() & getMyBalances()")
    class QueryTests {

        @Test
        @DisplayName("getMyLeaveHistory — returns mapped DTOs")
        void getMyLeaveHistory_ReturnsList() {
            when(leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(mockLeaveRequest));
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            List<LeaveResponseDto> result = leaveService.getMyLeaveHistory(1L);

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getUserId());
        }

        @Test
        @DisplayName("getMyLeaveHistory — returns empty list when no requests")
        void getMyLeaveHistory_Empty() {
            when(leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of());

            List<LeaveResponseDto> result = leaveService.getMyLeaveHistory(1L);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("getMyBalances — returns balance DTOs for current year")
        void getMyBalances_ReturnsList() {
            when(leaveBalanceRepository.findByUserIdAndYear(eq(1L), anyInt()))
                    .thenReturn(List.of(mockLeaveBalance));

            List<LeaveBalanceDto> result = leaveService.getMyBalances(1L);

            assertEquals(1, result.size());
            assertEquals("CASUAL", result.get(0).getLeaveType());
            assertEquals(12.0, result.get(0).getTotalDays());
        }

        @Test
        @DisplayName("getPendingRequests — returns only SUBMITTED leaves")
        void getPendingRequests_ReturnsSubmitted() {
            when(leaveRequestRepository.findByStatus("SUBMITTED"))
                    .thenReturn(List.of(mockLeaveRequest));
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            List<LeaveResponseDto> result = leaveService.getPendingRequests();

            assertEquals(1, result.size());
            assertEquals("SUBMITTED", result.get(0).getStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HOLIDAYS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Holiday Operations")
    class HolidayTests {

        @Test
        @DisplayName("addHoliday — success")
        void addHoliday_Success() {
            HolidayDto dto = new HolidayDto();
            dto.setHolidayDate(LocalDate.of(2026, 8, 15));
            dto.setHolidayName("Independence Day");
            dto.setHolidayType("NATIONAL");

            Holiday holiday = Holiday.builder()
                    .id(1L)
                    .holidayDate(LocalDate.of(2026, 8, 15))
                    .holidayName("Independence Day")
                    .holidayType("NATIONAL")
                    .build();

            when(holidayRepository.existsByHolidayDate(any())).thenReturn(false);
            when(holidayRepository.save(any())).thenReturn(holiday);

            Holiday result = leaveService.addHoliday(dto);

            assertNotNull(result);
            assertEquals("Independence Day", result.getHolidayName());
            assertEquals("NATIONAL", result.getHolidayType());
        }

        @Test
        @DisplayName("addHoliday — fails for duplicate date")
        void addHoliday_DuplicateDate() {
            HolidayDto dto = new HolidayDto();
            dto.setHolidayDate(LocalDate.of(2026, 8, 15));
            dto.setHolidayName("Independence Day");

            when(holidayRepository.existsByHolidayDate(any())).thenReturn(true);

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> leaveService.addHoliday(dto));
            assertTrue(ex.getMessage().contains("Holiday already exists"));
        }

        @Test
        @DisplayName("getHolidays — returns holidays for given year")
        void getHolidays_ReturnsForYear() {
            Holiday h = Holiday.builder().id(1L)
                    .holidayDate(LocalDate.of(2026, 1, 26))
                    .holidayName("Republic Day").build();
            when(holidayRepository.findByYear(2026)).thenReturn(List.of(h));

            List<Holiday> result = leaveService.getHolidays(2026);

            assertEquals(1, result.size());
            assertEquals("Republic Day", result.get(0).getHolidayName());
        }

        @Test
        @DisplayName("getNextHoliday — returns name and date")
        void getNextHoliday_Found() {
            Holiday upcoming = Holiday.builder()
                    .holidayName("Diwali")
                    .holidayDate(LocalDate.of(2026, 10, 20))
                    .build();
            when(holidayRepository.findFirstByHolidayDateAfterOrderByHolidayDateAsc(any()))
                    .thenReturn(Optional.of(upcoming));

            String result = leaveService.getNextHoliday();

            assertTrue(result.contains("Diwali"));
            assertTrue(result.contains("2026-10-20"));
        }

        @Test
        @DisplayName("getNextHoliday — returns fallback when none upcoming")
        void getNextHoliday_None() {
            when(holidayRepository.findFirstByHolidayDateAfterOrderByHolidayDateAsc(any()))
                    .thenReturn(Optional.empty());

            assertEquals("No upcoming holidays", leaveService.getNextHoliday());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // INITIALIZE LEAVE BALANCES (RabbitMQ onboarding)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("initializeLeaveBalances()")
    class InitializeBalancesTests {

        @Test
        @DisplayName("Success — creates 4 default balances for new user")
        void initialize_CreatesDefaultBalances() {
            when(leaveBalanceRepository.findByUserIdAndYear(eq(1L), anyInt()))
                    .thenReturn(List.of());
            when(leaveBalanceRepository.saveAll(anyList())).thenReturn(List.of());

            leaveService.initializeLeaveBalances(1L);

            verify(leaveBalanceRepository).saveAll(argThat(list ->
                    ((List<?>) list).size() == 4));
        }

        @Test
        @DisplayName("Idempotent — skips if balances already exist")
        void initialize_Idempotent() {
            when(leaveBalanceRepository.findByUserIdAndYear(eq(1L), anyInt()))
                    .thenReturn(List.of(mockLeaveBalance));

            leaveService.initializeLeaveBalances(1L);

            verify(leaveBalanceRepository, never()).saveAll(anyList());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SOFT DELETE (User deleted event)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("softDeleteUserData()")
    class SoftDeleteTests {

        @Test
        @DisplayName("Calls cancel, soft-delete requests, and soft-delete balances")
        void softDelete_CallsAllThreeOperations() {
            leaveService.softDeleteUserData(1L);

            verify(leaveRequestRepository).cancelPendingLeaves(1L);
            verify(leaveRequestRepository).softDeleteUserRequests(1L);
            verify(leaveBalanceRepository).softDeleteUserBalances(1L);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONSUMPTION STATS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getConsumptionStats()")
    class ConsumptionStatsTests {

        @Test
        @DisplayName("Returns mapped type-to-count stats")
        void getConsumptionStats_ReturnsMappedData() {
            List<Object[]> raw = List.of(
                    new Object[]{"CASUAL", 5L},
                    new Object[]{"SICK", 3L}
            );
            when(leaveRequestRepository.countApprovedByType()).thenReturn(raw);

            Map<String, Long> stats = leaveService.getConsumptionStats();

            assertEquals(2, stats.size());
            assertEquals(5L, stats.get("CASUAL"));
            assertEquals(3L, stats.get("SICK"));
        }

        @Test
        @DisplayName("getCountByStatus — delegates to repository")
        void getCountByStatus_DelegatesToRepo() {
            when(leaveRequestRepository.countByStatus("SUBMITTED")).thenReturn(7L);

            assertEquals(7L, leaveService.getCountByStatus("SUBMITTED"));
        }
    }
}