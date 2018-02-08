package cn.com.egova.mobile.tools.cello;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by withparadox2 on 2018/2/6.
 */
public class MainAction extends AnAction implements ICancelListener, IConfirmListener {
    protected static final Logger log = Logger.getInstance("CELLO");

    private static Pattern moduleNamePattern = Pattern.compile("['\"](:\\S*)['\"]");

    private VirtualFile settingsFile;
    private VirtualFile configFile;

    private List<ModuleElement> settingsModules;
    private Map<String, ModuleElement> nameToModule;

    private List<String> settingsLines;
    private List<String> configLines;

    private JFrame mDialog;

    private Project mProject;


    @Override
    public void actionPerformed(AnActionEvent event) {
        this.mProject = event.getData(PlatformDataKeys.PROJECT);

        VirtualFile file = mProject.getBaseDir();

        settingsFile = file.findChild("settings.gradle");
        if (!Util.exists(settingsFile)) {
            Util.toast(mProject, "settings.gradle不存在");
            return;
        }
        configFile = file.findChild("setting-plugins.gradle");
        if (!Util.exists(configFile)) {
            Util.toast(mProject, "setting-plugins.gradle不存在");
            return;
        }

        settingsLines = Util.readFileToLines(settingsFile);
        settingsModules = extractSettingsModules(settingsLines);

        configLines = Util.readFileToLines(configFile);
        extractConfigModules(configLines);

        showDialog();
    }

    private List<ModuleElement> extractSettingsModules(List<String> settingsLines) {
        nameToModule = new HashMap<>();
        List<ModuleElement> moduleList = new ArrayList<>();
        for (String line : settingsLines) {
            String name = getModuleName(line);
            if (name != null && !nameToModule.containsKey(name)) {
                ModuleElement element = new ModuleElement(name, !Util.isComment(line));
                element.setDisableCompile(ignoreForCompile(line));
                moduleList.add(element);
                nameToModule.put(name, element);
            }
        }

        Collections.sort(moduleList, new Comparator<ModuleElement>() {
            @Override
            public int compare(ModuleElement o1, ModuleElement o2) {
                return o1.lowerCaseName().compareTo(o2.lowerCaseName());
            }
        });

        return moduleList;
    }

    private boolean ignoreForCompile(String name) {
        return name.contains("eGovaMobile");
    }

    private void extractConfigModules(List<String> configLines) {
        boolean beginParse = false;
        for (String line : configLines) {
            if (beginParse) {
                if (Util.isIdentifier(line)) {
                    ModuleElement element = nameToModule.get(":" + line);
                    if (element != null) {
                        element.setCompile(true);
                    }
                    if (line.equals("All")) {
                        for (ModuleElement e : settingsModules) {
                            e.setCompile(true);
                        }
                        break;
                    }
                } else if (!Util.canIgnore(line)) {
                    // We enter other territory, be careful
                    break;
                }
            } else if (line.contains("-app-deps")) {
                beginParse = true;
            }
        }
    }


    private String getModuleName(String line) {
        Matcher m = moduleNamePattern.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private void updateSettingsLines(List<String> lineList, List<ModuleElement> elements) {
        Map<String, ModuleElement> nameToEle = new HashMap<>();
        for (ModuleElement ele : elements) {
            nameToEle.put(ele.getName(), ele);
        }

        for (int i = 0; i < lineList.size(); i++) {
            String line = lineList.get(i);
            String name = getModuleName(line);
            if (name == null) {
                continue;
            }

            ModuleElement element = nameToEle.get(name);
            if (element == null) {
                continue;
            }

            if (element.isEnable()) {
                lineList.set(i, Util.unComment(line));
            } else {
                lineList.set(i, Util.comment(line));
            }
        }
    }

    private void updateConfigLines(List<String> lineList, List<ModuleElement> elements) {
        int appDepsIndex = -1;
        int index = -1;
        for (Iterator<String> iterator = lineList.iterator(); iterator.hasNext();) {
            index++;
            String line = iterator.next();
            if (appDepsIndex >= 0) {
                if (Util.isIdentifier(line)) {
                    iterator.remove();
                } else if (!Util.canIgnore(line)) {
                    // We enter other territory, be careful
                    break;
                }
            } else if (line.contains("-app-deps")) {
                appDepsIndex = index;
            }
        }

        if (appDepsIndex < 0) {
            return;
        }

        for (ModuleElement element : elements) {
            if (element.isCompile()) {
                lineList.add(appDepsIndex + 1, element.toConfigModuleName());
            }
        }
    }

    private void showDialog() {
        SettingPanel panel = new SettingPanel(settingsModules, this, this);

        mDialog = new JFrame();
        mDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mDialog.getRootPane().setDefaultButton(panel.getConfirmButton());
        mDialog.getContentPane().add(panel);
        mDialog.pack();
        mDialog.setLocationRelativeTo(null);
        mDialog.setVisible(true);
    }

    @Override
    public void onCancel() {
        closeDialog();
    }

    @Override
    public void confirm() {
        List<ModuleElement> errList = new ArrayList<>();
        for (ModuleElement element : settingsModules) {
            if (element.isCompile() && !element.isEnable()) {
                errList.add(element);
            }
        }
        if (errList.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (ModuleElement element : errList) {
                if (sb.length() == 0) {
                    sb.append("以下模块加入了编译但没有加入settings.gradle：\n");
                } else {
                    sb.append("\n");
                }
                sb.append(element.getName());
            }
            Util.alert(mProject, sb.toString());
            return;
        }

        closeDialog();

        updateSettingsLines(settingsLines, settingsModules);
        updateConfigLines(configLines, settingsModules);

        Util.writeLinesToFile(settingsLines, settingsFile);
        Util.writeLinesToFile(configLines, configFile);
    }

    protected void closeDialog() {
        if (mDialog == null) {
            return;
        }

        mDialog.setVisible(false);
        mDialog.dispose();
    }
}
