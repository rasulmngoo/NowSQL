package kz.kotov.sms.service;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import kz.kotov.sms.model.Student;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Хранилище студентов: ObservableList в памяти + локальный CSV-файл
 * (~/.student-management/students.csv).
 */
public class StudentRepository {

    private static final String HEADER = "id;fullName;group;course;birthDate;email;phone;gpa";

    private final ObservableList<Student> students = FXCollections.observableArrayList(
            s -> new Observable[]{
                    s.fullNameProperty(), s.groupProperty(), s.courseProperty(),
                    s.birthDateProperty(), s.emailProperty(), s.phoneProperty(), s.gpaProperty()
            });

    private final Path file;
    private int lastSkipped;

    public StudentRepository() {
        this(Paths.get(System.getProperty("user.home"), ".student-management", "students.csv"));
    }

    public StudentRepository(Path file) {
        this.file = file;
    }

    public ObservableList<Student> getStudents() {
        return students;
    }

    public Path getFile() {
        return file;
    }

    /** Сколько строк было пропущено как повреждённые при последнем чтении. */
    public int getLastSkipped() {
        return lastSkipped;
    }

    public int nextId() {
        return students.stream().mapToInt(Student::getId).max().orElse(0) + 1;
    }

    public void add(Student s) {
        students.add(s);
    }

    public void remove(Student s) {
        students.remove(s);
    }

    /**
     * Загружает данные из файла. Если файла нет — создаёт демонстрационные записи.
     *
     * @return количество пропущенных (повреждённых) строк
     */
    public int load() throws IOException {
        students.clear();
        if (!Files.exists(file)) {
            students.addAll(demoData());
            try {
                save();
            } catch (IOException ignored) {
                // не критично: данные остаются в памяти
            }
            return 0;
        }
        students.addAll(readFile(file));
        return lastSkipped;
    }

    public void save() throws IOException {
        exportTo(file);
    }

    public void exportTo(Path target) throws IOException {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Student s : students) {
            lines.add(String.join(";",
                    String.valueOf(s.getId()),
                    clean(s.getFullName()),
                    clean(s.getGroup()),
                    String.valueOf(s.getCourse()),
                    String.valueOf(s.getBirthDate()),
                    clean(s.getEmail()),
                    clean(s.getPhone()),
                    String.valueOf(s.getGpa())));
        }
        Files.write(target, lines, StandardCharsets.UTF_8);
    }

    /**
     * Импортирует студентов из CSV, присваивая им новые id.
     *
     * @return количество импортированных записей
     */
    public int importFrom(Path source) throws IOException {
        List<Student> read = readFile(source);
        for (Student s : read) {
            students.add(new Student(nextId(), s.getFullName(), s.getGroup(), s.getCourse(),
                    s.getBirthDate(), s.getEmail(), s.getPhone(), s.getGpa()));
        }
        return read.size();
    }

    private List<Student> readFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Student> result = new ArrayList<>();
        lastSkipped = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).replace("\uFEFF", "").trim();
            if (line.isEmpty() || (i == 0 && line.startsWith("id;"))) {
                continue;
            }
            try {
                result.add(parse(line));
            } catch (RuntimeException e) {
                lastSkipped++;
            }
        }
        return result;
    }

    private Student parse(String line) {
        String[] p = line.split(";", -1);
        if (p.length != 8) {
            throw new IllegalArgumentException("Ожидалось 8 полей");
        }
        return new Student(
                Integer.parseInt(p[0].trim()),
                p[1].trim(), p[2].trim(),
                Integer.parseInt(p[3].trim()),
                LocalDate.parse(p[4].trim()),
                p[5].trim(), p[6].trim(),
                Double.parseDouble(p[7].trim()));
    }

    private String clean(String s) {
        return s == null ? "" : s.replace(';', ',').replace('\n', ' ').replace('\r', ' ');
    }

    /** 12 демонстрационных записей. */
    private static List<Student> demoData() {
        List<Student> list = new ArrayList<>();
        list.add(new Student(1, "Сатпаев Айдос Маратович", "ИС24-21", 2, LocalDate.of(2006, 3, 14), "aidos.satpaev@mail.kz", "+7 701 111 22 33", 3.6));
        list.add(new Student(2, "Нурланова Аружан Ерлановна", "ИС24-21", 2, LocalDate.of(2006, 7, 2), "aruzhan.nurlanova@mail.kz", "+7 702 222 33 44", 3.9));
        list.add(new Student(3, "Ахметов Тимур Русланович", "ИС24-22", 2, LocalDate.of(2005, 11, 25), "timur.akhmetov@mail.kz", "+7 705 333 44 55", 3.2));
        list.add(new Student(4, "Ким Алина Сергеевна", "ИС24-22", 2, LocalDate.of(2006, 1, 9), "alina.kim@mail.kz", "+7 707 444 55 66", 3.8));
        list.add(new Student(5, "Жумабаев Данияр Асхатович", "ИС23-21", 3, LocalDate.of(2005, 5, 30), "daniyar.zhumabaev@mail.kz", "+7 747 555 66 77", 2.9));
        list.add(new Student(6, "Иванова Мария Олеговна", "ИС23-21", 3, LocalDate.of(2005, 9, 18), "maria.ivanova@mail.kz", "+7 708 666 77 88", 3.5));
        list.add(new Student(7, "Касымов Ерлан Нурланович", "ИС23-22", 3, LocalDate.of(2004, 12, 4), "erlan.kassymov@mail.kz", "+7 775 777 88 99", 2.7));
        list.add(new Student(8, "Байжанова Динара Талгатовна", "ИС22-21", 4, LocalDate.of(2004, 2, 21), "dinara.baizhanova@mail.kz", "+7 700 888 99 00", 3.9));
        list.add(new Student(9, "Омаров Нурсултан Бекович", "ИС22-21", 4, LocalDate.of(2004, 6, 11), "nursultan.omarov@mail.kz", "+7 777 123 45 67", 3.1));
        list.add(new Student(10, "Петров Артём Игоревич", "ПО25-11", 1, LocalDate.of(2007, 4, 5), "artem.petrov@mail.kz", "+7 701 234 56 78", 3.4));
        list.add(new Student(11, "Тулегенова Сабина Маратовна", "ПО25-11", 1, LocalDate.of(2007, 8, 27), "sabina.tulegenova@mail.kz", "+7 702 345 67 89", 3.7));
        list.add(new Student(12, "Серикбаев Алихан Даниярович", "ПО25-12", 1, LocalDate.of(2007, 10, 15), "alikhan.serikbaev@mail.kz", "+7 705 456 78 90", 2.8));
        return list;
    }
}
