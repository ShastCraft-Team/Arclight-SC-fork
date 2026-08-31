package io.izzel.arclight.server;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Стартовый ANSI-баннер ShastCraft (порт под sourceSet applaunch, Java 7).
 *
 * Логотип — чистый ASCII: одинаково рендерится в любой консоли (cmd, PowerShell, Linux) вне
 * зависимости от кодовой страницы. Цвет — ANSI, отключается флагом {@code -Dsc.color=false}
 * или переменной окружения {@code NO_COLOR} (см. no-color.org).
 *
 * Данные о версии/лоадере читаются из манифеста своего же jar (атрибуты {@code SC-*} и
 * {@code Implementation-Version}, которые проставляет задача {@code jar} в build.gradle). Если
 * манифест недоступен (запуск из среды разработки) — соответствующие поля пропускаются.
 *
 * ВАЖНО: sourceSet applaunch компилируется под Java 7 — никаких java.time/lambda/String.repeat.
 */
final class Banner {

    private Banner() {
    }

    /** ASCII escape (0x1B) как строка — во избежание невидимых символов в исходнике. */
    private static final String ESC = String.valueOf((char) 27);

    private static final boolean COLOR =
            Boolean.parseBoolean(System.getProperty("sc.color", "true"))
                    && System.getenv("NO_COLOR") == null;

    // figlet "ShastCraft"; каждая строка отрисовывается отдельно с отступом.
    private static final String[] ART = {
            " ____   _   _     _     ____   _____   ____  ____      _     _____  _____ ",
            "/ ___| | | | |   / \\   / ___| |_   _| / ___||  _ \\    / \\   |  ___||_   _|",
            "\\___ \\ | |_| |  / _ \\  \\___ \\   | |  | |    | |_) |  / _ \\  | |_     | |  ",
            " ___) ||  _  | / ___ \\  ___) |  | |  | |___ |  _ <  / ___ \\ |  _|    | |  ",
            "|____/ |_| |_|/_/   \\_\\|____/   |_|   \\____||_| \\_\\/_/   \\_\\|_|      |_|  ",
    };

    private static String esc(String code, String s) {
        return COLOR ? ESC + "[" + code + "m" + s + ESC + "[0m" : s;
    }

    private static String logo(String s) {
        return esc("38;5;75", s); // мягкий голубой
    }

    private static String accent(String s) {
        return esc("36", s); // cyan
    }

    private static String bold(String s) {
        return esc("1", s);
    }

    private static String dim(String s) {
        return esc("2", s);
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String field(String name, String value) {
        return accent(pad(name, 10)) + pad(value == null ? "n/a" : value, 28);
    }

    static void print() {
        Attributes attr = manifest();
        String mc = value(attr, "SC-MC");
        String version = shortenVersion(value(attr, "Implementation-Version"), mc);
        String loader = value(attr, "SC-Loader");
        String loaderVersion = value(attr, "SC-Loader-Version");
        // дата сборки: раньше её показывал логотип Arclight, теперь её больше негде взять
        String built = value(attr, "Implementation-Timestamp");

        long maxHeapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        String java = System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")";
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version");
        String started = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String loaderLabel = loader == null ? "Forge" : loader;
        String sep = dim("  " + repeat('-', 60));

        StringBuilder sb = new StringBuilder("\n");
        for (String line : ART) {
            sb.append("  ").append(logo(line)).append('\n');
        }
        sb.append("  ")
                .append(bold("ShastCraft"))
                .append(dim("  Hybrid Minecraft Server Kernel   |   " + loaderLabel + " mods + Bukkit/Spigot plugins"))
                .append('\n');
        sb.append(sep).append('\n');
        sb.append("  ").append(field("Version", version)).append(field("Minecraft", mc)).append('\n');
        sb.append("  ").append(field(loaderLabel, loaderVersion)).append(field("Max heap", maxHeapMb + " MB")).append('\n');
        sb.append("  ").append(field("Java", java)).append(field("Built", built)).append('\n');
        sb.append("  ").append(field("OS", os)).append(field("Started", started)).append('\n');
        sb.append(sep).append('\n');
        System.out.println(sb);
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String value(Attributes attr, String key) {
        if (attr == null) {
            return null;
        }
        String v = attr.getValue(key);
        return (v == null || v.isEmpty()) ? null : v;
    }

    /**
     * Убирает префикс {@code arclight-<mc>-} из полной версии, чтобы в баннере остался только
     * номер сборки (напр. {@code arclight-1.20.1-1.0.6-SNAPSHOT-6de9fec} -> {@code 1.0.6-SNAPSHOT-6de9fec}).
     * Версия игры и так показывается отдельным полем «Minecraft».
     */
    private static String shortenVersion(String version, String mc) {
        if (version == null) {
            return null;
        }
        if (mc != null) {
            String prefix = "arclight-" + mc + "-";
            if (version.startsWith(prefix)) {
                return version.substring(prefix.length());
            }
        }
        if (version.startsWith("arclight-")) {
            return version.substring("arclight-".length());
        }
        return version;
    }

    /** Находит манифест нашего jar (Implementation-Title = ShastCraft) среди доступных на classpath. */
    private static Attributes manifest() {
        try {
            Enumeration<URL> resources = Banner.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                InputStream in = null;
                try {
                    in = url.openStream();
                    Manifest mf = new Manifest(in);
                    Attributes main = mf.getMainAttributes();
                    if ("ShastCraft".equals(main.getValue("Implementation-Title"))) {
                        return main;
                    }
                } catch (Exception ignored) {
                    // пропускаем нечитаемые манифесты
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // classpath недоступен — печатаем баннер без версий
        }
        return null;
    }
}
