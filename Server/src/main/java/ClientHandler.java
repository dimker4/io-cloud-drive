import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientHandler {

    public ClientHandler(TestServer server, Socket socket) {

        System.out.println("Client handler created!");
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            // От клиента всегда будет приходить объект CommonObj, это либо команда, либо файл
                            CommonObj co = (CommonObj) ois.readObject();
                            if (co instanceof FileWrap) {
                                // Если файл то создаем его на диске
                                FileWrap file = (FileWrap) co;
                                // Создаем физический файл на диске
                                creteFile(file);
                            } else {
                                // Если команда, то проверяем какая именно
                                Command command = (Command) co;
                                System.out.println("is command");
                                switch (command.getCommandName()) {
                                    case "del": {
                                        System.out.println("del " + command.getFileName());
                                        delFile(command.getFileName());
                                        break;
                                    }
                                    case "rename": {
                                        System.out.println("rename " + command.getFileName());
                                        renameFile(command.getFileName(), command.getNewFileName());
                                        break;
                                    }
                                    case "dir": {
                                        System.out.println("view all dirs");
                                        TextAnswer text = new TextAnswer(getCoolFiles());
                                        out.writeObject(text);
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void creteFile (FileWrap file) {
        // Создаю файл на диске из массива байтов
        try (FileOutputStream fs = new FileOutputStream("server_dir/"+file.getFileName())) {
            fs.write(file.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void delFile (String fileName) {
        try {
            Files.deleteIfExists(Path.of("server_dir", fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void renameFile (String fileName, String newFileName) {
        File f = new File("server_dir/" + fileName);
        f.renameTo(new File("server_dir/" + newFileName));
    }

    public static String getCoolFiles () {
        try (Stream<Path> walk = Files.walk(Paths.get("server_dir/"))) {

            List<String> resultList = walk.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());

            StringBuilder result = new StringBuilder();
            for (String s: resultList ) {
                result.append(s).append("\r\n");
            }
            return result.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
