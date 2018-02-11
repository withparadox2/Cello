package cn.com.egova.mobile.tools.cello;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.containers.HashSet;
import com.intellij.util.containers.OrderedSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by withparadox2 on 2018/2/11.
 */
class Snapshot {
    private static final String KEY_SNAPSHOT_NAMES = "cello-key-snapshot-names";
    private static final String KEY_SNAPSHOT_PREFIX = "cello-snapshot-";

    private static final String SPLIT_GROUP = "##";
    private static final String SPLIT_PART = "@@";

    private List<ModuleElement> mSettingModules;
    private JFrame mDialog;
    private List<SnapshotItem> mSSList = new ArrayList<>();
    private JPanel mListPanel;

    private IUpdateListListener mUpdateListListener;

    void showDialog(List<ModuleElement> modules, IUpdateListListener updateListListener) {
        this.mSettingModules = modules;
        this.mUpdateListListener = updateListListener;

        closeDialog();
        refreshList();
        JPanel panel = createMainPanel();
        mDialog = new JFrame();
        mDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mDialog.getContentPane().add(panel);
        mDialog.pack();
        mDialog.setLocationRelativeTo(null);
        mDialog.setVisible(true);
    }

    private void closeDialog() {
        if (mDialog == null) {
            return;
        }

        mDialog.setVisible(false);
        mDialog.dispose();
        mDialog = null;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(450, 500));
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JBScrollPane scrollPane = new JBScrollPane(createContentListPanel());
        scrollPane.setPreferredSize(new Dimension(430, 500));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        contentPanel.add(createHeaderPanel());
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPanel.add(scrollPane);

        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        JTextField textField = new JTextField();
        textField.setPreferredSize(new Dimension(300, 26));

        JButton btnAdd = new JButton();
        btnAdd.setPreferredSize(new Dimension(100, 26));
        btnAdd.setText("保存");
        btnAdd.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSnapShot(textField.getText());
            }
        });

        panel.add(textField);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(btnAdd);
        panel.add(Box.createHorizontalGlue());
        return panel;
    }

    private JPanel createContentListPanel() {
        if (mListPanel == null) {
            mListPanel = new JPanel();
        }
        mListPanel.removeAll();
        mListPanel.setLayout(new BoxLayout(mListPanel, BoxLayout.PAGE_AXIS));
        mListPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        int cnt = 0;
        for (SnapshotItem element : mSSList) {
            EntryPanel entry = new EntryPanel(element);
            if (cnt > 0) {
                mListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            mListPanel.add(entry);
            if (cnt == mSSList.size() - 1) {
                mListPanel.add(Box.createRigidArea(new Dimension(0, calContentPadding())));
            }
            cnt++;
        }

        mListPanel.add(Box.createVerticalGlue());
        mListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        return mListPanel;
    }

    private int calContentPadding() {
        int size = 500 - 31 - mSSList.size() * 35;
        return size < 0 ? 0 : size;
    }

    private void saveSnapShot(String snapshotName) {
        Set<String> nameSet = getNameSet();

        if (nameSet.contains(snapshotName)) {
            Util.alert(null, "已经存在" + snapshotName + "，请先删除");
            return;
        }

        nameSet.add(snapshotName);
        saveNameSet(nameSet);

        String content = toText(mSettingModules);
        String key = snapshotItemKey(snapshotName);
        PropertiesComponent.getInstance().setValue(key, content, "");
        refreshList();
        updateListLayout();
    }

    private Set<String> getNameSet() {
        String namesStr = PropertiesComponent.getInstance().getValue(KEY_SNAPSHOT_NAMES, "");
        String[] names = namesStr.split(SPLIT_GROUP);
        return new OrderedSet<>(Arrays.asList(names));
    }

    private String nameSetToStr(Set<String> nameSet) {
        StringBuilder sb = new StringBuilder();
        for (String name : nameSet) {
            if (sb.length() != 0) {
                sb.append(SPLIT_GROUP);
            }
            sb.append(name);
        }
        return sb.toString();
    }

    private void saveNameSet(Set<String> nameSet) {
        PropertiesComponent.getInstance().setValue(KEY_SNAPSHOT_NAMES, nameSetToStr(nameSet), "");
    }

    private void deleteItem(SnapshotItem item) {
        Set<String> nameSet = getNameSet();
        nameSet.remove(item.name);
        saveNameSet(nameSet);
        PropertiesComponent.getInstance().unsetValue(snapshotItemKey(item.name));
    }

    private String snapshotItemKey(String name) {
        return KEY_SNAPSHOT_PREFIX + name;
    }

    private void refreshList() {
        Set<String> nameSet = getNameSet();
        mSSList.clear();

        for (String name : nameSet) {
            String key = KEY_SNAPSHOT_PREFIX + name;
            String content = PropertiesComponent.getInstance().getValue(key, "");
            if (!Util.isEmpty(content)) {
                mSSList.add(new SnapshotItem(name, content));
            }
        }
    }

    private void updateListLayout() {
        createContentListPanel();
        mListPanel.revalidate();
        mListPanel.repaint();
    }

    private static class SnapshotItem {
        String name;
        String text;

        SnapshotItem(String name, String text) {
            this.name = name;
            this.text = text;
        }

        void restore(List<ModuleElement> elements) {
            if (!Util.isEmpty(text)) {
                String[] groups = text.split(SPLIT_GROUP);
                if (groups.length != 2) {
                    Util.alert(null, "格式不正确：" + text);
                    return;
                }

                String[] settingParts = groups[0].split(SPLIT_PART);
                String[] compileParts = groups[1].split(SPLIT_PART);
                Set<String> settingSet = new HashSet<>(Arrays.asList(settingParts));
                Set<String> compileSet = new HashSet<>(Arrays.asList(compileParts));

                for (ModuleElement element : elements) {
                    element.setEnable(settingSet.contains(element.getName()));
                    element.setCompile(compileSet.contains(element.getName()));
                }
            }
        }
    }

    private String toText(List<ModuleElement> elements) {
        StringBuilder sbSetting = new StringBuilder();
        StringBuilder sbCompile = new StringBuilder();
        for (ModuleElement element : elements) {
            if (element.isEnable()) {
                if (sbSetting.length() != 0) {
                    sbSetting.append(SPLIT_PART);
                }
                sbSetting.append(element.getName());
            }
            if (element.isCompile()) {
                if (sbCompile.length() != 0) {
                    sbCompile.append(SPLIT_PART);
                }
                sbCompile.append(element.getName());
            }
        }
        return sbSetting.toString() + SPLIT_GROUP + sbCompile.toString();
    }

    private class EntryPanel extends JPanel {

        EntryPanel(SnapshotItem item) {
            setPreferredSize(new Dimension(400, 30));

            JLabel nameLabel = new JLabel(item.name);
            nameLabel.setPreferredSize(new Dimension(150, 26));

            JButton btnRestore = new JButton();
            btnRestore.setPreferredSize(new Dimension(100, 26));
            btnRestore.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    item.restore(mSettingModules);
                    if (mUpdateListListener != null) {
                        mUpdateListListener.onListUpdate();
                    }
                    closeDialog();
                }
            });
            //setText must be called after setAction
            btnRestore.setText("恢复");
            btnRestore.setVisible(true);

            JButton btnDelete = new JButton();
            btnDelete.setPreferredSize(new Dimension(100, 26));
            btnDelete.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteItem(item);
                    refreshList();
                    updateListLayout();
                }
            });
            btnDelete.setText("删除");
            btnDelete.setVisible(true);


            add(nameLabel);
            add(Box.createRigidArea(new Dimension(10, 0)));
            add(btnRestore);
            add(Box.createRigidArea(new Dimension(10, 0)));
            add(btnDelete);
            add(Box.createHorizontalGlue());
        }
    }

    interface IUpdateListListener {
        void onListUpdate();
    }
}
