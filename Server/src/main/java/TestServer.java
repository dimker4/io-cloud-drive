import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TestServer {
    private final static int SERVER_PORT = 8189;

    TestServer () {
        try {
            // серверсокет
            AuthService.connect();
            ServerSocket server = new ServerSocket(SERVER_PORT);
            System.out.println("Server listening ...");
            while (true) {
                //сокет для общения
                Socket clientSocket = server.accept();
                System.out.println("Client connected!");
                // для каждого клиента создаем отдельный обработчик
                new ClientHandler(this, clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
