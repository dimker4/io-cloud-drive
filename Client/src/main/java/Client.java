import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;

public class Client {
    public final static int SERVER_PORT = 8189;
    public final static String SERVER_ADDR  = "127.0.0.1";
    private static Socket socket;
    private static boolean running = true;

    public static void main(String[] args) throws IOException {
        try {
            socket = new Socket(SERVER_ADDR , SERVER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            Thread t1 = new Thread (new Runnable() { // Отдельный поток для чтения сообщений от сервера
                @Override
                public void run() {
                    while (running) {
                        try {
                            // Получаем объект - Родителький класс для всех сущностей, которые могут придти с сервера
                            CommonObj co = (CommonObj) in.readObject();
                            // Пока что с сервера приходят только текстовые ответы, файлы будут приходить позже :)
                            if (co instanceof TextAnswer) {
                                TextAnswer answer = (TextAnswer) co;
                                System.out.println(answer.getText());
                            } else if (co instanceof FileWrap){
                                FileWrap fw = (FileWrap) co;
                                creteFile(fw);
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            System.out.println("Соединение закрыто! ");
                        } finally {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });

            // Цикл для авторизации
            // Данные в базе для проверки:
            //===============
            // login: john
            // password: 123
            //==============
            Scanner sca = new Scanner(System.in);
            while (true) {
                System.out.print("Введи логин: ");
                String login = sca.nextLine();
                System.out.print("Введи пароль: ");
                String password = sca.nextLine();
                // Создаем объект авторизации и отправляем на сервер
                Auth auth = new Auth(login, password);
                out.writeObject(auth);

                // Получаем ответ от сервера
                CommonObj co = (CommonObj) in.readObject();
                TextAnswer answer = (TextAnswer) co;
                if (answer.getText().equals("authok")) {
                    // Если пришел положительный ответ, то запускаем постоянный поток чтения и выходим из цикла
                    t1.start();
                    // Делаю так, что бы на клиенте не было признака авторизации, что бы авторизация была только на основании ответа сервера
                    break;
                }
                System.out.println("Пользовтель не авторизован, попробуй еще");
            }

            // В основном потоке будем отправлять сообщения серверу
            // Для простоты отладки, все файлы клиента лежат в client_dir
            System.out.println();
            System.out.println("Введи команду (send, del, rename, copy, dir, selfdir): ");
            System.out.println("send <имя_файла>  - Для отправки файла на сервер");
            System.out.println("del <имя_файла>  - Для удаления Файла с сервера");
            System.out.println("rename <имя_файла> <новое_имя_файла>  - Для переименования файла на сервере");
            System.out.println("copy <имя_файла>  - Для копирования файла с сервера в свою директорию");
            System.out.println("dir - Для получения списка файлов на сервере");
            System.out.println("selfdir - Для получения списка файлов своей директории");
            System.out.println("Пример: -> send 1.txt");

            while (true) {
                try {
                    sleep(300);
                    System.out.print("-> ");
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
                        case "del":
                        case "copy": {
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
                        case "selfdir": {
                            System.out.println("Список файлов в своей директории");
                            System.out.println(getFilesList());
                            break;
                        }
                        case "exit": {
                            System.out.println("Закрываем соединение");
                            running = false;
                            sleep(300);
                            com = new Command(outputSplit[0], null, null);
                            out.writeObject(com);
                            return;
                        }
                        default: {
                            // Во всех остальных случаях ругаемся
                            System.out.println("Неизвестная команда, попробуй еще!");
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finally {
                    socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            socket.close();
        }
    }

    public static void creteFile (FileWrap file) {
        // Создаю файл на диске из массива байтов
        try (FileOutputStream fs = new FileOutputStream("client_dir/"+file.getFileName())) {
            fs.write(file.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getFilesList() {
        try (Stream<Path> walk = Files.walk(Paths.get("client_dir/"))) {

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
