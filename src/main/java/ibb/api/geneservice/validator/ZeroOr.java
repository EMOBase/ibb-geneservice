package ibb.api.geneservice.validator;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ PARAMETER })
@Retention(RUNTIME)
@Constraint(validatedBy = ZeroOrValidator.class)
@Documented
public @interface ZeroOr {
    String message() default "must have size of 0 or {value}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int value();
}
