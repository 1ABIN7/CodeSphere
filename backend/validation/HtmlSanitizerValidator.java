package com.codesphere.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class HtmlSanitizerValidator implements ConstraintValidator<SanitizedHtml, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Use @NotNull separately if required
        }

        // Clean using Jsoup's safe basic markup list (allows strong, em, b, i, p, etc.)
        // If you want absolutely no HTML tags, change Safelist.basic() to Safelist.none()
        String clean = Jsoup.clean(value, Safelist.basic());

        // Check if Jsoup altered the string (indicating unsafe markup was stripped)
        // Note: For rich-text fields, we can mutate/replace the input. For validation check:
        boolean isValid = Jsoup.isValid(value, Safelist.basic());

        return isValid;
    }
}