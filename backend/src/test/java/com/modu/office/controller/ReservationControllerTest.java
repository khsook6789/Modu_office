package com.modu.office.controller;

import com.modu.office.dto.response.ReservationResponse;
import com.modu.office.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)

@Import(ReservationControllerTest.TestSecurityConfig.class)
@SuppressWarnings("null")
class ReservationControllerTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private ReservationService reservationService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.modu.office.security.JwtTokenProvider jwtTokenProvider;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.modu.office.service.CustomUserDetailsService customUserDetailsService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    @DisplayName("오퍼레이터는 예약 검색을 수행할 수 있다")
    @WithMockUser(roles = "MANAGER")
    void searchReservations_Operator_Success() throws Exception {
        // Given
        Page<ReservationResponse> emptyPage = new PageImpl<>(Collections.emptyList());
        given(reservationService.searchReservations(any(), any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/reservations/search")
                .param("guestName", "Alice")
                .param("startDate", "2023-10-01")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())) // CSRF token required for non-GET usually, but GET is safe. security config
                               // might require it depending on setup. usually ignored for GET.
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("일반 사용자는 예약 검색에 접근할 수 없다")
    @WithMockUser(roles = "USER") // USER role in Enum, usually mapped to ROLE_CUSTOMER or ROLE_USER depending
                                  // on security config. Assuming ROLE_USER based on previous context.
                                  // AppUser.getAuthorities() creates "ROLE_" + role.name(). UserRole has
                                  // USER. So it should be ROLE_CUSTOMER. But wait,
                                  // @WithMockUser(roles="USER") adds "ROLE_USER". Check SecurityConfig?
    // UserRole defined: USER, MANAGER, ADMIN.
    // So role should be "USER".
    void searchReservations_Customer_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/reservations/search")
                .param("guestName", "Alice")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 예약 검색을 수행할 수 있다")
    @WithMockUser(roles = "ADMIN")
    void searchReservations_Admin_Success() throws Exception {
        // Given
        Page<ReservationResponse> emptyPage = new PageImpl<>(Collections.emptyList());
        given(reservationService.searchReservations(any(), any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/reservations/search")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isOk());
    }
}
