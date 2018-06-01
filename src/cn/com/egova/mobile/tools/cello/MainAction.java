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

    private static Pattern sModuleNamePattern = Pattern.compile("['\"](:\\S*)['\"]");

    private VirtualFile mSettingsFile;
    private VirtualFile mCompileFile;

    private List<ModuleElement> mSettingsModules;
    private Map<String, ModuleElement> mNameToModule;

    private List<String> mSettingsLines;
    private List<String> mCompileLines;

    private JFrame mDialog;

    private Project mProject;

    @Override
    public void actionPerformed(AnActionEvent event) {
        this.mProject = event.getData(PlatformDataKeys.PROJECT);

        VirtualFile file = mProject.getBaseDir();

        mSettingsFile = file.findChild("settings_developer.gradle");
        if (!Util.exists(mSettingsFile)) {
            mSettingsFile = file.findChild("settings.gradle");
        }
        if (!Util.exists(mSettingsFile)) {
            Util.toast(mProject, "settings.gradle不存在");
            return;
        }
        mCompileFile = file.findChild("setting-plugins-local.gradle");
        if (!Util.exists(mCompileFile)) {
            mCompileFile = file.findChild("setting-plugins.gradle");
        }
        if (!Util.exists(mCompileFile)) {
            Util.toast(mProject, "setting-plugins.gradle不存在");
            return;
        }

        mSettingsLines = Util.readFileToLines(mSettingsFile);
        mSettingsModules = extractSettingsModules(mSettingsLines);

        mCompileLines = Util.readFileToLines(mCompileFile);
        extractCompileModules(mCompileLines);

        showDialog();
    }

    private List<ModuleElement> extractSettingsModules(List<String> settingsLines) {
        mNameToModule = new HashMap<>();
        List<ModuleElement> moduleList = new ArrayList<>();
        int index = 0;
        for (String line : settingsLines) {
            String name = getModuleName(line);
            if (name != null && !mNameToModule.containsKey(name)) {
                ModuleElement element = new ModuleElement(name, !Util.isComment(line));
                element.setDefinedIndex(index++);
                element.setDisableCompile(ignoreForCompile(line));
                moduleList.add(element);
                mNameToModule.put(name, element);
            }
        }

        return moduleList;
    }

    private boolean ignoreForCompile(String name) {
        return name.contains("eGovaMobile");
    }

    private void extractCompileModules(List<String> compileLines) {
        boolean beginParse = false;
        for (String line : compileLines) {
            if (beginParse) {
                if (Util.isIdentifier(line)) {
                    ModuleElement element = mNameToModule.get(":" + line);
                    if (element != null) {
                        element.setCompile(true);
                    }
                    if (line.equals("All")) {
                        for (ModuleElement e : mSettingsModules) {
                            e.setCompile(true);
                        }
                        // Find what we need, break to save energy
                        break;
                    }
                } else if (!Util.canIgnore(line)) {
                    // We enter other territory, be careful
                    break;
                }
            } else if (isModulePartStart(line)) {
                beginParse = true;
            }
        }
    }


    private String getModuleName(String line) {
        Matcher m = sModuleNamePattern.matcher(line);
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

    private void updateCompileLines(List<String> lineList, List<ModuleElement> elements) {
        int appDepsIndex = -1;
        int index = -1;
        for (Iterator<String> iterator = lineList.iterator(); iterator.hasNext(); ) {
            index++;
            String line = iterator.next();
            if (appDepsIndex >= 0) {
                if (Util.isIdentifier(line)) {
                    iterator.remove();
                } else if (!Util.canIgnore(line)) {
                    // We enter other territory, be careful
                    break;
                }
            } else if (isModulePartStart(line)) {
                appDepsIndex = index;
            }
        }

        if (appDepsIndex < 0) {
            return;
        }

        for (ModuleElement element : elements) {
            if (element.isCompile()) {
                lineList.add(appDepsIndex + 1, element.toCompileModuleName());
            }
        }
    }

    private boolean isModulePartStart(String line) {
        return line.startsWith("-app-deps") || line.startsWith("-app-not-aar");
    }

    private void showDialog() {
        SettingPanel panel = new SettingPanel(mSettingsModules, this, this);

        mDialog = new JFrame();
        mDialog.setTitle(mSettingsFile.getName() + "/" + mCompileFile.getName());
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
        for (ModuleElement element : mSettingsModules) {
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

        updateSettingsLines(mSettingsLines, mSettingsModules);
        updateCompileLines(mCompileLines, mSettingsModules);

        Util.writeLinesToFile(mSettingsLines, mSettingsFile);
        Util.writeLinesToFile(mCompileLines, mCompileFile);
    }

    private void closeDialog() {
        if (mDialog == null) {
            return;
        }

        mDialog.setVisible(false);
        mDialog.dispose();
    }
}
