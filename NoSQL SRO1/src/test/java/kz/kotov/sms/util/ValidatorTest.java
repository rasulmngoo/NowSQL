package kz.kotov.sms.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    private static final LocalDate OK_DATE = LocalDate.of(2006, 5, 1);

    @Test
    void validDataHasNoErrors() {
        assertTrue(Validator.validate("Котов Расул", "ИС24-22", 2, OK_DATE,
                "rasul@mail.kz", "+7 701 123 45 67").isEmpty());
    }

    @Test
    void emptyNameIsRejected() {
        assertFalse(Validator.validate("", "ИС24-22", 2, OK_DATE,
                "rasul@mail.kz", "+7 701 123 45 67").isEmpty());
    }

    @Test
    void nameWithDigitsIsRejected() {
        assertFalse(Validator.validate("Котов 123", "ИС24-22", 2, OK_DATE,
                "rasul@mail.kz", "+7 701 123 45 67").isEmpty());
    }

    @Test
    void wrongGroupFormatIsRejected() {
        assertFalse(Validator.validate("Котов Расул", "группа", 2, OK_DATE,
                "rasul@mail.kz", "+7 701 123 45 67").isEmpty());
    }

    @Test
    void courseOutOfRangeIsRejected() {
        assertFalse(Validator.validate("Котов Расул", "ИС24-22", 7, OK_DATE,
                "rasul@mail.kz", "+7 701 123 45 67").isEmpty());
    }

    @Test
    void futureBirthDateIsRejected() {
        assertFalse(Validator.validate("Котов Расул", "ИС24-22", 2, LocalDate.now().plusDays(1),
                "rasul@mail.kz", "+7 701 123 45 67").isEmpty());
    }

    @Test
    void missingBirthDateIsRejected() {
        assertFalse(Validator.validate("Котов Расул", "ИС24-22", 2, null,
                "rasul@mail.kz", "+7 701 123 45 67").isEmpty());
    }

    @Test
    void invalidEmailIsRejected() {
        assertFalse(Validator.validate("Котов Расул", "ИС24-22", 2, OK_DATE,
                "not-an-email", "+7 701 123 45 67").isEmpty());
    }

    @Test
    void invalidPhoneIsRejected() {
        assertFalse(Validator.validate("Котов Расул", "ИС24-22", 2, OK_DATE,
                "rasul@mail.kz", "abc").isEmpty());
    }
}
