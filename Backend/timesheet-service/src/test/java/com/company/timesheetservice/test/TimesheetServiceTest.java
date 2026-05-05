package com.company.timesheetservice.test;

import com.company.timesheetservice.client.AuthServiceClient;
import com.company.timesheetservice.dto.*;
import com.company.timesheetservice.entity.Project;
import com.company.timesheetservice.entity.Timesheet;
import com.company.timesheetservice.entity.TimesheetEntry;
import com.company.timesheetservice.event.EventPublisher;
import com.company.timesheetservice.event.TimesheetStatusEvent;
import com.company.timesheetservice.exception.InvalidOperationException;
import com.company.timesheetservice.exception.ResourceNotFoundException;
import com.company.timesheetservice.repository.ProjectRepository;
import com.company.timesheetservice.repository.TimesheetEntryRepository;
import com.company.timesheetservice.repository.TimesheetRepository;
import com.company.timesheetservice.service.TimesheetService;

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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimesheetService Unit Tests")
class TimesheetServiceTest {

    @Mock private TimesheetRepository timesheetRepository;
    @Mock private TimesheetEntryRepository entryRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks
    private TimesheetService timesheetService;

    // ─── Shared Fixtures ───────────────────────────────────────
    // Week of March 16 2026 (Monday) to March 22 (Sunday)
    private Project mockProject;
    private Timesheet mockTimesheet;
    private TimesheetEntry mockEntry;
    private TimesheetEntryRequest entryRequest;
    private UserResponse mockUserResponse;

    @BeforeEach
    void setUp() {
        mockProject = Project.builder()
                .id(1L)
                .projectCode("PROJ001")
                .projectName("Test Project")
                .isActive(true)
                .build();

        mockTimesheet = Timesheet.builder()
                .id(1L)
                .userId(1L)
                .weekStart(LocalDate.of(2026, 3, 16))
                .weekEnd(LocalDate.of(2026, 3, 22))
                .status("DRAFT")
                .totalHours(0.0)
                .build();

        mockEntry = TimesheetEntry.builder()
                .id(1L)
                .timesheet(mockTimesheet)
                .project(mockProject)
                .workDate(LocalDate.of(2026, 3, 17))
                .hoursLogged(8.0)
                .taskSummary("Working on feature")
                .createdAt(LocalDateTime.now())
                .build();

        entryRequest = new TimesheetEntryRequest();
        entryRequest.setProjectId(1L);
        entryRequest.setWorkDate(LocalDate.of(2026, 3, 17));
        entryRequest.setHoursLogged(8.0);
        entryRequest.setTaskSummary("Working on feature");

        mockUserResponse = UserResponse.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .role("EMPLOYEE")
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // LOG ENTRY
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("logEntry()")
    class LogEntryTests {

        @Test
        @DisplayName("Success — creates new timesheet and saves entry")
        void logEntry_Success_NewTimesheet() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
            // No existing timesheet → creates one
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.empty());
            when(timesheetRepository.save(any(Timesheet.class))).thenReturn(mockTimesheet);
            when(entryRepository.existsByTimesheetIdAndProjectIdAndWorkDate(anyLong(), anyLong(), any()))
                    .thenReturn(false);
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(new ArrayList<>());
            when(entryRepository.save(any(TimesheetEntry.class))).thenReturn(mockEntry);

            TimesheetEntryResponse response = timesheetService.logEntry(1L, entryRequest);

            assertNotNull(response);
            assertEquals(8.0, response.getHoursLogged());
            // save called once for new timesheet + once for updateTotalHours
            verify(timesheetRepository, times(2)).save(any(Timesheet.class));
            verify(entryRepository).save(any(TimesheetEntry.class));
        }

        @Test
        @DisplayName("Success — reuses existing DRAFT timesheet")
        void logEntry_Success_ExistingTimesheet() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));
            when(entryRepository.existsByTimesheetIdAndProjectIdAndWorkDate(anyLong(), anyLong(), any()))
                    .thenReturn(false);
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(new ArrayList<>());
            when(entryRepository.save(any(TimesheetEntry.class))).thenReturn(mockEntry);
            when(timesheetRepository.save(any())).thenReturn(mockTimesheet);

            TimesheetEntryResponse response = timesheetService.logEntry(1L, entryRequest);

            assertNotNull(response);
            assertEquals(1L, response.getProjectId());
        }

        @Test
        @DisplayName("Fail — future work date")
        void logEntry_FutureDate() {
            entryRequest.setWorkDate(LocalDate.now().plusDays(1));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> timesheetService.logEntry(1L, entryRequest));
            assertEquals("Cannot log hours for future dates", ex.getMessage());
            verifyNoInteractions(projectRepository);
        }

        @Test
        @DisplayName("Fail — project not found")
        void logEntry_ProjectNotFound() {
            when(projectRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> timesheetService.logEntry(1L, entryRequest));
        }

        @Test
        @DisplayName("Fail — project is inactive")
        void logEntry_InactiveProject() {
            mockProject.setIsActive(false);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> timesheetService.logEntry(1L, entryRequest));
            assertEquals("Project is inactive", ex.getMessage());
        }

        @Test
        @DisplayName("Fail — cannot modify a SUBMITTED timesheet")
        void logEntry_SubmittedTimesheet() {
            mockTimesheet.setStatus("SUBMITTED");
            when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> timesheetService.logEntry(1L, entryRequest));
            assertTrue(ex.getMessage().contains("Cannot modify a SUBMITTED"));
        }

        @Test
        @DisplayName("Fail — cannot modify an APPROVED timesheet")
        void logEntry_ApprovedTimesheet() {
            mockTimesheet.setStatus("APPROVED");
            when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> timesheetService.logEntry(1L, entryRequest));
            assertTrue(ex.getMessage().contains("Cannot modify a APPROVED"));
        }

        @Test
        @DisplayName("Fail — duplicate entry for same project and date")
        void logEntry_DuplicateEntry() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));
            when(entryRepository.existsByTimesheetIdAndProjectIdAndWorkDate(anyLong(), anyLong(), any()))
                    .thenReturn(true);

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> timesheetService.logEntry(1L, entryRequest));
            assertTrue(ex.getMessage().contains("Entry already exists"));
        }

        @Test
        @DisplayName("Fail — daily hours would exceed 12-hour limit")
        void logEntry_ExceedsDailyHoursLimit() {
            // 10 hours already logged for this date
            TimesheetEntry existingEntry = TimesheetEntry.builder()
                    .workDate(LocalDate.of(2026, 3, 17))
                    .hoursLogged(10.0)
                    .project(mockProject)
                    .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));
            when(entryRepository.existsByTimesheetIdAndProjectIdAndWorkDate(anyLong(), anyLong(), any()))
                    .thenReturn(false);
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(List.of(existingEntry));

            entryRequest.setHoursLogged(5.0); // 10 + 5 = 15 > 12

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> timesheetService.logEntry(1L, entryRequest));
            assertTrue(ex.getMessage().contains("exceed"));
        }

        @Test
        @DisplayName("Success — exactly at 12-hour daily limit is allowed")
        void logEntry_ExactlyAtDailyLimit() {
            // 4 hours already logged; adding 8 = exactly 12
            TimesheetEntry existingEntry = TimesheetEntry.builder()
                    .workDate(LocalDate.of(2026, 3, 17))
                    .hoursLogged(4.0)
                    .project(mockProject)
                    .build();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));
            when(entryRepository.existsByTimesheetIdAndProjectIdAndWorkDate(anyLong(), anyLong(), any()))
                    .thenReturn(false);
            when(entryRepository.findByTimesheetId(anyLong()))
                    .thenReturn(List.of(existingEntry))   // first call for daily check
                    .thenReturn(List.of(existingEntry));  // second call for updateTotalHours
            when(entryRepository.save(any())).thenReturn(mockEntry);
            when(timesheetRepository.save(any())).thenReturn(mockTimesheet);

            entryRequest.setHoursLogged(8.0); // 4 + 8 = 12 exactly
            assertDoesNotThrow(() -> timesheetService.logEntry(1L, entryRequest));
        }

        @Test
        @DisplayName("Success — auth service down, response uses fallback in entry mapping")
        void logEntry_AuthServiceDown_TaskSummaryFallback() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(mockProject));
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));
            when(entryRepository.existsByTimesheetIdAndProjectIdAndWorkDate(anyLong(), anyLong(), any()))
                    .thenReturn(false);
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(new ArrayList<>());

            // Entry with null taskSummary to test fallback
            TimesheetEntry entryNoSummary = TimesheetEntry.builder()
                    .id(2L).timesheet(mockTimesheet).project(mockProject)
                    .workDate(LocalDate.of(2026, 3, 17))
                    .hoursLogged(8.0).taskSummary(null).build();
            when(entryRepository.save(any())).thenReturn(entryNoSummary);
            when(timesheetRepository.save(any())).thenReturn(mockTimesheet);

            TimesheetEntryResponse response = timesheetService.logEntry(1L, entryRequest);
            assertEquals("No detailed summary provided", response.getTaskSummary());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET WEEKLY TIMESHEET
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getWeeklyTimesheet()")
    class GetWeeklyTimesheetTests {

        @Test
        @DisplayName("Success — returns timesheet with entries")
        void getWeeklyTimesheet_Found() {
            when(timesheetRepository.findByUserIdAndWeekStart(1L, LocalDate.of(2026, 3, 16)))
                    .thenReturn(Optional.of(mockTimesheet));
            when(entryRepository.findByTimesheetId(1L)).thenReturn(List.of(mockEntry));
            when(authServiceClient.getUserById(1L)).thenReturn(mockUserResponse);

            TimesheetResponse response = timesheetService.getWeeklyTimesheet(1L, LocalDate.of(2026, 3, 16));

            assertNotNull(response);
            assertEquals(1L, response.getUserId());
            assertEquals("John Doe", response.getEmployeeName());
            assertEquals(1, response.getEntries().size());
        }

        @Test
        @DisplayName("Fail — timesheet not found for given week")
        void getWeeklyTimesheet_NotFound() {
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.empty());

            LocalDate weekStart = LocalDate.of(2026, 3, 16);

            assertThrows(ResourceNotFoundException.class,
                    () -> timesheetService.getWeeklyTimesheet(1L, weekStart));
        }

        @Test
        @DisplayName("Success — auth service unavailable uses fallback name/email")
        void getWeeklyTimesheet_AuthDown_Fallback() {
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(List.of());
            when(authServiceClient.getUserById(anyLong()))
                    .thenThrow(new RuntimeException("Service unavailable"));

            TimesheetResponse response = timesheetService.getWeeklyTimesheet(1L, LocalDate.of(2026, 3, 16));

            assertEquals("Name information not found", response.getEmployeeName());
            assertEquals("Email information not found", response.getEmployeeEmail());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET ALL TIMESHEETS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllTimesheet()")
    class GetAllTimesheetsTests {

        @Test
        @DisplayName("Returns all timesheets ordered by weekStart desc")
        void getAllTimesheet_ReturnsList() {
            when(timesheetRepository.findByUserIdOrderByWeekStartDesc(1L))
                    .thenReturn(List.of(mockTimesheet));
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(List.of(mockEntry));
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            List<TimesheetResponse> result = timesheetService.getAllTimesheet(1L);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Returns empty list when no timesheets found")
        void getAllTimesheet_Empty() {
            when(timesheetRepository.findByUserIdOrderByWeekStartDesc(1L)).thenReturn(List.of());

            List<TimesheetResponse> result = timesheetService.getAllTimesheet(1L);
            assertTrue(result.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SUBMIT TIMESHEET
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("submitTimesheet()")
    class SubmitTimesheetTests {

        @Test
        @DisplayName("Success — DRAFT timesheet with entries is submitted")
        void submitTimesheet_Success() {
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(List.of(mockEntry));
            when(timesheetRepository.save(any())).thenReturn(mockTimesheet);
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);
            doNothing().when(eventPublisher).publishTimesheetSubmitted(any());

            TimesheetResponse response = timesheetService.submitTimesheet(1L, LocalDate.of(2026, 3, 16));

            assertNotNull(response);
            verify(timesheetRepository).save(argThat(t -> "SUBMITTED".equals(t.getStatus())));
            verify(eventPublisher).publishTimesheetSubmitted(any(TimesheetStatusEvent.class));
        }

        @Test
        @DisplayName("Fail — timesheet not found")
        void submitTimesheet_NotFound() {
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.empty());

            LocalDate weekStart = LocalDate.of(2026, 3, 16);

            assertThrows(ResourceNotFoundException.class,
                    () -> timesheetService.submitTimesheet(1L, weekStart));
        }

        @Test
        @DisplayName("Fail — already SUBMITTED timesheet cannot be submitted again")
        void submitTimesheet_AlreadySubmitted() {
            mockTimesheet.setStatus("SUBMITTED");
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));

            LocalDate weekStart = LocalDate.of(2026, 3, 16);

            InvalidOperationException ex = assertThrows(
                    InvalidOperationException.class,
                    () -> timesheetService.submitTimesheet(1L, weekStart)
            );
            assertTrue(ex.getMessage().contains("Only DRAFT timesheets can be submitted"));
        }

        @Test
        @DisplayName("Fail — APPROVED timesheet cannot be re-submitted")
        void submitTimesheet_AlreadyApproved() {
            mockTimesheet.setStatus("APPROVED");
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));

            LocalDate weekStart = LocalDate.of(2026, 3, 16);

            InvalidOperationException ex = assertThrows(
                    InvalidOperationException.class,
                    () -> timesheetService.submitTimesheet(1L, weekStart)
            );
            assertTrue(ex.getMessage().contains("Only DRAFT timesheets can be submitted"));
        }

        @Test
        @DisplayName("Fail — empty timesheet cannot be submitted")
        void submitTimesheet_EmptyTimesheet() {
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(new ArrayList<>());

            LocalDate weekStart = LocalDate.of(2026, 3, 16);

            InvalidOperationException ex = assertThrows(
                    InvalidOperationException.class,
                    () -> timesheetService.submitTimesheet(1L, weekStart)
            );
            assertEquals("Cannot submit empty timesheet", ex.getMessage());
        }

        @Test
        @DisplayName("Success — submittedAt timestamp is set on save")
        void submitTimesheet_SetsSubmittedAt() {
            when(timesheetRepository.findByUserIdAndWeekStart(anyLong(), any()))
                    .thenReturn(Optional.of(mockTimesheet));
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(List.of(mockEntry));
            when(timesheetRepository.save(any())).thenReturn(mockTimesheet);
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            timesheetService.submitTimesheet(1L, LocalDate.of(2026, 3, 16));

            verify(timesheetRepository).save(argThat(t -> t.getSubmittedAt() != null));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REVIEW TIMESHEET
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("reviewTimesheet()")
    class ReviewTimesheetTests {

        private ReviewRequest approveRequest;
        private ReviewRequest rejectRequest;

        @BeforeEach
        void setUp() {
            mockTimesheet.setStatus("SUBMITTED");

            approveRequest = new ReviewRequest();
            approveRequest.setAction("APPROVED");
            approveRequest.setComment("Good work!");

            rejectRequest = new ReviewRequest();
            rejectRequest.setAction("REJECTED");
            rejectRequest.setComment("Please fix issues");
        }

        @Test
        @DisplayName("Success — approve sets status to APPROVED")
        void reviewTimesheet_Approve() {
            when(timesheetRepository.findById(1L)).thenReturn(Optional.of(mockTimesheet));
            when(timesheetRepository.save(any())).thenReturn(mockTimesheet);
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(List.of(mockEntry));
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            timesheetService.reviewTimesheet(1L, 2L, approveRequest);

            verify(timesheetRepository).save(argThat(t -> "APPROVED".equals(t.getStatus())));
        }

        @Test
        @DisplayName("Success — reject reverts status to DRAFT (not REJECTED)")
        void reviewTimesheet_Reject_StatusSetToDraft() {
            when(timesheetRepository.findById(1L)).thenReturn(Optional.of(mockTimesheet));
            when(timesheetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(List.of(mockEntry));
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            timesheetService.reviewTimesheet(1L, 2L, rejectRequest);

            // The service sets REJECTED then overwrites with DRAFT — verify DRAFT is final
            verify(timesheetRepository).save(argThat(t -> "DRAFT".equals(t.getStatus())));
        }

        @Test
        @DisplayName("Success — managerId and comment are stored on review")
        void reviewTimesheet_StoresManagerIdAndComment() {
            when(timesheetRepository.findById(1L)).thenReturn(Optional.of(mockTimesheet));
            when(timesheetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(List.of(mockEntry));
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            timesheetService.reviewTimesheet(1L, 2L, approveRequest);

            verify(timesheetRepository).save(argThat(t ->
                    t.getReviewedBy().equals(2L) &&
                    "Good work!".equals(t.getReviewComment()) &&
                    t.getReviewedAt() != null));
        }

        @Test
        @DisplayName("Fail — timesheet not found")
        void reviewTimesheet_NotFound() {
            when(timesheetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> timesheetService.reviewTimesheet(99L, 2L, approveRequest));
        }

        @Test
        @DisplayName("Fail — only SUBMITTED timesheets can be reviewed")
        void reviewTimesheet_NotSubmitted() {
            mockTimesheet.setStatus("DRAFT");
            when(timesheetRepository.findById(1L)).thenReturn(Optional.of(mockTimesheet));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> timesheetService.reviewTimesheet(1L, 2L, approveRequest));
            assertTrue(ex.getMessage().contains("Only SUBMITTED timesheets"));
        }

        @Test
        @DisplayName("Fail — reject without comment throws exception")
        void reviewTimesheet_RejectWithoutComment() {
            rejectRequest.setComment(null);
            when(timesheetRepository.findById(1L)).thenReturn(Optional.of(mockTimesheet));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> timesheetService.reviewTimesheet(1L, 2L, rejectRequest));
            assertEquals("Comment is mandatory when rejecting", ex.getMessage());
        }

        @Test
        @DisplayName("Fail — reject with blank comment throws exception")
        void reviewTimesheet_RejectWithBlankComment() {
            rejectRequest.setComment("   ");
            when(timesheetRepository.findById(1L)).thenReturn(Optional.of(mockTimesheet));

            InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                    () -> timesheetService.reviewTimesheet(1L, 2L, rejectRequest));
            assertEquals("Comment is mandatory when rejecting", ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET ACTIVE PROJECTS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllActiveProjects()")
    class GetActiveProjectsTests {

        @Test
        @DisplayName("Returns mapped project responses for all active projects")
        void getAllActiveProjects_ReturnsList() {
            Project p2 = Project.builder().id(2L).projectCode("P2")
                    .projectName("Project Two").isActive(true).build();
            when(projectRepository.findByIsActiveTrue()).thenReturn(List.of(mockProject, p2));

            List<ProjectResponse> result = timesheetService.getAllActiveProjects();

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(ProjectResponse::getIsActive));
        }

        @Test
        @DisplayName("Returns empty list when no active projects")
        void getAllActiveProjects_Empty() {
            when(projectRepository.findByIsActiveTrue()).thenReturn(List.of());

            assertTrue(timesheetService.getAllActiveProjects().isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET PENDING TIMESHEETS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPendingTimesheets()")
    class GetPendingTimesheetsTests {

        @Test
        @DisplayName("Returns only SUBMITTED timesheets")
        void getPendingTimesheets_ReturnsList() {
            mockTimesheet.setStatus("SUBMITTED");
            when(timesheetRepository.findByStatus("SUBMITTED")).thenReturn(List.of(mockTimesheet));
            when(entryRepository.findByTimesheetId(anyLong())).thenReturn(List.of(mockEntry));
            when(authServiceClient.getUserById(anyLong())).thenReturn(mockUserResponse);

            List<TimesheetResponse> result = timesheetService.getPendingTimesheets();

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Returns empty when no pending timesheets")
        void getPendingTimesheets_Empty() {
            when(timesheetRepository.findByStatus("SUBMITTED")).thenReturn(List.of());

            assertTrue(timesheetService.getPendingTimesheets().isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SOFT DELETE
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("softDeleteUserData()")
    class SoftDeleteTests {

        @Test
        @DisplayName("Delegates to repository soft delete")
        void softDelete_CallsRepository() {
            timesheetService.softDeleteUserData(5L);
            verify(timesheetRepository).softDeleteUserTimesheets(5L);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL / REPORTING
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getSubmittedCount() & getCountByStatus()")
    class ReportingTests {

        @Test
        @DisplayName("getSubmittedCount — sums SUBMITTED + APPROVED for a week")
        void getSubmittedCount_SumsBothStatuses() {
            LocalDate week = LocalDate.of(2026, 3, 16);
            when(timesheetRepository.countByStatusAndWeekStart("SUBMITTED", week)).thenReturn(3L);
            when(timesheetRepository.countByStatusAndWeekStart("APPROVED", week)).thenReturn(2L);

            assertEquals(5L, timesheetService.getSubmittedCount(week));
        }

        @Test
        @DisplayName("getSubmittedCount — returns zero when none found")
        void getSubmittedCount_Zero() {
            LocalDate week = LocalDate.of(2026, 3, 16);
            when(timesheetRepository.countByStatusAndWeekStart(anyString(), any())).thenReturn(0L);

            assertEquals(0L, timesheetService.getSubmittedCount(week));
        }

        @Test
        @DisplayName("getCountByStatus — delegates to repository")
        void getCountByStatus_Delegates() {
            when(timesheetRepository.countByStatus("DRAFT")).thenReturn(7L);

            assertEquals(7L, timesheetService.getCountByStatus("DRAFT"));
        }
    }
}