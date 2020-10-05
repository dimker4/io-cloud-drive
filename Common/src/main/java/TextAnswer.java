import java.io.Serializable;

public class TextAnswer extends CommonObj implements Serializable {
    private String Text;

    public TextAnswer(String text) {
        Text = text;
    }

    public String getText() {
        return Text;
    }
}
