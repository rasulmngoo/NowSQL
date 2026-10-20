package kz.kotov.sms.controller;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import kz.kotov.sms.model.Student;
import kz.kotov.sms.service.StudentRepository;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Контроллер главного окна: таблица студентов, поиск, фильтры, меню, диалоги.
 */
public class MainController {

    private static final String ALL_GROUPS = "Все группы";
    private static final String ALL_COURSES = "Все курсы";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ----- таблица и фильтры -----
    @FXML private TableView<Student> table;
    @FXML private TableColumn<Student, Number> colId;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colGroup;
    @FXML private TableColumn<Student, Number> colCourse;
    @FXML private TableColumn<Student, String> colEmail;
    @FXML private TableColumn<Student, Number> colGpa;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> groupFilter;
    @FXML private ComboBox<String> courseFilter;

    // ----- панель инструментов и меню -----
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private MenuItem menuEdit;
    @FXML private MenuItem menuDelete;

    // ----- строка состояния -----
    @FXML private Label statusLabel;
    @FXML private Label countLabel;

    // ----- карточка студента -----
    @FXML private Label cardName;
    @FXML private Label cardGroup;
    @FXML private Label cardCourse;
    @FXML private Label cardBirth;
    @FXML private Label cardEmail;
    @FXML private Label cardPhone;
    @FXML private Label cardGpa;

    private StudentRepository repository;
    private FilteredList<Student> filtered;

    @FXML
    private void initialize() {
        repository = new StudentRepository();
        String loadWarning = null;
        try {
            int skipped = repository.load();
            if (skipped > 0) {
                loadWarning = "Пропущено повреждённых строк в файле данных: " + skipped;
            }
        } catch (IOException | RuntimeException e) {
            loadWarning = "Не удалось прочитать файл данных:\n" + e.getMessage();
        }

        setupTable();
        setupFilters();
        setupSelection();
        refreshFilters();
        applyFilter();
        showCard(null);
        setStatus("Готово. Загружено студентов: " + repository.getStudents().size());

        if (loadWarning != null) {
            final String msg = loadWarning;
            Platform.runLater(() -> showAlert(Alert.AlertType.WARNING, "Загрузка данных", msg));
        }
    }

    // =====================================================================
    //  Настройка компонентов
    // =====================================================================

    private void setupTable() {
        colId.setCellValueFactory(c -> c.getValue().idProperty());
        colName.setCellValueFactory(c -> c.getValue().fullNameProperty());
        colGroup.setCellValueFactory(c -> c.getValue().groupProperty());
        colCourse.setCellValueFactory(c -> c.getValue().courseProperty());
        colEmail.setCellValueFactory(c -> c.getValue().emailProperty());
        colGpa.setCellValueFactory(c -> c.getValue().gpaProperty());
        colGpa.setCellFactory(col -> new TableCell<Student, Number>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.format("%.1f", value.doubleValue()));
            }
        });

        filtered = new FilteredList<>(repository.getStudents(), s -> true);
        SortedList<Student> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        // двойной клик — редактирование
        table.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });

        // клавиша Delete в таблице — удаление
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                onDelete();
            }
        });
    }

    private void setupFilters() {
        courseFilter.setItems(FXCollections.observableArrayList(ALL_COURSES, "1", "2", "3", "4"));
        courseFilter.setValue(ALL_COURSES);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        groupFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        courseFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void setupSelection() {
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, s) -> showCard(s));

        BooleanBinding nothingSelected = table.getSelectionModel().selectedItemProperty().isNull();
        btnEdit.disableProperty().bind(nothingSelected);
        btnDelete.disableProperty().bind(nothingSelected);
        menuEdit.disableProperty().bind(nothingSelected);
        menuDelete.disableProperty().bind(nothingSelected);
    }

    // =====================================================================
    //  Поиск и фильтрация
    // =====================================================================

    /** Обновляет список групп в ComboBox, сохраняя выбранное значение. */
    private void refreshFilters() {
        String current = groupFilter.getValue();
        TreeSet<String> groups = repository.getStudents().stream()
                .map(Student::getGroup)
                .collect(Collectors.toCollection(TreeSet::new));
        List<String> items = new ArrayList<>();
        items.add(ALL_GROUPS);
        items.addAll(groups);
        groupFilter.setItems(FXCollections.observableArrayList(items));
        groupFilter.setValue(current != null && items.contains(current) ? current : ALL_GROUPS);
    }

    private void applyFilter() {
        if (filtered == null) {
            return;
        }
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String group = groupFilter.getValue();
        String course = courseFilter.getValue();

        filtered.setPredicate(s -> {
            boolean byText = query.isEmpty()
                    || s.getFullName().toLowerCase().contains(query)
                    || s.getEmail().toLowerCase().contains(query);
            boolean byGroup = group == null || group.equals(ALL_GROUPS) || s.getGroup().equals(group);
            boolean byCourse = course == null || course.equals(ALL_COURSES)
                    || String.valueOf(s.getCourse()).equals(course);
            return byText && byGroup && byCourse;
        });
        countLabel.setText("Показано: " + filtered.size() + " из " + repository.getStudents().size());
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        groupFilter.setValue(ALL_GROUPS);
        courseFilter.setValue(ALL_COURSES);
        setStatus("Фильтры сброшены");
    }

    @FXML
    private void onFocusSearch() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    // =====================================================================
    //  Добавление / редактирование / удаление
    // =====================================================================

    @FXML
    private void onAdd() {
        Student created = openForm(null);
        if (created == null) {
            setStatus("Добавление отменено");
            return;
        }
        repository.add(created);
        persist();
        refreshFilters();
        applyFilter();
        table.getSelectionModel().select(created);
        table.scrollTo(created);
        setStatus("Добавлен студент: " + created.getFullName());
    }

    @FXML
    private void onEdit() {
        Student selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Редактирование", "Сначала выберите студента в таблице.");
            return;
        }
        Student edited = openForm(selected);
        if (edited == null) {
            setStatus("Редактирование отменено");
            return;
        }
        selected.copyFrom(edited);
        persist();
        refreshFilters();
        applyFilter();
        showCard(selected);
        setStatus("Данные обновлены: " + selected.getFullName());
    }

    @FXML
    private void onDelete() {
        Student selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Удаление", "Сначала выберите студента в таблице.");
            return;
        }
        boolean ok = confirm("Удаление студента",
                "Удалить студента «" + selected.getFullName() + "»?\nЭто действие нельзя отменить.");
        if (!ok) {
            return;
        }
        repository.remove(selected);
        persist();
        refreshFilters();
        applyFilter();
        setStatus("Удалён студент: " + selected.getFullName());
    }

    /**
     * Открывает второе окно (форму). Возвращает новый/изменённый объект
     * или null, если пользователь отменил ввод.
     */
    private Student openForm(Student existing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student-form.fxml"));
            Parent root = loader.load();
            StudentFormController form = loader.getController();
            form.setStudent(existing, repository.nextId());

            Stage stage = new Stage();
            stage.initOwner(window());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(existing == null ? "Добавление студента" : "Редактирование студента");
            stage.setResizable(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            return form.getResult();
        } catch (IOException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось открыть форму:\n" + e.getMessage());
            return null;
        }
    }

    // =====================================================================
    //  Сохранение, импорт и экспорт CSV
    // =====================================================================

    @FXML
    private void onSave() {
        if (persist()) {
            setStatus("Данные сохранены: " + repository.getFile());
        }
    }

    private boolean persist() {
        try {
            repository.save();
            return true;
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка сохранения",
                    "Не удалось сохранить данные:\n" + e.getMessage());
            setStatus("Ошибка сохранения");
            return false;
        }
    }

    @FXML
    private void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт в CSV");
        chooser.setInitialFileName("students.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-файлы (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            repository.exportTo(file.toPath());
            setStatus("Экспортировано в " + file.getName());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка экспорта", "Не удалось записать файл:\n" + e.getMessage());
        }
    }

    @FXML
    private void onImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Импорт из CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-файлы (*.csv)", "*.csv"));
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return;
        }
        try {
            int count = repository.importFrom(file.toPath());
            int skipped = repository.getLastSkipped();
            persist();
            refreshFilters();
            applyFilter();
            setStatus("Импортировано записей: " + count);
            if (skipped > 0) {
                showAlert(Alert.AlertType.WARNING, "Импорт",
                        "Импортировано: " + count + "\nПропущено некорректных строк: " + skipped);
            }
        } catch (IOException | RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка импорта", "Не удалось прочитать файл:\n" + e.getMessage());
        }
    }

    // =====================================================================
    //  Статистика, справка, выход
    // =====================================================================

    @FXML
    private void onStatistics() {
        List<Student> all = repository.getStudents();
        if (all.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Статистика", "В базе пока нет студентов.");
            return;
        }
        double avg = all.stream().mapToDouble(Student::getGpa).average().orElse(0);
        Student best = all.stream().max((a, b) -> Double.compare(a.getGpa(), b.getGpa())).orElse(null);
        Map<Integer, Long> byCourse = all.stream().collect(
                Collectors.groupingBy(Student::getCourse, TreeMap::new, Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("Всего студентов: ").append(all.size()).append('\n');
        sb.append(String.format("Средний GPA: %.2f%n", avg));
        if (best != null) {
            sb.append(String.format("Лучший GPA: %s (%.1f)%n", best.getFullName(), best.getGpa()));
        }
        sb.append("\nПо курсам:\n");
        byCourse.forEach((course, count) -> sb.append("  ").append(course).append(" курс — ")
                .append(count).append('\n'));
        showAlert(Alert.AlertType.INFORMATION, "Статистика", sb.toString());
    }

    @FXML
    private void onAbout() {
        showAlert(Alert.AlertType.INFORMATION, "О программе",
                "Student Management System\n"
                        + "СРО №1 по дисциплине «Визуальное программирование»\n\n"
                        + "Автор: Котов Расул, группа ИС24-22\n"
                        + "Технологии: Java, JavaFX, FXML, Scene Builder\n\n"
                        + "Горячие клавиши:\n"
                        + "Ctrl+N — добавить, Ctrl+E — изменить, Ctrl+D — удалить,\n"
                        + "Ctrl+F — поиск, Ctrl+S — сохранить, Ctrl+Q — выход, F1 — справка.");
    }

    @FXML
    public void onExit() {
        if (confirm("Выход", "Сохранить данные и закрыть приложение?")) {
            persist();
            Platform.exit();
        }
    }

    // =====================================================================
    //  Вспомогательные методы
    // =====================================================================

    private void showCard(Student s) {
        if (s == null) {
            cardName.setText("Студент не выбран");
            cardGroup.setText("—");
            cardCourse.setText("—");
            cardBirth.setText("—");
            cardEmail.setText("—");
            cardPhone.setText("—");
            cardGpa.setText("—");
            return;
        }
        cardName.setText(s.getFullName());
        cardGroup.setText(s.getGroup());
        cardCourse.setText(String.valueOf(s.getCourse()));
        cardBirth.setText(s.getBirthDate() == null ? "—" : s.getBirthDate().format(DATE_FMT));
        cardEmail.setText(s.getEmail());
        cardPhone.setText(s.getPhone());
        cardGpa.setText(String.format("%.1f", s.getGpa()));
    }

    private void setStatus(String message) {
        statusLabel.setText(LocalTime.now().format(TIME_FMT) + "  —  " + message);
    }

    private Window window() {
        return table.getScene() == null ? null : table.getScene().getWindow();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Window owner = window();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Window owner = window();
        if (owner != null) {
            alert.initOwner(owner);
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }
}
