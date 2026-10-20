package kz.kotov.sms.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import kz.kotov.sms.model.Student;
import kz.kotov.sms.util.Validator;

import java.util.List;

/**
 * Контроллер второго окна — формы добавления / редактирования студента.
 */
public class StudentFormController {

    @FXML private Label formTitle;
    @FXML private TextField nameField;
    @FXML private TextField groupField;
    @FXML private ComboBox<Integer> courseCombo;
    @FXML private DatePicker birthPicker;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Slider gpaSlider;
    @FXML private Label gpaValueLabel;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private int studentId;
    private Student result;

    @FXML
    private void initialize() {
        courseCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4));
        courseCombo.setValue(1);
        gpaSlider.valueProperty().addListener((obs, oldV, newV) ->
                gpaValueLabel.setText(String.format("%.1f", newV.doubleValue())));
        gpaValueLabel.setText(String.format("%.1f", gpaSlider.getValue()));
        errorLabel.setText("");
    }

    /**
     * Подготавливает форму. existing == null — режим добавления.
     */
    public void setStudent(Student existing, int newId) {
        if (existing == null) {
            studentId = newId;
            formTitle.setText("Новый студент");
            return;
        }
        studentId = existing.getId();
        formTitle.setText("Редактирование студента");
        nameField.setText(existing.getFullName());
        groupField.setText(existing.getGroup());
        courseCombo.setValue(existing.getCourse());
        birthPicker.setValue(existing.getBirthDate());
        emailField.setText(existing.getEmail());
        phoneField.setText(existing.getPhone());
        gpaSlider.setValue(existing.getGpa());
    }

    /** Результат формы или null, если пользователь нажал «Отмена». */
    public Student getResult() {
        return result;
    }

    @FXML
    private void onSave() {
        List<String> errors = Validator.validate(
                nameField.getText(), groupField.getText(), courseCombo.getValue(),
                birthPicker.getValue(), emailField.getText(), phoneField.getText());

        if (!errors.isEmpty()) {
            errorLabel.setText(String.join("\n", errors));
            return;
        }

        double gpa = Math.round(gpaSlider.getValue() * 10.0) / 10.0;
        result = new Student(studentId,
                nameField.getText().trim(),
                groupField.getText().trim().toUpperCase(),
                courseCombo.getValue(),
                birthPicker.getValue(),
                emailField.getText().trim(),
                phoneField.getText().trim(),
                gpa);
        close();
    }

    @FXML
    private void onCancel() {
        result = null;
        close();
    }

    private void close() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}
