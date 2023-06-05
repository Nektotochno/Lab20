import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class Client extends JFrame {
    private final String SERVER_ADDR = "localhost";
    private final int SERVER_PORT = 8189;
    private JTextField msgInputField;
    private JTextArea chatArea;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private JButton authBtn;

    public Client() {
        try {
            openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        prepareGUI();
        createAuthPanel();
    }

    public void openConnection() throws IOException {
        socket = new Socket(SERVER_ADDR, SERVER_PORT);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                while (true) {
                    String strFromServer = in.readUTF();
                    if (strFromServer.startsWith("/authok")) {
                        nickname = strFromServer.split("\\s")[1];
                        break;
                    }
                    appendMessage(strFromServer);
                }
                while (true) {
                    String strFromServer = in.readUTF();
                    if (strFromServer.equalsIgnoreCase("/end")) {
                        break;
                    }
                    appendMessage(strFromServer);
                }
            } catch (EOFException eofException) {
                // Connection closed
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void closeConnection() {
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage() {
        if (!msgInputField.getText().trim().isEmpty()) {
            try {
                out.writeUTF(msgInputField.getText());
                msgInputField.setText("");
                msgInputField.grabFocus();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка отправки сообщения");
            }
        }
    }

    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    private void createAuthPanel() {
        JPanel authPanel = new JPanel(new GridLayout());
        JTextField login = new JTextField();
        JPasswordField password = new JPasswordField();
        JButton authBtn = new JButton("Авторизация");
        authPanel.add(login);
        authPanel.add(password);
        authPanel.add(authBtn);
        add(authPanel, BorderLayout.NORTH);

        authBtn.addActionListener(e -> {
            String enteredLogin = login.getText();
            String enteredPassword = new String(password.getText());
            sendAuthMessage(enteredLogin, enteredPassword);
        });
    }

    private void sendAuthMessage(String login, String password) {
        try {
            out.writeUTF("/auth " + login + " " + password);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareGUI() {
        setBounds(600, 300, 500, 500);
        setTitle("Клиент");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JButton btnSendMsg = new JButton("Отправить");
        bottomPanel.add(btnSendMsg, BorderLayout.EAST);
        msgInputField = new JTextField();
        add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.add(msgInputField, BorderLayout.CENTER);

        btnSendMsg.addActionListener(e -> sendMessage());
        msgInputField.addActionListener(e -> sendMessage());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                outMessage("/end");
                closeConnection();
            }
        });

        setVisible(true);
    }

    private void outMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect(String nickname) {
        this.nickname = nickname;
        setTitle("Клиент - " + nickname);
        authBtn.setEnabled(false);
        msgInputField.setEnabled(true);
        msgInputField.requestFocus();
        appendMessage("Вы вошли в чат под ником: " + nickname);
        outMessage("/authok " + nickname);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
