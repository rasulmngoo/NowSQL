package kz.kotov.sms.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Проверка пользовательского ввода. Возвращает список сообщений об ошибках;
 * пустой список означает, что данные корректны.
 */
public final class Validator {

    /** 2–4 слова, только буквы, дефис и апостроф. */
    private static final Pattern NAME =
            Pattern.compile("^\\p{L}[\\p{L}'\\-]+(\\s+\\p{L}[\\p{L}'\\-]+){1,3}$");
    /** Например: ИС24-22, ПО23-1. */
    private static final Pattern GROUP = Pattern.compile("^\\p{L}{2,4}\\d{2}-\\d{1,2}$");
    private static final Pattern EMAIL =
            Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 ()\\-]{8,17}$");

    private Validator() {
    }

    public static List<String> validate(String fullName, String group, Integer course,
                                        LocalDate birthDate, String email, String phone) {
        List<String> errors = new ArrayList<>();

        if (isBlank(fullName)) {
            errors.add("ФИО: поле обязательно для заполнения.");
        } else if (!NAME.matcher(fullName.trim()).matches()) {
            errors.add("ФИО: введите 2–4 слова только буквами (например, Котов Расул).");
        }

        if (isBlank(group)) {
            errors.add("Группа: поле обязательно для заполнения.");
        } else if (!GROUP.matcher(group.trim()).matches()) {
            errors.add("Группа: формат «ИС24-22» (буквы, две цифры года, дефис, номер).");
        }

        if (course == null || course < 1 || course > 4) {
            errors.add("Курс: выберите значение от 1 до 4.");
        }

        if (birthDate == null) {
            errors.add("Дата рождения: укажите дату в формате дд.мм.гггг.");
        } else if (birthDate.isAfter(LocalDate.now().minusYears(15))) {
            errors.add("Дата рождения: студент должен быть не младше 15 лет.");
        } else if (birthDate.isBefore(LocalDate.of(1940, 1, 1))) {
            errors.add("Дата рождения: дата не может быть раньше 1940 года.");
        }

        if (isBlank(email)) {
            errors.add("E-mail: поле обязательно для заполнения.");
        } else if (!EMAIL.matcher(email.trim()).matches()) {
            errors.add("E-mail: некорректный адрес (пример: name@mail.kz).");
        }

        if (isBlank(phone)) {
            errors.add("Телефон: поле обязательно для заполнения.");
        } else if (!PHONE.matcher(phone.trim()).matches()) {
            errors.add("Телефон: некорректный номер (пример: +7 701 123 45 67).");
        }

        return errors;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
