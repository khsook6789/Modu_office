package com.modu.office.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * 모든 Controller 테스트가 상속하는 REST Docs 기반 추상 클래스.
 *
 * <p>
 * Why: PrettyPrint, 한글 인코딩, Security 필터 체인을 한 곳에서 관리하여
 * 각 테스트 클래스가 문서화 로직에만 집중할 수 있도록 분리.
 * </p>
 */
@SuppressWarnings("null")
@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsSupport {

	@Autowired
	protected MockMvc mockMvc;

	@BeforeEach
	void setUpRestDocs(WebApplicationContext webApplicationContext,
			RestDocumentationContextProvider provider) {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(springSecurity())
				.apply(MockMvcRestDocumentation.documentationConfiguration(provider)
						.operationPreprocessors()
						.withRequestDefaults(prettyPrint())
						.withResponseDefaults(prettyPrint()))
				.addFilters(new CharacterEncodingFilter("UTF-8", true))
				.build();
	}

	/**
	 * 스키마 이름을 명시적으로 지정하기 위한 헬퍼 메서드.
	 * 
	 * @param name 스키마 이름 (예: "ReservationRequest")
	 * @return Schema 객체
	 */
	protected com.epages.restdocs.apispec.Schema schema(String name) {
		return com.epages.restdocs.apispec.Schema.schema(name);
	}

	/**
	 * Bean Validation 제약 조건을 추출하여 한 줄로 요약해주는 헬퍼 메서드.
	 * 
	 * @param clazz    DTO 클래스
	 * @param property 필드 명
	 * @return 제약 조건 요약 문자열 (예: "(필수, 최소: 10)")
	 */
	protected String constDocs(Class<?> clazz, String property) {
		org.springframework.restdocs.constraints.ConstraintDescriptions constraints = new org.springframework.restdocs.constraints.ConstraintDescriptions(
				clazz);
		java.util.List<String> descriptions = constraints.descriptionsForProperty(property);
		if (descriptions.isEmpty()) {
			return "";
		}
		return " (" + String.join(", ", descriptions) + ")";
	}

	/**
	 * 공통 에러 응답 필드 (ErrorCode 체계)
	 * 
	 * <p>
	 * 필수: 안정적인 OpenAPI 명세 생성을 위해 사용 지점에서 반드시 
	 * .responseSchema(schema("ErrorResponse"))를 지정하십시오.
	 * </p>
	 * 
	 * @return FieldDescriptor[]
	 */
	protected org.springframework.restdocs.payload.FieldDescriptor[] commonErrorFields() {
		return new org.springframework.restdocs.payload.FieldDescriptor[] {
				org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath("status").type(
						org.springframework.restdocs.payload.JsonFieldType.STRING).description("응답 상태 (SUCCESS/ERROR)"),
				org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath("code").type(
						org.springframework.restdocs.payload.JsonFieldType.STRING).description("에러 코드 (예: E001, O002)"),
				org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath("message").type(
						org.springframework.restdocs.payload.JsonFieldType.STRING).description("에러 메시지 (사용자용 모달 등 표시용)"),
				org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath("data").type(
						org.springframework.restdocs.payload.JsonFieldType.VARIES).description("유효성 검사 실패 시 에러 상세 내역 (그 외엔 null)").optional()
		};
	}
}
