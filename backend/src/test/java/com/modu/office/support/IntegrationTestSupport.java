package com.modu.office.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Integration 테스트용 Base 클래스 (TestContainers Singleton + Flyway)
 *
 * <p>모든 @SpringBootTest 클래스가 이 클래스를 상속하면:
 * <ul>
 *   <li>TestContainers가 PostgreSQL 컨테이너를 자동 생성/관리 (Singleton)</li>
 *   <li>Flyway V1~V11 마이그레이션 실행 → 프로덕션과 동일한 스키마 (EXCLUDE 제약조건 포함)</li>
 *   <li>docker-compose up 없이 ./gradlew test만으로 실행 가능</li>
 * </ul>
 *
 * <p>application-test.yml의 기본값(flyway=false, create-drop)을
 * @TestPropertySource로 오버라이드하여 Flyway 기반 스키마를 사용합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
public abstract class IntegrationTestSupport {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("modu_office_test")
            .withUsername("postgres")
            .withPassword("postgres")
            .withUrlParam("stringtype", "unspecified");

    static {
        POSTGRES.start();
    }
}
