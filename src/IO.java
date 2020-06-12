import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.System.out;

public class IO {
    static int running = 0;

    static ArrayList<String> loadRules(File home, String path) {
        Pattern patRule = Pattern.compile("(\\[.+?])(?:\\nSOURCE:\\n.+)?\\nTARGET:\\n[\\s\\S]+?\\[/.+?]", Pattern.MULTILINE);
        if (Main.rules_mode == 0) {
            out.println();
        }
        String content, f = "";
        ArrayList<String> rulesListArr = new ArrayList<>();
        out.println("Loading rules...");
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(path))) {
            ZipEntry entry;        //Текущий файл
            while ((entry = zip.getNextEntry()) != null) {
                FileOutputStream fout = new FileOutputStream(home + File.separator + "temp" + File.separator + entry.getName());
                for (int c = zip.read(); c != -1; c = zip.read()) {
                    fout.write(c);
                }
                fout.flush();
                zip.closeEntry();
                fout.close();
            }
            f = home + File.separator + "temp" + File.separator + "patch.txt";
            File fil = new File(f);
            if (!fil.exists()) {
                out.println("No patch.txt file in patch!");
                System.exit(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        content = IO.read(f);
        Matcher ruleMatched = patRule.matcher(content);
        while (ruleMatched.find()) {
            for (int i = 0; i < ruleMatched.groupCount(); i++) {
                rulesListArr.add(ruleMatched.group(i));
            }
        }
        out.println(rulesListArr.size() + " rules found\n");
        return rulesListArr;
    }

    static String read(String path) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[8192];
            int length;
            assert inputStream != null;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    static synchronized void write(String path, String content) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path));
            bufferedWriter.write(content);
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static synchronized void copy(String src, String dst) {
        try (InputStream is = new FileInputStream(src); OutputStream os = new FileOutputStream(dst)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
        } catch (IOException e) {
            out.println("Hmm.. error during copying file. No smali folders?");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static synchronized void deleteInDirectory(File file) {
        if (file.isDirectory()) {
            if (Objects.requireNonNull(file.list()).length == 0) {
                file.delete();
            } else {
                String[] files = file.list();
                assert files != null;
                for (String temp : files) {
                    File fileDelete = new File(file, temp);
                    deleteInDirectory(fileDelete);
                }
                if (Objects.requireNonNull(file.list()).length == 0) file.delete();
            }
        } else file.delete();
    }

    static void scan(String projectPath) {
        Object object = new Object();
        ExecutorService service = Executors.newFixedThreadPool(16);
            for (String i : Objects.requireNonNull(new File(projectPath).list())) {
                if (i.startsWith("smali")) {
                    running++;
                    Runnable task = () -> {
                        Stack<String> stack = new Stack<>();
                        stack.push(projectPath + File.separator + i);
                        while (!stack.isEmpty()) {
                            for (File file : Objects.requireNonNull(new File(stack.pop()).listFiles())){
                                String str = file.toString();
                                if (file.isDirectory()) stack.push(str);
                                else {
                                    synchronized (object) {
                                        Regex.projectSmaliList.add(str);
                                        Regex.projectSmaliText.add(IO.read(str));
                                    }
                                }
                            }
                        }
                        IO.running--;
                    };
                    service.submit(task);
                }
            }

        while (running!=0) {  //  В - велосипед =D
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}