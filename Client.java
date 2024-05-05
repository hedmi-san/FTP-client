package ftpclient;
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Client {

    private String user;
    private String password;
    private Socket socket = null;
    private boolean DEBUG = true;
    private String host;
    private int port;
    private BufferedWriter writer;
    private BufferedReader reader;

    public Client(String ipAddress, int pPort) {
        port = pPort;
        host = ipAddress;
    }

    public void connect(String pUser, String pPassword) throws IOException {
        user = pUser;
        password = pPassword;

        if (socket != null) {
            throw new IOException("A FTP connection is already active");
        }

        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        String response = read();
        if (!response.startsWith("220")) {
            throw new IOException("FTP connection error:\n" + response);
        }

        send("USER " + user);
        response = read();
        if (!response.startsWith("331")) {
            throw new IOException("Error connecting with user account:\n" + response);
        }

        // Provide the password
        send("PASS " + password);
        response = read();
        if (!response.startsWith("230")) {
            throw new IOException("Error connecting with user account:\n" + response);
        }
    }

    public void send(String command) throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
        System.out.println("Command sent to server: " + command);
    }

    public String read() throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
            if (!reader.ready()) {
                break;
            }
        }
        return response.toString().trim();
    }

    public void disconnect() throws IOException {
        send("QUIT");
        socket.close();
        socket = null;
    }

    public void printWorkingDirectory() throws IOException {
        System.out.println("Current directory: C:\\Users\\LAPTOP SPIRIT\\Documents\\serverfolder");
    }

    public void changeWorkingDirectory(String directory) throws IOException {
        // No need to change directory as we are operating within the user's native directory
        System.out.println("Current directory: C:\\Users\\LAPTOP SPIRIT\\Documents\\serverfolder");
    }

    public void enterPassiveMode() throws IOException {
        send("PASV");
        System.out.println(read());
    }

    public void setAsciiMode() throws IOException {
        send("TYPE A");
        System.out.println(read());
    }

    public void listDirectory() throws IOException {
        setAsciiMode();
        enterPassiveMode();
        send("LIST");
        String response = read();
        if (response.startsWith("150")) {
            try (BufferedReader dirReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = dirReader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            response = read();
            if (!response.startsWith("226")) {
                throw new IOException("Error listing directory: " + response);
            }
        } else {
            throw new IOException("Error listing directory: " + response);
        }
    }

    public void retrieveFile(String remoteFilePath, String localFilePath) throws IOException {
        // Copy the file from the user's native directory to localFilePath
        String sourcePath = "C:\\Users\\LAPTOP SPIRIT\\Documents\\serverfolder\\" + remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1);
        File sourceFile = new File(sourcePath);
        File destinationFile = new File(localFilePath);
        Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("File downloaded successfully: " + remoteFilePath);
    }

    public void storeFile(String localFilePath, String remoteFilePath) throws IOException {
        // Copy the file from localFilePath to the user's native directory
        String destinationPath = "C:\\Users\\LAPTOP SPIRIT\\Documents\\serverfolder\\" + remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1);
        File sourceFile = new File(localFilePath);
        File destinationFile = new File(destinationPath);
        Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("File uploaded successfully: " + remoteFilePath);
    }

    public void deleteFile(String fileName) throws IOException {
        String filePath = "C:\\Users\\LAPTOP SPIRIT\\Documents\\serverfolder\\" + fileName;
        File file = new File(filePath);
        if (file.delete()) {
            System.out.println("File deleted successfully: " + fileName);
        } else {
            System.out.println("Failed to delete the file: " + fileName);
        }
    }

    public void readConsoleCommands() throws IOException {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        String command;

        while (true) {
            System.out.print("Enter FTP username: ");
            user = consoleReader.readLine();
            System.out.print("Enter FTP password: ");
            password = consoleReader.readLine();

            connect(user, password);

            while (true) {
                System.out.print("Enter an FTP command (QUIT to exit): ");
                command = consoleReader.readLine();

                if (command.equalsIgnoreCase("QUIT")) {
                    disconnect();
                    break;
                } else if (command.equalsIgnoreCase("PWD")) {
                    printWorkingDirectory();
                } else if (command.startsWith("CWD ")) {
                    String directory = command.substring(4);
                    changeWorkingDirectory(directory);
                } else if (command.equalsIgnoreCase("PASV")) {
                    enterPassiveMode();
                } else if (command.equalsIgnoreCase("LIST")) {
                    listDirectory();
                } else if (command.startsWith("RETR ")) {
                    String remoteFilePath = command.substring(5);
                    String localFilePath = "C:\\Users\\LAPTOP SPIRIT\\Documents\\ClientLocal\\" + remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1);
                    System.out.println("Attempting to retrieve file from remote path: " + remoteFilePath);
                    System.out.println("Saving to local file path: " + localFilePath);
                    retrieveFile(remoteFilePath, localFilePath);
                } else if (command.startsWith("STOR ")) {
                    String localFilePath = command.substring(5);
                    String remoteFilePath = "/" + localFilePath.substring(localFilePath.lastIndexOf("\\") + 1);
                    System.out.println("Attempting to send file from local path: " + localFilePath);
                    System.out.println("Saving to remote file path: " + remoteFilePath);
                    storeFile(localFilePath, remoteFilePath);
                } else if (command.startsWith("DELE ")) {
                    String remoteFilePath = command.substring(5);
                    deleteFile(remoteFilePath);
                }
            }
        }
    }

    public static void main(String[] args) {
        String host = "127.0.0.1"; // Provide the hostname or IP address of your FTP server
        int port = 21; // Default FTP port

        Client client = new Client(host, port);

        try {
            client.readConsoleCommands();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
