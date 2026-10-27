package com.servicehub.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.Locale;

@Component
public class InputSanitizer {
    private static final PolicyFactory TEXT_ONLY = new HtmlPolicyBuilder().toFactory();
    public String plainText(String value) {
        if (value == null) return "";
        return TEXT_ONLY.sanitize(Normalizer.normalize(value.strip(), Normalizer.Form.NFC)).strip();
    }
    public String optionalText(String value) {
        String text = plainText(value);
        return text.isBlank() ? null : text;
    }
    public String email(String value) { return value.strip().toLowerCase(Locale.ROOT); }
}
