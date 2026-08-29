package io.izzel.arclight.server;

import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * Подключение authlib-injector без флага {@code -javaagent} в строке запуска.
 * <p>
 * Начиная с Java 9 JVM читает из манифеста запускаемого jar атрибут
 * {@code Launcher-Agent-Class} и вызывает у указанного класса {@code agentmain}
 * до того, как стартует {@code main}. Это позволяет поднять агент на хостингах,
 * где параметры запуска править нельзя.
 * <p>
 * Настройка — файл {@code authlib-injector.properties} в рабочей папке сервера:
 * <pre>
 * url=https://api.example.com/authlib
 * jar=authlib-injector.jar
 * </pre>
 * либо переменная окружения {@code SC_AUTHLIB_URL}. Если ничего не задано,
 * класс молча завершается и поведение сервера не меняется.
 * <p>
 * Любая ошибка здесь перехватывается: исключение из {@code agentmain} заставило бы
 * JVM прервать запуск, а терять сервер из-за проблем с авторизацией недопустимо.
 */
public final class AuthlibAgent {

    private static final String CONFIG_FILE = "authlib-injector.properties";
    private static final String ENV_URL = "SC_AUTHLIB_URL";
    private static final String DEFAULT_JAR = "authlib-injector.jar";
    private static final String PREMAIN_CLASS = "moe.yushi.authlibinjector.Premain";

    private AuthlibAgent() {
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        try {
            load(instrumentation);
        } catch (Throwable t) {
            // сервер обязан подняться в любом случае - авторизация просто не заработает
            System.err.println("[SC] Не удалось подключить authlib-injector: " + t);
        }
    }

    private static void load(Instrumentation instrumentation) throws Exception {
        Properties properties = readConfig();
        String url = properties.getProperty("url", "").trim();
        if (url.isEmpty()) {
            url = orEmpty(System.getenv(ENV_URL)).trim();
        }
        if (url.isEmpty()) {
            return;
        }

        Path jar = Paths.get(properties.getProperty("jar", DEFAULT_JAR).trim());
        if (!Files.isRegularFile(jar)) {
            System.err.println("[SC] authlib-injector включён в " + CONFIG_FILE
                + ", но файл " + jar.toAbsolutePath() + " не найден - авторизация работать не будет.");
            return;
        }

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            instrumentation.appendToSystemClassLoaderSearch(jarFile);
        }

        Class<?> premain = Class.forName(PREMAIN_CLASS, true, ClassLoader.getSystemClassLoader());
        Method agentmain = premain.getMethod("agentmain", String.class, Instrumentation.class);
        agentmain.invoke(null, url, instrumentation);
        System.out.println("[SC] authlib-injector подключён из манифеста ядра: " + url);
    }

    private static Properties readConfig() {
        Properties properties = new Properties();
        Path path = Paths.get(CONFIG_FILE);
        if (Files.isRegularFile(path)) {
            try (InputStream stream = Files.newInputStream(path)) {
                properties.load(stream);
            } catch (Exception e) {
                System.err.println("[SC] Не удалось прочитать " + CONFIG_FILE + ": " + e);
            }
        }
        return properties;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
