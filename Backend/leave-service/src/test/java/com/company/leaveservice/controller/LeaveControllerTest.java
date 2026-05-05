package com.company.leaveservice.controller;

import com.company.leaveservice.dto.*;
import com.company.leaveservice.entity.Holiday;
import com.company.leaveservice.exception.InvalidOperationException;
import com.company.leaveservice.service.LeaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveController Unit Tests")
class LeaveControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private LeaveService leaveService;
    @InjectMocks private LeaveController leaveController;

    // ─── Shared Fixtures ───────────────────────────────────────
    private LeaveResponseDto mockLeaveResponse;
    private LeaveBalanceDto mockBalanceDto;

    @BeforeEach
    void setUp() {
        // Standalone MockMvc — no Spring context, ultra-fast
        mockMvc = MockMvcBuilders
                .standaloneSetup(leaveController)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockLeaveResponse = LeaveResponseDto.builder()
                .id(1L)
                .userId(1L)
                .employeeName("John Doe")
                .employeeEmail("john@example.com")
                .leaveType("CASUAL")
                .fromDate(LocalDate.of(2026, 4, 1))
                .toDate(LocalDate.of(2026, 4, 2))
                .totalDays(2.0)
                .reason("Personal work")
                .status("SUBMITTED")
                .createdAt(LocalDateTime.now())
                .build();

        mockBalanceDto = LeaveBalanceDto.builder()
                .id(1L)
                .leaveType("CASUAL")
                .totalDays(12.0)
                .usedDays(2.0)
                .remainingDays(10.0)
                .year(2026)
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // POST /leave/apply
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /leave/apply")
    class ApplyLeaveEndpoint {

        @Test
        @DisplayName("201 — valid request applies leave successfully")
        void applyLeave_Returns201() throws Exception {
            LeaveRequestDto requestDto = new LeaveRequestDto();
            requestDto.setLeaveType("CASUAL");
            requestDto.setFromDate(LocalDate.of(2026, 4, 1));
            requestDto.setToDate(LocalDate.of(2026, 4, 2));
            requestDto.setReason("Personal work");

            when(leaveService.applyLeave(eq(1L), any()))
                    .thenReturn(mockLeaveResponse);

            mockMvc.perform(post("/leave/apply")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUBMITTED"))
                    .andExpect(jsonPath("$.leaveType").value("CASUAL"))
                    .andExpect(jsonPath("$.employeeName").value("John Doe"));

            verify(leaveService).applyLeave(eq(1L), any());
        }

        @Test
        @DisplayName("400 — missing leaveType fails validation")
        void applyLeave_MissingLeaveType_Returns400() throws Exception {
            // leaveType deliberately omitted
            String body = """
                    {
                      "fromDate": "2026-04-01",
                      "toDate": "2026-04-02",
                      "reason": "Personal work"
                    }
                    """;

            mockMvc.perform(post("/leave/apply")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(leaveService);
        }

        @Test
        @DisplayName("400 — invalid leaveType pattern fails validation")
        void applyLeave_InvalidLeaveType_Returns400() throws Exception {
            String body = """
                    {
                      "leaveType": "UNKNOWN",
                      "fromDate": "2026-04-01",
                      "toDate": "2026-04-02",
                      "reason": "test"
                    }
                    """;

            mockMvc.perform(post("/leave/apply")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — missing reason fails validation")
        void applyLeave_MissingReason_Returns400() throws Exception {
            String body = """
                    {
                      "leaveType": "CASUAL",
                      "fromDate": "2026-04-01",
                      "toDate": "2026-04-02"
                    }
                    """;

            mockMvc.perform(post("/leave/apply")
                            .header("X-User-Id", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET /leave/my-requests
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /leave/my-requests")
    class MyRequestsEndpoint {

        @Test
        @DisplayName("200 — returns leave history list")
        void getMyRequests_Returns200() throws Exception {
            when(leaveService.getMyLeaveHistory(1L)).thenReturn(List.of(mockLeaveResponse));

            mockMvc.perform(get("/leave/my-requests")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].leaveType").value("CASUAL"));
        }

        @Test
        @DisplayName("200 — returns empty list when no history")
        void getMyRequests_EmptyList() throws Exception {
            when(leaveService.getMyLeaveHistory(1L)).thenReturn(List.of());

            mockMvc.perform(get("/leave/my-requests")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET /leave/my-balance
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /leave/my-balance")
    class MyBalanceEndpoint {

        @Test
        @DisplayName("200 — returns balance list")
        void getMyBalance_Returns200() throws Exception {
            when(leaveService.getMyBalances(1L)).thenReturn(List.of(mockBalanceDto));

            mockMvc.perform(get("/leave/my-balance")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].leaveType").value("CASUAL"))
                    .andExpect(jsonPath("$[0].remainingDays").value(10.0));
        }
    }

   
    @Nested
    @DisplayName("PUT /leave/cancel/{leaveId}")
    class CancelLeaveEndpoint {

        @Test
        @DisplayName("200 — cancels leave successfully")
        void cancelLeave_Returns200() throws Exception {
            LeaveResponseDto cancelled = LeaveResponseDto.builder()
                    .id(1L).userId(1L).status("CANCELLED").build();

            when(leaveService.cancelLeave(1L, 1L)).thenReturn(cancelled);

            mockMvc.perform(put("/leave/cancel/1")
                            .header("X-User-Id", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET /leave/manager/pending
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /leave/manager/pending")
    class PendingRequestsEndpoint {

        @Test
        @DisplayName("200 — returns pending requests list")
        void getPendingRequests_Returns200() throws Exception {
            when(leaveService.getPendingRequests()).thenReturn(List.of(mockLeaveResponse));

            mockMvc.perform(get("/leave/manager/pending")
                            .header("X-User-Role", "MANAGER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("PUT /leave/manager/review/{leaveId}")
    class ReviewLeaveEndpoint {

        @Test
        @DisplayName("200 — approve leave")
        void reviewLeave_Approve_Returns200() throws Exception {
            LeaveReviewDto reviewDto = new LeaveReviewDto();
            reviewDto.setAction("APPROVED");
            reviewDto.setComment("Approved");

            LeaveResponseDto approved = LeaveResponseDto.builder()
                    .id(1L).status("APPROVED").build();

            when(leaveService.reviewLeave(eq(1L), eq(2L), any())).thenReturn(approved);

            mockMvc.perform(put("/leave/manager/review/1")
                            .header("X-User-Id", 2L)
                            .header("X-User-Role", "MANAGER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reviewDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));
        }

        @Test
        @DisplayName("400 — invalid action fails @Pattern validation")
        void reviewLeave_InvalidAction_Returns400() throws Exception {
            String body = """
                    { "action": "MAYBE", "comment": "hmm" }
                    """;

            mockMvc.perform(put("/leave/manager/review/1")
                            .header("X-User-Id", 2L)
                            .header("X-User-Role", "MANAGER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — missing action fails @NotBlank validation")
        void reviewLeave_MissingAction_Returns400() throws Exception {
            String body = """
                    { "comment": "some comment" }
                    """;

            mockMvc.perform(put("/leave/manager/review/1")
                            .header("X-User-Id", 2L)
                            .header("X-User-Role", "MANAGER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET /leave/holidays
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /leave/holidays")
    class HolidaysEndpoint {

        @Test
        @DisplayName("200 — returns holidays for given year")
        void getHolidays_WithYear_Returns200() throws Exception {
            Holiday h = Holiday.builder()
                    .id(1L)
                    .holidayName("Republic Day")
                    .holidayDate(LocalDate.of(2026, 1, 26))
                    .build();
            when(leaveService.getHolidays(2026)).thenReturn(List.of(h));

            mockMvc.perform(get("/leave/holidays").param("year", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].holidayName").value("Republic Day"));
        }

        @Test
        @DisplayName("200 — defaults to current year when year=0")
        void getHolidays_DefaultYear_Returns200() throws Exception {
            when(leaveService.getHolidays(anyInt())).thenReturn(List.of());

            mockMvc.perform(get("/leave/holidays").param("year", "0"))
                    .andExpect(status().isOk());

            // year=0 in controller triggers LocalDate.now().getYear()
            verify(leaveService).getHolidays(LocalDate.now().getYear());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // POST /leave/holidays
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /leave/holidays")
    class AddHolidayEndpoint {

        @Test
        @DisplayName("201 — adds holiday successfully")
        void addHoliday_Returns201() throws Exception {
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

            when(leaveService.addHoliday(any())).thenReturn(holiday);

            mockMvc.perform(post("/leave/holidays")
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.holidayName").value("Independence Day"));
        }

        @Test
        @DisplayName("400 — missing holidayName fails validation")
        void addHoliday_MissingName_Returns400() throws Exception {
            String body = """
                    { "holidayDate": "2026-08-15" }
                    """;

            mockMvc.perform(post("/leave/holidays")
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET /leave/internal/*
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Internal Endpoints")
    class InternalEndpoints {

        @Test
        @DisplayName("GET /internal/consumption — returns stats map")
        void getConsumptionStats_Returns200() throws Exception {
            when(leaveService.getConsumptionStats()).thenReturn(Map.of("CASUAL", 5L));

            mockMvc.perform(get("/leave/internal/consumption"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.CASUAL").value(5));
        }

        @Test
        @DisplayName("GET /internal/next-holiday — returns holiday string")
        void getNextHoliday_Returns200() throws Exception {
            when(leaveService.getNextHoliday()).thenReturn("Diwali (2026-10-20)");

            mockMvc.perform(get("/leave/internal/next-holiday"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Diwali (2026-10-20)"));
        }

        @Test
        @DisplayName("GET /internal/count — returns count by status")
        void getCountByStatus_Returns200() throws Exception {
            when(leaveService.getCountByStatus("SUBMITTED")).thenReturn(3L);

            mockMvc.perform(get("/leave/internal/count").param("status", "SUBMITTED"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("3"));
        }
    }
}