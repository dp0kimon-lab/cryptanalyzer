import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CaesarCipherTool {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static final String ALPHABET_UPPER = ALPHABET.toUpperCase();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            printMenu();
            int choice = getIntInput(scanner, "Выберите действие: ", 0, 5);

            switch (choice) {
                case 1 -> encryptText(scanner);
                case 2 -> decryptWithKey(scanner);
                case 3 -> bruteForceDecrypt(scanner);
                case 4 -> statisticalDecrypt(scanner);
                case 5 -> processFile(scanner);
                case 0 -> {
                    System.out.println("До свидания!");
                    scanner.close();
                    return;
                }
            }
        }
    }

    private static void printMenu() {
        System.out.println("""
                
                === Caesar Cipher Tool ===
                1. Зашифровать текст
                2. Расшифровать с известным ключом
                3. Brute force расшифровка
                4. Статистический анализ (автоподбор ключа)
                5. Работа с файлом
                0. Выход
                """);
    }

    private static void encryptText(Scanner scanner) {
        System.out.print("Введите текст для шифрования: ");
        String text = scanner.nextLine();
        int shift = getIntInput(scanner, "Введите ключ (сдвиг): ", 0, 25);

        String result = caesarCipher(text, shift);
        System.out.println("Зашифрованный текст: " + result);
        System.out.println("Hex-представление: " + toHex(result));
    }

    private static void decryptWithKey(Scanner scanner) {
        System.out.print("Введите зашифрованный текст: ");
        String text = scanner.nextLine();
        int shift = getIntInput(scanner, "Введите ключ (сдвиг): ", 0, 25);

        String result = caesarCipher(text, 26 - (shift % 26));
        System.out.println("Расшифрованный текст: " + result);
    }

    private static void bruteForceDecrypt(Scanner scanner) {
        System.out.print("Введите зашифрованный текст: ");
        String text = scanner.nextLine();

        System.out.println("\n=== Результаты brute force (ключи 0-25) ===");
        for (int key = 0; key < 26; key++) {
            String decrypted = caesarCipher(text, 26 - key);
            System.out.printf("Ключ %2d: %s%n", key, truncate(decrypted));
        }

        System.out.print("\nВведите ключ, который выглядит правильным (или 0 для пропуска): ");
        int selectedKey = getIntInput(scanner, "", 0, 25);
        if (selectedKey > 0) {
            System.out.println("Полный текст: " + caesarCipher(text, 26 - selectedKey));
        }
    }

    private static void statisticalDecrypt(Scanner scanner) {
        System.out.print("Введите зашифрованный текст: ");
        String text = scanner.nextLine();

        int bestKey = findBestKeyByFrequency(text);
        System.out.println("Автоматически подобранный ключ: " + bestKey);

        String decrypted = caesarCipher(text, 26 - bestKey);
        System.out.println("Расшифрованный текст: " + decrypted);

        System.out.print("Устраивает результат? (да/нет): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("нет")) {
            bruteForceDecrypt(scanner);
        }
    }

    private static void processFile(Scanner scanner) {
        System.out.print("Введите путь к входному файлу: ");
        String inputPath = scanner.nextLine();

        if (!Files.exists(Path.of(inputPath))) {
            System.out.println("Ошибка: файл не существует!");
            return;
        }

        System.out.print("Введите путь к выходному файлу: ");
        String outputPath = scanner.nextLine();

        System.out.print("Выберите режим (1 - шифрование, 2 - расшифровка с ключом, 3 - brute force): ");
        int mode = getIntInput(scanner, "", 1, 3);

        try {
            String content = Files.readString(Path.of(inputPath));
            String result;

            if (mode == 1) {
                int shift = getIntInput(scanner, "Введите ключ (0-25): ", 0, 25);
                result = caesarCipher(content, shift);
            } else if (mode == 2) {
                int shift = getIntInput(scanner, "Введите ключ (0-25): ", 0, 25);
                result = caesarCipher(content, 26 - (shift % 26));
            } else {
                // brute force - сохраняем все варианты
                StringBuilder sb = new StringBuilder();
                for (int key = 0; key < 26; key++) {
                    sb.append("Ключ ").append(key).append(":\n");
                    sb.append(caesarCipher(content, 26 - key)).append("\n\n");
                }
                result = sb.toString();
            }

            Files.writeString(Path.of(outputPath), result);
            System.out.println("Результат сохранён в " + outputPath);

        } catch (IOException e) {
            System.out.println("Ошибка при работе с файлом: " + e.getMessage());
        }
    }

    // Основной алгоритм шифра Цезаря (только буквы A-Z, a-z)
    private static String caesarCipher(String text, int shift) {
        shift = shift % 26;
        if (shift < 0) shift += 26;

        StringBuilder result = new StringBuilder();

        for (char ch : text.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                int originalPos = ALPHABET_UPPER.indexOf(ch);
                if (originalPos != -1) {
                    int newPos = (originalPos + shift) % 26;
                    result.append(ALPHABET_UPPER.charAt(newPos));
                } else {
                    result.append(ch);
                }
            } else if (Character.isLowerCase(ch)) {
                int originalPos = ALPHABET.indexOf(ch);
                if (originalPos != -1) {
                    int newPos = (originalPos + shift) % 26;
                    result.append(ALPHABET.charAt(newPos));
                } else {
                    result.append(ch);
                }
            } else {
                result.append(ch); // сохраняем пробелы, знаки препинания, цифры
            }
        }

        return result.toString();
    }

    // Статистический анализ: подбор ключа по частоте букв в английском языке
    private static int findBestKeyByFrequency(String text) {
        // Частоты букв в английском языке (от a до z)
        double[] englishFreq = {
                0.08167, 0.01492, 0.02782, 0.04253, 0.12702, 0.02228, 0.02015, // a-g
                0.06094, 0.06966, 0.00153, 0.00772, 0.04025, 0.02406, 0.06749, // h-n
                0.07507, 0.01929, 0.00095, 0.05987, 0.06327, 0.09056, 0.02758, // o-u
                0.00978, 0.02360, 0.00150, 0.01974, 0.00074                      // v-z
        };

        int[] letterCount = new int[26];
        int totalLetters = 0;

        for (char ch : text.toCharArray()) {
            if (Character.isLetter(ch)) {
                char lower = Character.toLowerCase(ch);
                int index = lower - 'a';
                if (index >= 0 && index < 26) {
                    letterCount[index]++;
                    totalLetters++;
                }
            }
        }

        if (totalLetters == 0) return 0;

        double[] observedFreq = new double[26];
        for (int i = 0; i < 26; i++) {
            observedFreq[i] = (double) letterCount[i] / totalLetters;
        }

        // Ищем ключ, максимизирующий корреляцию с эталонными частотами
        double bestScore = -1;
        int bestKey = 0;

        for (int shift = 0; shift < 26; shift++) {
            double score = 0;
            for (int i = 0; i < 26; i++) {
                int shiftedIndex = (i + shift) % 26;
                score += observedFreq[shiftedIndex] * englishFreq[i];
            }
            if (score > bestScore) {
                bestScore = score;
                bestKey = shift;
            }
        }

        return bestKey;
    }
    
    private static int getIntInput(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("Введите число от %d до %d%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("Пожалуйста, введите целое число");
            }
        }
    }

    private static String toHex(String text) {
        StringBuilder hex = new StringBuilder();
        for (char ch : text.toCharArray()) {
            hex.append(String.format("%04x ", (int) ch));
        }
        return hex.toString().trim();
    }

    private static String truncate(String text) {
        if (text.length() <= 60) return text;
        return text.substring(0, 60 - 3) + "...";
    }
}