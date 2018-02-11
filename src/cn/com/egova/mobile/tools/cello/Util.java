package cn.com.egova.mobile.tools.cello;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by withparadox2 on 2018/2/7.
 */
public class Util {
    public static boolean exists(VirtualFile file) {
        return file != null && file.exists();
    }

    public static void toast(Project project, String text) {
        toast(project, MessageType.INFO, text);
    }

    public static void toast(Project project, MessageType type, String text) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, type, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
    }

    public static void alert(Project project, String text) {
        Messages.showMessageDialog(project, text, "提示", Messages.getInformationIcon());
    }

    public static void alert(Project project, String title, String text) {
        Messages.showMessageDialog(project, text, title, Messages.getInformationIcon());
    }

    public static List<String> readFileToLines(VirtualFile file) {
        List<String> lineList = new ArrayList<>();
        if (file.exists()) {
            BufferedReader reader = null;
            try {
                InputStream inputStream = file.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    lineList.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeSilently(reader);
            }
        }
        return lineList;
    }

    public static void writeLinesToFile(List<String> lineList, VirtualFile file) {
        if (isEmpty(lineList)) {
            return;
        }
        String lineSeparator = System.getProperty("line.separator");

        StringBuilder sb = new StringBuilder();
        for (String str : lineList) {
            sb.append(str);
            sb.append(lineSeparator);
        }
        writeStringToFile(sb.toString(), file);
    }

    public static void writeStringToFile(String text, VirtualFile file) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    file.setBinaryContent(text.getBytes("utf-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.size() == 0;
    }

    public static boolean canIgnore(String line) {
        return isEmpty(line) || isComment(line);
    }

    public static boolean isEmpty(String line) {
        return line == null || line.trim().length() == 0;
    }

    private static Pattern identifierPattern = Pattern.compile("[a-zA-Z]+[a-zA-Z0-9_-]*");
    public static boolean isIdentifier(String line) {
        return identifierPattern.matcher(line).matches();
    }

    public static boolean isComment(String line) {
        return line.trim().startsWith("//");
    }

    public static String comment(String line) {
        return "//" + unComment(line);
    }

    public static String unComment(String line) {
        return line.replaceFirst("^\\s*//", "");
    }

    public static boolean equals(String left, String right) {
        if (left == null && right == null) {
            return true;
        } else if (left != null && right != null) {
            return left.equals(right);
        } else {
            return false;
        }
    }
}
