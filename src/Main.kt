package client

import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import java.io.IOException
import javax.swing.*
import java.nio.file.Files
import java.util.Base64

class ClientWindow : JFrame(), ActionListener, TCPConnectionListener {

    companion object {
        private const val IP_ADDR = "127.0.0.1" // Локальный IP-адрес сервера
        private const val PORT = 8189           // Порт сервера
        private const val WIDTH = 600           // Ширина окна
        private const val HEIGHT = 400          // Высота окна

        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                ClientWindow()
            }
        }
    }

    private val log = JTextArea()                 // Поле для отображения сообщений
    private val fieldNickname = JTextField("Гость") // Поле для ввода имени пользователя
    private val fieldInput = JTextField()         // Поле для ввода сообщения
    private var connection: TCPConnection? = null // Соединение TCP
    private val btnUploadImage = JButton("Загрузить изображение")  // Кнопка для загрузки изображения

    init {
        // Настройка основного окна
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(WIDTH, HEIGHT)
        setLocationRelativeTo(null)
        isAlwaysOnTop = true

        // Добавление компонентов интерфейса
        add(fieldNickname, BorderLayout.NORTH)

        log.isEditable = false
        log.lineWrap = true
        add(JScrollPane(log), BorderLayout.CENTER)

        fieldInput.addActionListener(this)
        add(fieldInput, BorderLayout.SOUTH)

        btnUploadImage.addActionListener(this) // Обработчик для кнопки загрузки
        add(btnUploadImage, BorderLayout.EAST)  // Кнопка на панели справа

        isVisible = true

        // Инициализация TCP-соединения
        try {
            connection = TCPConnection(this, IP_ADDR, PORT)
        } catch (e: IOException) {
            printMessage("Ошибка подключения: ${e.message}")
        }
    }

    // Обработка ввода сообщения и нажатия кнопки
    override fun actionPerformed(e: ActionEvent?) {
        if (e?.source == btnUploadImage) {
            uploadImage() // Если нажата кнопка "Загрузить изображение"
        } else {
            val msg = fieldInput.text
            if (msg.isBlank()) return
            fieldInput.text = null
            connection?.sendString("${fieldNickname.text}: $msg")
        }
    }

    // Открытие диалога для выбора изображения
    private fun uploadImage() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Выберите изображение"
        val result = fileChooser.showOpenDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            sendImage(selectedFile)  // Отправка выбранного изображения
        }
    }

    // Отправка изображения на сервер в виде Base64
    private fun sendImage(file: File) {
        try {
            val imageBytes = Files.readAllBytes(file.toPath())
            val base64Encoded = Base64.getEncoder().encodeToString(imageBytes)
            connection?.sendString("IMAGE:${file.name}:$base64Encoded")  // Отправка изображения
        } catch (e: IOException) {
            printMessage("Ошибка при чтении изображения: ${e.message}")
        }
    }

    // Обработчик событий подключения
    override fun onConnectionReady(tcpConnection: TCPConnection) {
        printMessage("Соединение установлено...")
    }

    override fun onReceiveString(tcpConnection: TCPConnection, value: String) {
        if (value.startsWith("IMAGE:")) {
            // Обработка получения изображения
            displayImage(value)  // Отображение изображения в чате
        } else {
            printMessage(value)  // Обычное сообщение
        }
    }

    // Отображение изображения в текстовом поле (для упрощения)
    private fun displayImage(encodedImage: String) {
        val parts = encodedImage.split(":", limit = 3)
        val imageName = parts[1]
        val imageUri = parts[2]  // Путь или URI к изображению

        // Показ изображения в чате
        SwingUtilities.invokeLater {
            log.append("Получено изображение: $imageName\n")
            val imageIcon = ImageIcon(imageUri) // Используем путь к изображению
            JOptionPane.showMessageDialog(this, JLabel(imageIcon), "Изображение", JOptionPane.PLAIN_MESSAGE)
        }
    }

    // Обработчик отключения
    override fun onDisconnect(tcpConnection: TCPConnection) {
        printMessage("Соединение закрыто")
    }

    // Обработчик исключений
    override fun onException(tcpConnection: TCPConnection, exception: IOException) {
        printMessage("Ошибка соединения: ${exception.message}")
    }

    // Синхронизированный вывод сообщений в лог
    private fun printMessage(message: String) {
        SwingUtilities.invokeLater {
            log.append("$message\n")
        }
    }
}



