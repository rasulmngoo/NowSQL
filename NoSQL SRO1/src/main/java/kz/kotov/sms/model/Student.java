package kz.kotov.sms.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

/**
 * Модель «Студент». Используются JavaFX Property, чтобы TableView
 * автоматически обновлялся при изменении данных.
 */
public class Student {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty fullName = new SimpleStringProperty("");
    private final StringProperty group = new SimpleStringProperty("");
    private final IntegerProperty course = new SimpleIntegerProperty(1);
    private final ObjectProperty<LocalDate> birthDate = new SimpleObjectProperty<>();
    private final StringProperty email = new SimpleStringProperty("");
    private final StringProperty phone = new SimpleStringProperty("");
    private final DoubleProperty gpa = new SimpleDoubleProperty();

    public Student(int id, String fullName, String group, int course,
                   LocalDate birthDate, String email, String phone, double gpa) {
        this.id.set(id);
        this.fullName.set(fullName);
        this.group.set(group);
        this.course.set(course);
        this.birthDate.set(birthDate);
        this.email.set(email);
        this.phone.set(phone);
        this.gpa.set(gpa);
    }

    /** Копирует все поля (кроме id) из другого студента. */
    public void copyFrom(Student other) {
        setFullName(other.getFullName());
        setGroup(other.getGroup());
        setCourse(other.getCourse());
        setBirthDate(other.getBirthDate());
        setEmail(other.getEmail());
        setPhone(other.getPhone());
        setGpa(other.getGpa());
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getFullName() { return fullName.get(); }
    public void setFullName(String v) { fullName.set(v); }
    public StringProperty fullNameProperty() { return fullName; }

    public String getGroup() { return group.get(); }
    public void setGroup(String v) { group.set(v); }
    public StringProperty groupProperty() { return group; }

    public int getCourse() { return course.get(); }
    public void setCourse(int v) { course.set(v); }
    public IntegerProperty courseProperty() { return course; }

    public LocalDate getBirthDate() { return birthDate.get(); }
    public void setBirthDate(LocalDate v) { birthDate.set(v); }
    public ObjectProperty<LocalDate> birthDateProperty() { return birthDate; }

    public String getEmail() { return email.get(); }
    public void setEmail(String v) { email.set(v); }
    public StringProperty emailProperty() { return email; }

    public String getPhone() { return phone.get(); }
    public void setPhone(String v) { phone.set(v); }
    public StringProperty phoneProperty() { return phone; }

    public double getGpa() { return gpa.get(); }
    public void setGpa(double v) { gpa.set(v); }
    public DoubleProperty gpaProperty() { return gpa; }

    @Override
    public String toString() {
        return getFullName();
    }
}
