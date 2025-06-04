package org.example.client;

import org.example.model.Role;
import org.example.model.User;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {

    private Stage primaryStage;
    private ClientService clientService;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Library Management System");
        this.clientService = ClientService.getInstance();

        // 尝试启动时连接服务器，但如果失败则由登录页面处理重连逻辑
        if (!clientService.connect()) {
            org.example.util.AlertUtil.showError(
                    "Connection Failed",
                    "Server Connection Error",
                    "Could not connect to the server. Please ensure the server is running."
            );
            // 可以在这里提示用户重试或退出
        }

        showLoginView();

        // 应用关闭时断开客户端并退出
        primaryStage.setOnCloseRequest(event -> {
            clientService.disconnect();
            Platform.exit();
            System.exit(0);
        });
    }

    public void showLoginView() {
        try {
            // 将相对路径改为 classpath 绝对路径：/org/example/client/view/LoginView.fxml
            URL fxmlUrl = getClass().getResource("/org/example/client/view/LoginView.fxml");
            if (fxmlUrl == null) {
                throw new IOException("无法找到 /org/example/client/view/LoginView.fxml，请检查 FXML 是否存在");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            org.example.client.controller.LoginController controller = loader.getController();
            controller.setMainApp(this);

            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            org.example.util.AlertUtil.showError(
                    "Application Error",
                    "Could not load the login screen.",
                    e.getMessage()
            );
        }
    }

    public void showRegistrationView() {
        try {
            // 绝对路径改为 /org/example/client/view/RegistrationView.fxml
            URL fxmlUrl = getClass().getResource("/org/example/client/view/RegistrationView.fxml");
            if (fxmlUrl == null) {
                throw new IOException("无法找到 /org/example/client/view/RegistrationView.fxml，请检查 FXML 是否存在");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            org.example.client.controller.RegistrationController controller = loader.getController();
            controller.setMainApp(this);

            // 新建一个独立的注册窗口
            Stage registrationStage = new Stage();
            registrationStage.setTitle("User Registration");
            registrationStage.initOwner(primaryStage);
            registrationStage.setScene(new Scene(root));

            // 把 Stage 传给 controller，以便在 Controller 中关闭窗口
            controller.setDialogStage(registrationStage);

            registrationStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            org.example.util.AlertUtil.showError(
                    "Application Error",
                    "Could not load the registration screen.",
                    e.getMessage()
            );
        }
    }

    public void showDashboard(User user) {
        try {
            // 根据角色选择对应的 FXML 绝对路径
            String fxmlPath = (user.getRole() == Role.ADMIN)
                    ? "/org/example/client/view/AdminDashboardView.fxml"
                    : "/org/example/client/view/UserDashboardView.fxml";

            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("无法找到 " + fxmlPath + "，请检查 FXML 是否存在");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            if (user.getRole() == Role.ADMIN) {
                org.example.client.controller.AdminDashboardController controller =
                        loader.getController();
                controller.setMainApp(this);
            } else {
                org.example.client.controller.UserDashboardController controller =
                        loader.getController();
                controller.setMainApp(this);
            }

            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle(
                    "Library Dashboard - " + user.getUsername() + " (" + user.getRole() + ")"
            );
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            org.example.util.AlertUtil.showError(
                    "Application Error",
                    "Could not load the dashboard.",
                    e.getMessage()
            );
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
