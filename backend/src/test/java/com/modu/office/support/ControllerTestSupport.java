package com.modu.office.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modu.office.controller.*;
import com.modu.office.controller.Auth.AdminAuthController;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.enums.UserRole;
import org.mockito.Mockito;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.modu.office.controller.Auth.ManagerAuthController;
import com.modu.office.controller.Auth.UserAuthController;
import com.modu.office.security.JwtAuthenticationFilter;
import com.modu.office.security.OAuth2AuthenticationSuccessHandler;
import com.modu.office.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.context.annotation.Import;

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
@Import(TestSecurityConfig.class)
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
        NotificationController.class,
        // Admin
        AdminManagerController.class,
        AdminReservationController.class,
        AdminUserController.class,
        AdminDashboardController.class,
        AuditLogController.class,
        FacilityReportController.class,
        PaymentController.class
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

    @MockitoBean
    protected NotificationService notificationService;

    // --- Admin Services ---
    @MockitoBean
    protected AdminManagerService adminManagerService;

    @MockitoBean
    protected AdminUserService adminUserService;

    @MockitoBean
    protected AdminDashboardService adminDashboardService;

    @MockitoBean
    protected UpdateLogService updateLogService;

    @MockitoBean
    protected FacilityReportService facilityReportService;

    @MockitoBean
    protected PaymentService paymentService;

    // --- Security Infrastructure ---
    @MockitoBean
    protected JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    protected CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    protected OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockitoBean
    protected CustomUserDetailsService customUserDetailsService;

    /**
     * @AuthenticationPrincipal AppUser 주입을 위한 테스트용 AppUser Mock 생성.
     *
     *                          <p>
     *                          Why: AppUser는 Account FK 종속성이 있어 @Builder로 직접 생성이
     *                          어렵다.
     *                          Mockito.mock()으로 필요한 동작만 stub하여 슬라이스 테스트에 사용한다.
     *                          </p>
     *
     * @param roleName "USER", "MANAGER", "ADMIN" 등 UserRole 이름
     */
    protected AppUser createTestUser(String roleName) {
        AppUser mockUser = Mockito.mock(AppUser.class);
        UserRole userRole = UserRole.valueOf(roleName);
        Mockito.when(mockUser.getId()).thenReturn(1L);
        Mockito.when(mockUser.getRole()).thenReturn(userRole);
        Mockito.when(mockUser.getUsername()).thenReturn("test@example.com");
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = java.util.Collections
                .singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
        Mockito.doReturn(authorities).when(mockUser).getAuthorities();
        Mockito.when(mockUser.isEnabled()).thenReturn(true);
        Mockito.when(mockUser.isAccountNonExpired()).thenReturn(true);
        Mockito.when(mockUser.isAccountNonLocked()).thenReturn(true);
        Mockito.when(mockUser.isCredentialsNonExpired()).thenReturn(true);
        return mockUser;
    }
}
