package com.modu.office.support;

import com.modu.office.config.JpaConfig;
import com.modu.office.config.QueryDslConfig;
import com.modu.office.config.SecurityConfig;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Repository 테스트용 Base 클래스 (TestContainers Singleton 패턴)
 *
 * <p>모든 @DataJpaTest 클래스가 이 클래스를 상속하면:
 * <ul>
 *   <li>TestContainers가 PostgreSQL 컨테이너를 자동 생성/관리</li>
 *   <li>docker-compose up 없이 ./gradlew test만으로 실행 가능</li>
 *   <li>@Transactional 롤백으로 테스트 간 데이터 격리 보장</li>
 * </ul>
 */
@DataJpaTest(excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { SecurityConfig.class }))
@Import({ QueryDslConfig.class, JpaConfig.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public abstract class RepositoryTestSupport {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("modu_office_test")
            .withUsername("modu_user")
            .withPassword("modu_office_pw123!");

    static {
        POSTGRES.start();
    }
}
