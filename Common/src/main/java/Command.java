import java.io.Serializable;

public class Command extends CommonObj implements Serializable {
    private String commandName;
    private boolean isFSCommand = false;
    private String fileName;

    public String getNewFileName() {
        return newFileName;
    }

    private String newFileName;

    public Command(String commandName, String fileName, String newFileName) {
        this.commandName = commandName;
        this.fileName = fileName;
        this.newFileName = newFileName;
        isFSCommand = (fileName == null);
    }

    public String getFileName() {
        return fileName;
    }

    public String getCommandName() {
        return commandName;
    }

    public boolean isFSCommand() {
        return isFSCommand;
    }
}
