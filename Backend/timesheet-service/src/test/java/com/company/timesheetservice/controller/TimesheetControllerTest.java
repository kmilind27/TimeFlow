package com.company.timesheetservice.controller;

import com.company.timesheetservice.dto.*;
import com.company.timesheetservice.exception.GlobalExceptionHandler;
import com.company.timesheetservice.exception.InvalidOperationException;
import com.company.timesheetservice.exception.ResourceNotFoundException;
import com.company.timesheetservice.service.TimesheetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimesheetController Unit Tests")
class TimesheetControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TimesheetService timesheetService;

    @InjectMocks
    private TimesheetController timesheetController;

    private TimesheetResponse mockTimesheetResponse;
    private TimesheetEntryResponse mockEntryResponse;

    @BeforeEach
    void setUp() {
        // ✅ FIX 1 — wire GlobalExceptionHandler so exceptions → correct HTTP status
        // Without this, InvalidOperationException → 500 (not 400)
        // and ResourceNotFoundException → 500 (not 404)
        mockMvc = MockMvcBuilders
                .standaloneSetup(timesheetController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockEntryResponse = TimesheetEntryResponse.builder()
                .id(1L)
                .projectId(1L)
                .projectName("Test Project")
                .workDate(LocalDate.of(2026, 3, 17))
                .hoursLogged(8.0)
                .taskSummary("Working on feature")
                .createdAt(LocalDateTime.now())
                .build();

        mockTimesheetResponse = TimesheetResponse.builder()
                .id(1L)
                .userId(1L)
                .employeeName("John Doe")
                .employeeEmail("john@example.com")
                .weekStart(LocalDate.of(2026, 3, 16))
                .weekEnd(LocalDate.of(2026, 3, 22))
                .status("DRAFT")
                .totalHours(8.0)
                .entries(List.of(mockEntryResponse))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // GET /timesheet/projects
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /timesheet/projects")
    class GetActiveProjectsEndpoint {

        @Test
        @DisplayName("200 — returns list of active projects")
        void getActiveProjects_Returns200() throws Exception {
            ProjectResponse p = ProjectResponse.builder()
                    .id(1L)
                    .projectCode("P001")
                    .projectName("Test Project")
                    .isActive(true)
                    .build();
            when(timesheetService.getAllActiveProjects()).thenReturn(List.of(p));

            mockMvc.perform(get("/timesheet/projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].projectCode").value("P001"))
                    .andExpect(jsonPath("$[0].isActive").value(true));
        }

        @Test
        @DisplayName("200 — returns empty list when no active projects")
        void getActiveProjects_EmptyList() throws Exception {
            when(timesheetService.getAllActiveProjects()).thenReturn(List.of());

            mockMvc.perform(get("/timesheet/projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("200 — returns multiple projects")
        void getActiveProjects_MultipleProjects() throws Exception {
            List<ProjectResponse> projects = List.of(
                    ProjectResponse.builder().id(1L).projectCode("P001")
                            .projectName("Alpha").isActive(true).build(),
                    ProjectResponse.builder().id(2L).projectCode("P002")
                            .projectName("Beta").isActive(true).build()
            );
            when(timesheetService.getAllActiveProjects()).thenReturn(projects);

            mockMvc.perform(get("/timesheet/projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[1].projectCode").value("P002"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // POST /timesheet/entries
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /timesheet/entries")
    class LogEntryEndpoint {

        @Test
        @DisplayName("201 — valid request logs entry successfully")
        void logEntry_Returns201() throws Exception {
            TimesheetEntryRequest request = new TimesheetEntryRequest();
            request.setProjectId(1L);
            request.setWorkDate(LocalDate.of(2026, 3, 17));
            request.setHoursLogged(8.0);
            request.setTaskSummary("Working on feature");

            when(timesheetService.logEntry(eq(1L), any()))
                    .thenReturn(mockEntryResponse);

            mockMvc.perform(post("/timesheet/entries")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.hoursLogged").value(8.0))
                    .andExpect(jsonPath("$.projectName").value("Test Project"))
                    .andExpect(jsonPath("$.workDate").value("2026-03-17"));
        }

        @Test
        @DisplayName("400 — missing projectId fails @Valid")
        void logEntry_MissingProjectId_Returns400() throws Exception {
            // ✅ TimesheetEntryRequest: projectId @NotNull, workDate @NotNull,
            //    hoursLogged @NotNull
            TimesheetEntryRequest request = new TimesheetEntryRequest();
            // projectId is null → @NotNull fails
            request.setWorkDate(LocalDate.of(2026, 3, 17));
            request.setHoursLogged(8.0);

            mockMvc.perform(post("/timesheet/entries")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(timesheetService);
        }

        @Test
        @DisplayName("400 — missing workDate fails @Valid")
        void logEntry_MissingWorkDate_Returns400() throws Exception {
            TimesheetEntryRequest request = new TimesheetEntryRequest();
            request.setProjectId(1L);
            // workDate is null → @NotNull fails
            request.setHoursLogged(8.0);

            mockMvc.perform(post("/timesheet/entries")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(timesheetService);
        }

        @Test
        @DisplayName("400 — hoursLogged below minimum fails @DecimalMin")
        void logEntry_HoursBelowMin_Returns400() throws Exception {
            TimesheetEntryRequest request = new TimesheetEntryRequest();
            request.setProjectId(1L);
            request.setWorkDate(LocalDate.of(2026, 3, 17));
            // ✅ @DecimalMin(0.5) — 0.1 fails
            request.setHoursLogged(0.1);

            mockMvc.perform(post("/timesheet/entries")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(timesheetService);
        }

        @Test
        @DisplayName("400 — all required fields missing returns 400")
        void logEntry_AllFieldsMissing_Returns400() throws Exception {
            mockMvc.perform(post("/timesheet/entries")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(timesheetService);
        }

        @Test
        @DisplayName("400 — future date throws InvalidOperationException → 400")
        void logEntry_FutureDate_Returns400() throws Exception {
            TimesheetEntryRequest request = new TimesheetEntryRequest();
            request.setProjectId(1L);
            request.setWorkDate(LocalDate.of(2026, 3, 17));
            request.setHoursLogged(8.0);
            request.setTaskSummary("task");

            // ✅ FIX 2 — GlobalExceptionHandler handles InvalidOperationException → 400
            when(timesheetService.logEntry(anyLong(), any()))
                    .thenThrow(new InvalidOperationException(
                            "Cannot log hours for future dates"));

            mockMvc.perform(post("/timesheet/entries")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — exceeds daily 12 hour limit → 400")
        void logEntry_ExceedsDailyLimit_Returns400() throws Exception {
            TimesheetEntryRequest request = new TimesheetEntryRequest();
            request.setProjectId(1L);
            request.setWorkDate(LocalDate.of(2026, 3, 17));
            request.setHoursLogged(8.0);
            request.setTaskSummary("task");

            when(timesheetService.logEntry(anyLong(), any()))
                    .thenThrow(new InvalidOperationException(
                            "Total hours for 2026-03-17 would exceed 12.0 hours"));

            mockMvc.perform(post("/timesheet/entries")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 — project not found → 404")
        void logEntry_ProjectNotFound_Returns404() throws Exception {
            TimesheetEntryRequest request = new TimesheetEntryRequest();
            request.setProjectId(99L);
            request.setWorkDate(LocalDate.of(2026, 3, 17));
            request.setHoursLogged(8.0);

            when(timesheetService.logEntry(anyLong(), any()))
                    .thenThrow(new ResourceNotFoundException("Project not found"));

            mockMvc.perform(post("/timesheet/entries")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("400 — duplicate entry → 400")
        void logEntry_DuplicateEntry_Returns400() throws Exception {
            TimesheetEntryRequest request = new TimesheetEntryRequest();
            request.setProjectId(1L);
            request.setWorkDate(LocalDate.of(2026, 3, 17));
            request.setHoursLogged(4.0);

            when(timesheetService.logEntry(anyLong(), any()))
                    .thenThrow(new InvalidOperationException(
                            "Entry already exists for this project and date"));

            mockMvc.perform(post("/timesheet/entries")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

   
    @Nested
    @DisplayName("GET /timesheet/weeks/{weekStart}")
    class GetWeeklyTimesheetEndpoint {

        @Test
        @DisplayName("200 — returns timesheet for given week")
        void getWeeklyTimesheet_Returns200() throws Exception {
            when(timesheetService.getWeeklyTimesheet(1L, LocalDate.of(2026, 3, 16)))
                    .thenReturn(mockTimesheetResponse);

            mockMvc.perform(get("/timesheet/weeks/2026-03-16")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeName").value("John Doe"))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.totalHours").value(8.0))
                    .andExpect(jsonPath("$.entries", hasSize(1)));
        }

        @Test
        @DisplayName("200 — returns timesheet with no entries")
        void getWeeklyTimesheet_NoEntries_Returns200() throws Exception {
            TimesheetResponse empty = TimesheetResponse.builder()
                    .id(1L).userId(1L).status("DRAFT")
                    .totalHours(0.0).entries(List.of()).build();

            when(timesheetService.getWeeklyTimesheet(1L, LocalDate.of(2026, 3, 16)))
                    .thenReturn(empty);

            mockMvc.perform(get("/timesheet/weeks/2026-03-16")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entries", hasSize(0)));
        }

        @Test
        @DisplayName("404 — timesheet not found for given week")
        void getWeeklyTimesheet_NotFound_Returns404() throws Exception {
            // ✅ FIX 2 — GlobalExceptionHandler wired → ResourceNotFoundException → 404
            when(timesheetService.getWeeklyTimesheet(anyLong(), any()))
                    .thenThrow(new ResourceNotFoundException(
                            "No timesheet found for week: 2026-03-16"));

            mockMvc.perform(get("/timesheet/weeks/2026-03-16")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET /timesheet/my-timesheets
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /timesheet/my-timesheets")
    class GetMyTimesheetsEndpoint {

        @Test
        @DisplayName("200 — returns all timesheets for the user")
        void getMyTimesheets_Returns200() throws Exception {
            when(timesheetService.getAllTimesheet(1L))
                    .thenReturn(List.of(mockTimesheetResponse));

            mockMvc.perform(get("/timesheet/my-timesheets")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].totalHours").value(8.0))
                    .andExpect(jsonPath("$[0].status").value("DRAFT"));
        }

        @Test
        @DisplayName("200 — returns empty list when no timesheets")
        void getMyTimesheets_Empty() throws Exception {
            when(timesheetService.getAllTimesheet(anyLong()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/timesheet/my-timesheets")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("200 — returns timesheets for different user id")
        void getMyTimesheets_DifferentUser() throws Exception {
            when(timesheetService.getAllTimesheet(5L))
                    .thenReturn(List.of(mockTimesheetResponse));

            mockMvc.perform(get("/timesheet/my-timesheets")
                            .header("X-User-Id", 5L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(timesheetService).getAllTimesheet(5L);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // POST /timesheet/weeks/{weekStart}/submit
    // ✅ FIX 3 — controller returns 200 (ResponseEntity.ok), not 201
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /timesheet/weeks/{weekStart}/submit")
    class SubmitTimesheetEndpoint {

        @Test
        @DisplayName("200 — submits timesheet successfully")
        void submitTimesheet_Returns200() throws Exception {
            TimesheetResponse submitted = TimesheetResponse.builder()
                    .id(1L)
                    .userId(1L)
                    // ✅ REJECTED sets status back to DRAFT in service
                    // APPROVED sets to "APPROVED"
                    // SUBMITTED sets to "SUBMITTED"
                    .status("SUBMITTED")
                    .build();

            when(timesheetService.submitTimesheet(1L, LocalDate.of(2026, 3, 16)))
                    .thenReturn(submitted);

            mockMvc.perform(post("/timesheet/weeks/2026-03-16/submit")
                            .header("X-User-Id", 1L))
                    // ✅ Controller uses ResponseEntity.ok → 200 NOT 201
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUBMITTED"));
        }

        @Test
        @DisplayName("404 — timesheet not found for week → 404")
        void submitTimesheet_TimesheetNotFound_Returns404() throws Exception {
            when(timesheetService.submitTimesheet(anyLong(), any()))
                    .thenThrow(new ResourceNotFoundException(
                            "No timesheet found for week: 2026-03-16"));

            mockMvc.perform(post("/timesheet/weeks/2026-03-16/submit")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isNotFound());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Cannot submit empty timesheet",
                "Only DRAFT timesheets can be submitted. Current status: SUBMITTED",
                "Only DRAFT timesheets can be submitted. Current status: APPROVED"
        })
        @DisplayName("400 — invalid timesheet submission scenarios")
        void submitTimesheet_InvalidCases_Returns400(String errorMessage) throws Exception {

            when(timesheetService.submitTimesheet(anyLong(), any()))
                    .thenThrow(new InvalidOperationException(errorMessage));

            mockMvc.perform(post("/timesheet/weeks/2026-03-16/submit")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isBadRequest());
        }
    }

    
    @Nested
    @DisplayName("GET /timesheet/manager/pending")
    class GetPendingTimesheetsEndpoint {

        @Test
        @DisplayName("200 — returns pending timesheets list")
        void getPendingTimesheets_Returns200() throws Exception {
            when(timesheetService.getPendingTimesheets())
                    .thenReturn(List.of(mockTimesheetResponse));

            // ✅ FIX 4 — Do NOT assert role-based 403 in standaloneSetup
            // @PreAuthorize is bypassed in unit tests — that's expected
            // Security integration tests cover role checks
            mockMvc.perform(get("/timesheet/manager/pending")
                            .header("X-User-Role", "MANAGER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status").value("DRAFT"));
        }

        @Test
        @DisplayName("200 — returns empty when no pending timesheets")
        void getPendingTimesheets_Empty() throws Exception {
            when(timesheetService.getPendingTimesheets()).thenReturn(List.of());

            mockMvc.perform(get("/timesheet/manager/pending")
                            .header("X-User-Role", "MANAGER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("200 — admin can also access pending timesheets")
        void getPendingTimesheets_AsAdmin_Returns200() throws Exception {
            when(timesheetService.getPendingTimesheets())
                    .thenReturn(List.of(mockTimesheetResponse));

            mockMvc.perform(get("/timesheet/manager/pending")
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /timesheet/manager/review/{timesheetId}")
    class ReviewTimesheetEndpoint {

        @Test
        @DisplayName("200 — approve timesheet")
        void reviewTimesheet_Approve_Returns200() throws Exception {
            ReviewRequest reviewRequest = new ReviewRequest();
            reviewRequest.setAction("APPROVED");
            reviewRequest.setComment("Good job");

            TimesheetResponse approved = TimesheetResponse.builder()
                    .id(1L)
                    .status("APPROVED")
                    .build();

            // ✅ Service call order: reviewTimesheet(timesheetId, managerId, request)
            when(timesheetService.reviewTimesheet(eq(1L), eq(2L), any()))
                    .thenReturn(approved);

            mockMvc.perform(put("/timesheet/manager/review/1")
                            .header("X-User-Id", 2L)
                            .header("X-User-Role", "MANAGER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));
        }

        @Test
        @DisplayName("200 — reject timesheet with comment")
        void reviewTimesheet_Reject_Returns200() throws Exception {
            ReviewRequest reviewRequest = new ReviewRequest();
            reviewRequest.setAction("REJECTED");
            reviewRequest.setComment("Missing details");

            // ✅ REJECTED → service sets status back to DRAFT
            TimesheetResponse rejected = TimesheetResponse.builder()
                    .id(1L)
                    .status("DRAFT")
                    .reviewComment("Missing details")
                    .build();

            when(timesheetService.reviewTimesheet(eq(1L), eq(2L), any()))
                    .thenReturn(rejected);

            mockMvc.perform(put("/timesheet/manager/review/1")
                            .header("X-User-Id", 2L)
                            .header("X-User-Role", "MANAGER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.reviewComment").value("Missing details"));
        }

        @Test
        @DisplayName("400 — missing action fails @Valid")
        void reviewTimesheet_MissingAction_Returns400() throws Exception {
            // ✅ ReviewRequest: action @NotBlank @Pattern(APPROVED|REJECTED)
            String body = "{ \"comment\": \"some comment\" }";

            mockMvc.perform(put("/timesheet/manager/review/1")
                            .header("X-User-Id", 2L)
                            .header("X-User-Role", "MANAGER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(timesheetService);
        }

        @Test
        @DisplayName("400 — invalid action value fails @Pattern")
        void reviewTimesheet_InvalidAction_Returns400() throws Exception {
            // ✅ @Pattern(APPROVED|REJECTED) — "PENDING" is invalid
            String body = "{ \"action\": \"PENDING\", \"comment\": \"ok\" }";

            mockMvc.perform(put("/timesheet/manager/review/1")
                            .header("X-User-Id", 2L)
                            .header("X-User-Role", "MANAGER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(timesheetService);
        }

        @Test
        @DisplayName("400 — reject without comment → InvalidOperationException → 400")
        void reviewTimesheet_RejectNoComment_Returns400() throws Exception {
            ReviewRequest reviewRequest = new ReviewRequest();
            reviewRequest.setAction("REJECTED");
            reviewRequest.setComment(null);

            // ✅ @Valid passes (comment has no constraint)
            // Service throws InvalidOperationException → 400
            when(timesheetService.reviewTimesheet(anyLong(), anyLong(), any()))
                    .thenThrow(new InvalidOperationException(
                            "Comment is mandatory when rejecting"));

            mockMvc.perform(put("/timesheet/manager/review/1")
                            .header("X-User-Id", 2L)
                            .header("X-User-Role", "MANAGER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — reviewing non-SUBMITTED timesheet → 400")
        void reviewTimesheet_NotSubmitted_Returns400() throws Exception {
            ReviewRequest reviewRequest = new ReviewRequest();
            reviewRequest.setAction("APPROVED");
            reviewRequest.setComment("ok");

            when(timesheetService.reviewTimesheet(anyLong(), anyLong(), any()))
                    .thenThrow(new InvalidOperationException(
                            "Only SUBMITTED timesheets can be reviewed"));

            mockMvc.perform(put("/timesheet/manager/review/1")
                            .header("X-User-Id", 2L)
                            .header("X-User-Role", "MANAGER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 — timesheet not found returns 404")
        void reviewTimesheet_NotFound_Returns404() throws Exception {
            ReviewRequest reviewRequest = new ReviewRequest();
            reviewRequest.setAction("APPROVED");
            reviewRequest.setComment("ok");

            when(timesheetService.reviewTimesheet(anyLong(), anyLong(), any()))
                    .thenThrow(new ResourceNotFoundException("Timesheet not found"));

            mockMvc.perform(put("/timesheet/manager/review/1")
                            .header("X-User-Id", 2L)
                            .header("X-User-Role", "MANAGER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Internal Endpoints
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Internal Endpoints")
    class InternalEndpoints {

        @Test
        @DisplayName("GET /internal/compliance — returns submitted count for week")
        void getSubmittedCount_Returns200() throws Exception {
            when(timesheetService.getSubmittedCount(LocalDate.of(2026, 3, 16)))
                    .thenReturn(5L);

            mockMvc.perform(get("/timesheet/internal/compliance")
                            .param("weekStart", "2026-03-16"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        }

        @Test
        @DisplayName("GET /internal/compliance — returns 0 when none submitted")
        void getSubmittedCount_Zero_Returns200() throws Exception {
            when(timesheetService.getSubmittedCount(LocalDate.of(2026, 3, 16)))
                    .thenReturn(0L);

            mockMvc.perform(get("/timesheet/internal/compliance")
                            .param("weekStart", "2026-03-16"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("0"));
        }

        @Test
        @DisplayName("GET /internal/count — returns count by SUBMITTED status")
        void getCountByStatus_Submitted_Returns200() throws Exception {
            when(timesheetService.getCountByStatus("SUBMITTED")).thenReturn(3L);

            mockMvc.perform(get("/timesheet/internal/count")
                            .param("status", "SUBMITTED"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("3"));
        }

        @Test
        @DisplayName("GET /internal/count — returns count by APPROVED status")
        void getCountByStatus_Approved_Returns200() throws Exception {
            when(timesheetService.getCountByStatus("APPROVED")).thenReturn(7L);

            mockMvc.perform(get("/timesheet/internal/count")
                            .param("status", "APPROVED"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("7"));
        }

        @Test
        @DisplayName("GET /internal/count — returns count by DRAFT status")
        void getCountByStatus_Draft_Returns200() throws Exception {
            when(timesheetService.getCountByStatus("DRAFT")).thenReturn(10L);

            mockMvc.perform(get("/timesheet/internal/count")
                            .param("status", "DRAFT"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("10"));
        }
    }
}