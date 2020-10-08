import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientHandler extends Thread {
    private static String nickname;
    // Список комманд, которые приходят с аргументом в виде имена файла
    // Список нужен для проверки существования файла
    private static ArrayList<String> fileCom = new ArrayList<>() {{
        add("del");
        add("rename");
        add("copy");
    }};

    public ClientHandler(TestServer server, Socket socket) {

        System.out.println("Client handler created!");
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean isAuthOK = false;
                        while (!isAuthOK) {
                            CommonObj co = (CommonObj) ois.readObject();
                            if (co instanceof Auth) {
                                Auth auth = (Auth) co;
                                System.out.println("is command");
                                nickname = AuthService.getNickByAuth(auth.getLogin(), auth.getPassword());
                                if (nickname == null) {
                                    TextAnswer answer = new TextAnswer("authfailed");
                                    out.writeObject(answer);
                                } else {
                                    // В случае успешной авторизации, отправляем клиенту, что все ок
                                    isAuthOK = true;
                                    TextAnswer answer = new TextAnswer("authok");
                                    out.writeObject(answer);
                                    // И создадим папку на сервере, если её еще нет
                                    createUserDir(nickname);
                                }
                            } else {
                                TextAnswer answer = new TextAnswer("Сначала необходимо авторизоваться!");
                                out.writeObject(answer);
                            }
                        }

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

                                // При получении команды обязательно проверим, что файл существует
                                if (fileCom.contains(command.getCommandName())) {
                                    if (!checkFileExists(command.getFileName())) {
                                        // Если файла не существует, то сообщаем пользователю и уходим на новую итерацию
                                        TextAnswer text = new TextAnswer("Файл на сервере не существует");
                                        out.writeObject(text);
                                        continue;
                                    }
                                }

                                System.out.println("is command");
                                switch (command.getCommandName()) {
                                    case "del": {
                                        System.out.println("del " + command.getFileName());
                                        delFile(command.getFileName());
                                        TextAnswer text = new TextAnswer("Файл " + command.getFileName() + " удален");
                                        out.writeObject(text);
                                        break;
                                    }
                                    case "rename": {
                                        System.out.println("rename " + command.getFileName());
                                        TextAnswer text = new TextAnswer("Файл " + command.getFileName() + " переименован в " + command.getNewFileName());
                                        renameFile(command.getFileName(), command.getNewFileName());
                                        break;
                                    }
                                    case "dir": {
                                        System.out.println("view all dirs");
                                        TextAnswer text = new TextAnswer(getFilesList());
                                        out.writeObject(text);
                                        break;
                                    }
                                    case "copy": {
                                        System.out.println("copy file" + command.getFileName());
                                        FileWrap fw = new FileWrap(Paths.get("server_dir/", nickname,command.getFileName()));
                                        out.writeObject(fw);
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
        try (FileOutputStream fs = new FileOutputStream("server_dir/"+ nickname +"/"+file.getFileName())) {
            fs.write(file.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void delFile (String fileName) {
        try {
            Files.deleteIfExists(Path.of("server_dir", nickname, fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void renameFile (String fileName, String newFileName) {
        File f = new File("server_dir/"+ nickname +"/"+fileName);
        f.renameTo(new File("server_dir/"+ nickname +"/"+newFileName));
    }

    public static String getFilesList() {
        try (Stream<Path> walk = Files.walk(Paths.get("server_dir/"+ nickname +"/"))) {

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

    public static void createUserDir (String nickname) {
        File directory = new File("server_dir/" + nickname);
        if (! directory.exists()){
            directory.mkdir();
        }
    }

    public static boolean checkFileExists(String filename) {
        File file = new File("server_dir/" + nickname + "/" + filename);
        return file.exists();
    }
}
