package com.modu.office.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidImageUrlValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR,
        ElementType.PARAMETER, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImageUrl {
    String message() default "유효하지 않거나 안전하지 않은 외부 이미지 URL입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
