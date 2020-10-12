import java.sql.*;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC"); // Инициализация драйвера
            connection = DriverManager.getConnection("jdbc:sqlite:main.db");
            stmt = connection.createStatement(); // Создали стейтмент, с помощью него выполняем запросы
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getNickByAuth (String login, String pass) {
        String sql = String.format("SELECT login " +
                "FROM users " +
                "WHERE login = '%s' " +
                "AND password = '%s'", login, pass);
        try {
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
