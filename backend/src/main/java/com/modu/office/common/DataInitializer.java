package com.modu.office.common;

import com.modu.office.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@Profile("prod")
@ConditionalOnProperty(name = "app.data-seeding.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final int USER_COUNT = 5000;
    private static final int MANAGER_COUNT = 500;
    private static final int TOTAL_ACCOUNT_COUNT = USER_COUNT + MANAGER_COUNT;
    private static final int OFFICE_COUNT = 100;
    private static final int ROOMS_PER_OFFICE = 5;
    private static final int BATCH_SIZE = 1000;

    private final AccountRepository accountRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager em;
    private final PasswordEncoder passwordEncoder;
    
    private final Random random = new Random(42); 

    private String dummyPasswordHash;

    @org.springframework.beans.factory.annotation.Value("${app.data-seeding.clean-before-seeding:false}")
    private boolean cleanBeforeSeeding;

    @org.springframework.beans.factory.annotation.Value("${app.data-seeding.initial-password:Test1234!}")
    private String initialPassword;

    @PostConstruct
    public void init() {
        // Fix: Credentials should not be hard-coded (Use @Value or secure source)
        this.dummyPasswordHash = passwordEncoder.encode(initialPassword);
    }

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> managerIds = new ArrayList<>();
    private final List<Long> officeIds = new ArrayList<>();
    private final List<Long> roomIds = new ArrayList<>();
    private final List<Long> facilityIds = new ArrayList<>();
    private final List<Long> accountIds = new ArrayList<>();
    private final Map<Long, Long> roomIdToOfficeIdMap = new HashMap<>();

    private static final String[] ROOM_CATEGORIES = {
        "회의실", "세미나실", "스터디룸", "프레젠테이션룸", "소회의실", "대회의실", "미팅룸", "워크샵룸"
    };

    private static final long[] ROOM_PRICES = {
        8000, 12000, 15000, 20000, 25000, 30000, 40000, 50000
    };

    private static final String[] ROOM_DESCRIPTIONS = {
        "자연 채광이 풍부한 쾌적한 공간입니다.",
        "대형 모니터와 화이트보드가 구비된 회의 전용 공간입니다.",
        "소규모 팀 미팅에 최적화된 아담한 공간입니다.",
        "프레젠테이션 장비가 완비된 발표 전용 공간입니다.",
        "조용한 환경에서 집중 작업이 가능한 스터디 공간입니다.",
        "대규모 세미나 및 워크샵에 적합한 넓은 공간입니다.",
        "편안한 좌석과 음료 서비스가 제공되는 프리미엄 공간입니다.",
        "빔 프로젝터와 음향 시설이 완비된 다목적 공간입니다."
    };

    private static final String[][] CITY_DATA = {
        {"서울 강남구", "37.4979", "127.0276"},
        {"서울 서초구", "37.4837", "127.0324"},
        {"서울 마포구", "37.5663", "126.9014"},
        {"서울 종로구", "37.5720", "126.9794"},
        {"서울 영등포구", "37.5264", "126.8963"},
        {"서울 송파구", "37.5145", "127.1066"},
        {"서울 중구", "37.5636", "126.9976"},
        {"부산 해운대구", "35.1631", "129.1638"},
        {"부산 부산진구", "35.1629", "129.0531"},
        {"대전 유성구", "36.3623", "127.3561"},
        {"대전 서구", "36.3553", "127.3835"},
        {"인천 남동구", "37.4488", "126.7316"},
        {"인천 연수구", "37.4101", "126.6783"},
        {"대구 중구", "35.8714", "128.6014"},
        {"대구 수성구", "35.8583", "128.6309"},
        {"광주 서구", "35.1529", "126.8905"},
        {"수원시 영통구", "37.2596", "127.0462"},
        {"성남시 분당구", "37.3825", "127.1188"},
        {"제주시", "33.4996", "126.5312"},
        {"울산 남구", "35.5447", "129.3301"},
    };

    private static final Short[][] OPEN_DAY_PATTERNS = {
        {1, 2, 3, 4, 5},
        {1, 2, 3, 4, 5},
        {1, 2, 3, 4, 5, 6},
        {1, 2, 3, 4, 5, 6},
        {1, 2, 3, 4, 5, 6, 7},
    };

    @Override
    @Transactional
    public void run(String... args) {
        if (!isLoadTestDatabase()) {
            return;
        }

        if (cleanBeforeSeeding) {
            truncateAllTables();
        }

        if (accountRepository.count() > 0) {
            log.info("Data already exists. Skipping data seeding.");
            return;
        }

        executeSeeding();
    }

    private boolean isLoadTestDatabase() {
        String currentDatabase = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        if (!"modu_office_loadtest".equals(currentDatabase)) {
            log.warn("Seeding skip: Current database [{}] is NOT 'modu_office_loadtest'.", currentDatabase);
            return false;
        }
        return true;
    }

    private void truncateAllTables() {
        log.info("Truncating tables for a fresh load test...");
        jdbcTemplate.execute("TRUNCATE TABLE account, app_user, office, room, facility, room_facility, reservation, review, update_log, cancellation_policy, notification, room_favorite, refresh_token, facility_report, room_image, payment CASCADE");
    }

    private void executeSeeding() {
        log.info("Starting large-scale data seeding...");
        long startTime = System.currentTimeMillis();

        seedAccountsAndUsers();
        seedRefreshTokens();
        seedFacilities();
        seedOfficesAndRooms();
        seedCancellationPolicies();
        seedRoomFacilities();
        seedReservations();
        seedReviews();

        log.info("Data seeding completed in {} ms.", (System.currentTimeMillis() - startTime));
    }

    public void seedAccountsAndUsers() {
        log.info("Seeding {} Accounts and AppUsers...", TOTAL_ACCOUNT_COUNT);
        List<String> emailsInOrder = insertAccountsBatch();
        mapEmailsToAccountIds();
        insertAppUsersBatch(emailsInOrder);
        collectUserIdsByRole();
    }

    private List<String> insertAccountsBatch() {
        String sql = "INSERT INTO account (email, password_hash, login_type, status, created_at, updated_at) VALUES (?, ?, 'LOCAL'::login_type, 'ACTIVE'::account_status, NOW(), NOW())";
        List<Object[]> batch = new ArrayList<>();
        List<String> emails = new ArrayList<>();

        for (int i = 1; i <= TOTAL_ACCOUNT_COUNT; i++) {
            boolean isUser = (i <= USER_COUNT);
            String email = String.format("loadtest-%s-%04d@test.com", isUser ? "user" : "manager", isUser ? i : i - USER_COUNT);
            emails.add(email);
            batch.add(new Object[]{email, dummyPasswordHash});
            
            if (batch.size() == BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) jdbcTemplate.batchUpdate(sql, batch);
        return emails;
    }

    private void mapEmailsToAccountIds() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, email FROM account WHERE email LIKE 'loadtest-%' ORDER BY id");
        for (Map<String, Object> row : rows) {
            accountIds.add(((Number) row.get("id")).longValue());
        }
    }

    private void insertAppUsersBatch(List<String> emails) {
        String sql = "INSERT INTO app_user (account_id, name, role, approval_status, created_at, updated_at) VALUES (?, ?, ?::user_role, ?::operator_approval_status, NOW(), NOW())";
        List<Object[]> batch = new ArrayList<>();
        
        List<Map<String, Object>> accountRows = jdbcTemplate.queryForList("SELECT id, email FROM account WHERE email LIKE 'loadtest-%' ORDER BY id");
        Map<String, Long> emailToId = new HashMap<>();
        for (Map<String, Object> row : accountRows) {
            emailToId.put((String) row.get("email"), ((Number) row.get("id")).longValue());
        }

        for (int i = 0; i < emails.size(); i++) {
            String email = emails.get(i);
            boolean isUser = (i < USER_COUNT);
            String role = isUser ? "USER" : "MANAGER";
            String name = String.format("Test %s %d", isUser ? "user" : "manager", isUser ? (i + 1) : (i + 1 - USER_COUNT));
            
            batch.add(new Object[]{emailToId.get(email), name, role, isUser ? null : "APPROVED"});
            if (batch.size() == BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) jdbcTemplate.batchUpdate(sql, batch);
    }

    private void collectUserIdsByRole() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT u.id, u.role FROM app_user u JOIN account a ON u.account_id = a.id WHERE a.email LIKE 'loadtest-%' ORDER BY u.id");
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            if ("USER".equals(row.get("role"))) userIds.add(id);
            else managerIds.add(id);
        }
    }

    public void seedRefreshTokens() {
        String sql = "INSERT INTO refresh_token (token, account_id, expiry_date, created_at) VALUES (?, ?, ?, NOW())";
        List<Object[]> batch = new ArrayList<>();
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);

        for (Long accountId : accountIds) {
            batch.add(new Object[]{UUID.randomUUID().toString(), accountId, Timestamp.valueOf(expiry)});
            if (batch.size() == BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) jdbcTemplate.batchUpdate(sql, batch);
    }

    public void seedFacilities() {
        String[] names = {"Wi-Fi", "Projector", "Whiteboard", "Monitor", "Air Conditioner", "Heater", "Water", "Coffee", "Power", "Parking", "Elevator", "Print", "HDMI", "Mic", "Speakers"};
        String[] codes = {"WIFI", "PROJ", "WHTB", "MONI", "AIR", "HEAT", "WATR", "COFF", "POWR", "PARK", "ELEV", "PRNT", "HDMI", "MIC", "SPKR"};

        for (int i = 0; i < names.length; i++) {
            com.modu.office.entity.Facility facility = com.modu.office.entity.Facility.builder()
                    .facilityCode(codes[i]).facilityName(names[i]).isActive(true).build();
            em.persist(facility);
            facilityIds.add(facility.getId());
        }
        em.flush();
        em.clear();
    }

    public void seedOfficesAndRooms() {
        int roomCounter = 1;
        for (int i = 1; i <= OFFICE_COUNT; i++) {
            com.modu.office.entity.Office office = createOffice(i);
            em.persist(office);
            officeIds.add(office.getId());

            for (int j = 1; j <= ROOMS_PER_OFFICE; j++) {
                com.modu.office.entity.Room room = createRoom(office, roomCounter++);
                em.persist(room);
                roomIds.add(room.getId());
                roomIdToOfficeIdMap.put(room.getId(), office.getId());
            }
            if (i % 10 == 0) { em.flush(); em.clear(); }
        }
        em.flush(); em.clear();
    }

    private com.modu.office.entity.Office createOffice(int index) {
        String[] cityInfo = CITY_DATA[index % CITY_DATA.length];
        return com.modu.office.entity.Office.builder()
                .name(cityInfo[0] + " " + ((index - 1) / CITY_DATA.length + 1) + "호점")
                .location(cityInfo[0] + " " + index + "번길 " + (random.nextInt(50) + 1))
                .latitude(Double.parseDouble(cityInfo[1]) + (random.nextDouble() * 0.02 - 0.01))
                .longitude(Double.parseDouble(cityInfo[2]) + (random.nextDouble() * 0.02 - 0.01))
                .openTime(java.time.LocalTime.of(9, 0)).closeTime(java.time.LocalTime.of(18, 0))
                .openDays(OPEN_DAY_PATTERNS[random.nextInt(OPEN_DAY_PATTERNS.length)])
                .manager(em.getReference(com.modu.office.entity.AppUser.class, managerIds.get(random.nextInt(managerIds.size()))))
                .build();
    }

    private com.modu.office.entity.Room createRoom(com.modu.office.entity.Office office, int counter) {
        return com.modu.office.entity.Room.builder()
                .office(office).name("Room " + counter).roomCode("R" + counter)
                .capacity(random.nextInt(10) + 2).price(java.math.BigDecimal.valueOf(ROOM_PRICES[counter % ROOM_PRICES.length]))
                .bufferTime(30).floor((counter % 10) + 1)
                .status((counter % 20 == 19) ? com.modu.office.entity.enums.RoomStatus.INACTIVE : com.modu.office.entity.enums.RoomStatus.AVAILABLE)
                .category(ROOM_CATEGORIES[counter % ROOM_CATEGORIES.length])
                .description(ROOM_DESCRIPTIONS[counter % ROOM_DESCRIPTIONS.length]).build();
    }

    public void seedCancellationPolicies() {
        String sql = "INSERT INTO cancellation_policy (office_id, days_before, refund_rate, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())";
        List<Object[]> batch = new ArrayList<>();
        for (Long officeId : officeIds) {
            batch.add(new Object[]{officeId, 7, 100});
            batch.add(new Object[]{officeId, 3, 50});
            batch.add(new Object[]{officeId, 1, 0});
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    public void seedRoomFacilities() {
        String sql = "INSERT INTO room_facility (room_id, facility_id, created_at) VALUES (?, ?, NOW())";
        List<Object[]> batch = new ArrayList<>();
        for (Long roomId : roomIds) {
            int num = random.nextInt(3) + 2;
            java.util.Set<Long> picked = new java.util.HashSet<>();
            while (picked.size() < num) picked.add(facilityIds.get(random.nextInt(facilityIds.size())));
            for (Long facId : picked) batch.add(new Object[]{roomId, facId});
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    public void seedReservations() {
        int resPerRoom = 25;
        int expected = roomIds.size() * resPerRoom;
        String sql = "INSERT INTO reservation (title, office_id, room_id, user_id, start_at, end_at, end_at_include_buffer_time, status, version, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?::reservation_status, 0, NOW(), NOW())";
        List<Object[]> batch = new ArrayList<>();
        LocalDateTime baseDate = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        int count = 0;
        for (Long roomId : roomIds) {
            for (int i = 0; i < resPerRoom; i++) {
                batch.add(createReservationArgs(roomId, i, baseDate, ++count, expected));
                if (batch.size() == BATCH_SIZE) { jdbcTemplate.batchUpdate(sql, batch); batch.clear(); }
            }
        }
        if (!batch.isEmpty()) jdbcTemplate.batchUpdate(sql, batch);
    }

    private Object[] createReservationArgs(Long roomId, int index, LocalDateTime baseDate, int count, int expected) {
        int hour = (index < 20) ? 9 + (index % 8) : 9 + (index % 3);
        LocalDateTime start = baseDate.plusDays(index % 30).withHour(hour).withMinute(0);
        
        String status;
        if (count < expected * 0.6) {
            status = "CONFIRMED";
        } else if (count < expected * 0.8) {
            status = "PENDING_APPROVAL";
        } else if (count < expected * 0.9) {
            status = "PENDING_PAYMENT";
        } else {
            status = "CANCELED";
        }
        
        return new Object[]{"Res " + count, roomIdToOfficeIdMap.get(roomId), roomId, userIds.get(random.nextInt(userIds.size())), Timestamp.valueOf(start), Timestamp.valueOf(start.plusHours(1)), Timestamp.valueOf(start.plusHours(1).plusMinutes(30)), status};
    }

    public void seedReviews() {
        List<Long> confirmed = jdbcTemplate.queryForList("SELECT id FROM reservation WHERE status = 'CONFIRMED' LIMIT 5000", Long.class);
        String sql = "INSERT INTO review (reservation_id, author_user_id, rating, content, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())";
        List<Object[]> batch = new ArrayList<>();
        String[] contents = {"Good!", "Great space!", "Recommended.", "Clean and quiet.", "Nice meeting room."};
        for (Long resId : confirmed) {
            batch.add(new Object[]{resId, userIds.get(random.nextInt(userIds.size())), random.nextInt(5) + 1, contents[random.nextInt(contents.length)]});
            if (batch.size() == BATCH_SIZE) { jdbcTemplate.batchUpdate(sql, batch); batch.clear(); }
        }
        if (!batch.isEmpty()) jdbcTemplate.batchUpdate(sql, batch);
    }
}
