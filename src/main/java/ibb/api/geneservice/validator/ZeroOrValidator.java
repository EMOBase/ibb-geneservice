package ibb.api.geneservice.validator;

import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ZeroOrValidator implements ConstraintValidator<ZeroOr, List<?>>{

    private int value;

    @Override
    public void initialize(ZeroOr constraintAnnotation) {
        this.value = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(List<?> list, ConstraintValidatorContext context) {
        if (list == null) {
            return false;
        }
        return list.size() == 0 || list.size() == value;
    }
}
