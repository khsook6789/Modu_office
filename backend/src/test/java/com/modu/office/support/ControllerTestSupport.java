package com.modu.office.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modu.office.controller.*;
import com.modu.office.controller.Auth.AdminAuthController;
import com.modu.office.controller.Auth.ManagerAuthController;
import com.modu.office.controller.Auth.UserAuthController;
import com.modu.office.security.JwtAuthenticationFilter;
import com.modu.office.security.OAuth2AuthenticationSuccessHandler;
import com.modu.office.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 모든 Controller 테스트의 공통 기반 클래스.
 *
 * <p>
 * Why: @WebMvcTest 컨텍스트에서 필요한 모든 @MockBean을 한 곳에 선언하여
 * 각 테스트 파일이 테스트 로직에만 집중하도록 관심사를 분리.
 * </p>
 *
 * <p>
 * 주의: AuditLogController는 UpdateLogService를 사용함 (AuditLogService 없음).
 * </p>
 */
@WebMvcTest(controllers = {
        // Auth
        UserAuthController.class,
        ManagerAuthController.class,
        AdminAuthController.class,
        // Core Domain
        UserController.class,
        OfficeController.class,
        RoomController.class,
        FacilityController.class,
        ReservationController.class,
        ReviewController.class,
        RoomFavoriteController.class,
        // Admin
        AdminManagerController.class,
        AdminReservationController.class,
        AdminUserController.class,
        AuditLogController.class,
        UpdateLogController.class
})
public abstract class ControllerTestSupport extends RestDocsSupport {

    @Autowired
    protected ObjectMapper objectMapper;

    // --- Auth ---
    @MockitoBean
    protected AuthService authService;

    // --- Core Domain Services ---
    @MockitoBean
    protected UserService userService;

    @MockitoBean
    protected OfficeService officeService;

    @MockitoBean
    protected RoomService roomService;

    @MockitoBean
    protected FacilityService facilityService;

    @MockitoBean
    protected ReservationService reservationService;

    @MockitoBean
    protected ReviewService reviewService;

    @MockitoBean
    protected RoomFavoriteService roomFavoriteService;

    // --- Admin Services ---
    @MockitoBean
    protected AdminManagerService adminManagerService;

    @MockitoBean
    protected AdminUserService adminUserService;

    @MockitoBean
    protected UpdateLogService updateLogService;

    // --- Security Infrastructure ---
    @MockitoBean
    protected JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    protected CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    protected OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockitoBean
    protected CustomUserDetailsService customUserDetailsService;
}
