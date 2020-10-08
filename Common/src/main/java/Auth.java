import java.io.Serializable;

public class Auth extends CommonObj implements Serializable {
    private String login = "";
    private String password = "";

    public Auth(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
