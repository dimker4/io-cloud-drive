import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    public final static int SERVER_PORT = 8189;
    public final static String SERVER_ADDR  = "127.0.0.1";
    private static Socket socket;

    // Список команд доступных для отправки
    public static ArrayList<String> fileCom = new ArrayList<>() {{
        add("del");
        add("rename");
        add("copy");
        add("dir");
    }};

    public static void main(String[] args) {
        try {
            socket = new Socket(SERVER_ADDR , SERVER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            Thread t1 = new Thread (new Runnable() { // Отдельный поток для чтения сообщений от сервера
                @Override
                public void run() {
                    while (true) {
                        try {
                            // Получаем объект - Родителький класс для всех сущностей, которые могут придти с сервера
                            CommonObj co = (CommonObj) in.readObject();
                            // Пока что с сервера приходят только текстовые ответы, файлы будут приходить позже :)
                            if (co instanceof TextAnswer) {
                                TextAnswer answer = (TextAnswer) co;
                                System.out.println(answer.getText());
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });
            t1.start();

            // В основном потоке будем отправлять сообщения серверу
            // Для простоты отладки, все файлы клиента лежат в client_dir
            while (true) {
                System.out.println("Input command (send, del, rename, dir, copy)");
                try {
                    // Читаем строку из консоли
                    Scanner sc = new Scanner(System.in);
                    String output = sc.nextLine();
                    // Разбиваем по пробелу
                    String[] outputSplit = output.split(" ");
                    Command com;
                    // В зависимости от первого слова выбираем команду
                    switch (outputSplit[0]) {
                        // Для переименования нужное старое и новое имя
                        case "rename": {
                            com = new Command(outputSplit[0], outputSplit[1], outputSplit[2]);
                            out.writeObject(com);
                            break;
                        }
                        case "del": {
                            // Для удаления нужно только имя файла
                            com = new Command(outputSplit[0], outputSplit[1], null);
                            out.writeObject(com);
                            break;
                        }
                        case "send": {
                            // Для отправки создаем объект-обертку
                            FileWrap fw = new FileWrap(Paths.get("client_dir/", outputSplit[1]));
                            out.writeObject(fw);
                        }
                        case "dir": {
                            // Команда "dir" показывает все файлы, которые лежат в папке клиента на сервере
                            // Для простоты отладки вернет все файлы из папки server_dir
                            com = new Command(outputSplit[0], null, null);
                            out.writeObject(com);
                            break;
                        }
                        default: {
                            // Во всех остальных случаях ругаемся
                            System.out.println("Unknown command, try again!");
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
