package kz.kotov.sms;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kz.kotov.sms.controller.MainController;

import java.io.IOException;

/**
 * Точка входа приложения «Student Management System».
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 1150, 700);
        scene.getStylesheets().add(MainApp.class.getResource("/css/style.css").toExternalForm());

        MainController controller = loader.getController();

        stage.setTitle("Student Management System — Котов Расул, ИС24-22");
        stage.setScene(scene);
        stage.setMinWidth(950);
        stage.setMinHeight(600);
        // закрытие окна крестиком проходит через то же подтверждение, что и «Файл → Выход»
        stage.setOnCloseRequest(event -> {
            event.consume();
            controller.onExit();
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
