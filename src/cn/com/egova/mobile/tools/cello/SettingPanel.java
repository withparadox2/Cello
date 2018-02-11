package cn.com.egova.mobile.tools.cello;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by withparadox2 on 2018/2/7.
 */
public class SettingPanel extends JPanel implements Snapshot.IUpdateListListener {
    protected JButton mConfirm;
    protected JButton mCancel;
    private IConfirmListener mConfirmListener;
    private ICancelListener mCancelListener;

    private List<ModuleElement> mSettingModules;

    private CheckBoxWrapper mCheckAllSettingWrapper;
    private CheckBoxWrapper mCheckAllCompileWrapper;

    private List<EntryPanel> mEntries = new ArrayList<>();

    private OrderManager mOrderManager;

    private JPanel mListPanel;

    private List<ModuleElement> mShowModules;

    private boolean mIsLargeScreen;
    private Snapshot mSnapshot;

    public SettingPanel(List<ModuleElement> settingModules,
                        IConfirmListener confirmListener, ICancelListener cancelListener) {
        this.mSettingModules = settingModules;
        this.mShowModules = new ArrayList<>(settingModules);
        this.mConfirmListener = confirmListener;
        this.mCancelListener = cancelListener;
        this.mOrderManager = new OrderManager(mShowModules);
        this.mOrderManager.sort();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (screenSize.width > 1400 && screenSize.height > 900) {
            mIsLargeScreen = true;
        }

        if (mIsLargeScreen) {
            setPreferredSize(new Dimension(580, 830));
        } else {
            setPreferredSize(new Dimension(450, 600));
        }
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        addModules();
        addButtons();
    }

    private void addModules() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        contentPanel.add(checkAllPanel());

        contentPanel.add(new FilterPanel());

        JPanel header = new HeaderPanel();
        contentPanel.add(header);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JBScrollPane scrollPane = new JBScrollPane(buildListLayout());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        contentPanel.add(scrollPane);

        add(contentPanel, BorderLayout.CENTER);
        refresh();
    }

    private Component buildListLayout() {
        if (mListPanel == null) {
            mListPanel = new JPanel();
        }
        mListPanel.removeAll();
        mEntries.clear();
        mListPanel.setLayout(new BoxLayout(mListPanel, BoxLayout.PAGE_AXIS));
        mListPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        int cnt = 0;
        for (ModuleElement element : mShowModules) {
            EntryPanel entry = new EntryPanel(element);
            if (cnt > 0) {
                mListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            mListPanel.add(entry);
            cnt++;

            mEntries.add(entry);
        }

        mListPanel.add(Box.createVerticalGlue());
        mListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        return mListPanel;
    }

    private CheckAllResult getCheckAllResult() {
        boolean setting = true;
        boolean compile = true;
        for (ModuleElement element : mSettingModules) {
            if (!element.isEnable()) {
                setting = false;
            }
            if (!element.isDisableCompile() && !element.isRawCompile()) {
                compile = false;
                if (!setting) {
                    break;
                }
            }
        }
        return new CheckAllResult(setting, compile);
    }

    private JPanel checkAllPanel() {
        JCheckBox cbSetting = new JCheckBox();
        cbSetting.setPreferredSize(new Dimension(40, 26));

        JLabel settingDesc = configLabel(new JLabel("全选/反选"), 90);

        JCheckBox cbCompile = new JCheckBox();
        cbCompile.setPreferredSize(new Dimension(40, 26));

        JLabel compileDesc = configLabel(new JLabel("全选/反选"), 90);

        JButton btnSnapShot = new JButton();
        btnSnapShot.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mSnapshot == null) {
                    mSnapshot = new Snapshot();
                }
                mSnapshot.showDialog(mSettingModules, SettingPanel.this);
            }
        });
        btnSnapShot.setPreferredSize(new Dimension(90, 26));
        btnSnapShot.setText("Snapshot");
        btnSnapShot.setVisible(true);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(Box.createRigidArea(new Dimension(1, 0)));
        panel.add(cbSetting);
        panel.add(settingDesc);
        panel.add(Box.createRigidArea(new Dimension(127, 0)));
        panel.add(cbCompile);
        panel.add(compileDesc);
        panel.add(btnSnapShot);
        panel.add(Box.createHorizontalGlue());

        mCheckAllSettingWrapper = new CheckBoxWrapper(cbSetting, new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean isChecked = mCheckAllSettingWrapper.isSelected();
                for (EntryPanel entry : mEntries) {
                    entry.element.setEnable(isChecked);
                    entry.settingWrapper.setSelectedWrapper(isChecked);
                    if (!isChecked) {
                        entry.unCheckCompileAsNeed(true);
                    }
                }
                updateIsCheckAll(true);
            }
        }).attach();
        mCheckAllCompileWrapper = new CheckBoxWrapper(cbCompile, new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean isChecked = mCheckAllCompileWrapper.checkBox.isSelected();
                for (EntryPanel entry : mEntries) {
                    if (entry.cbCompile.isEnabled()) {
                        entry.element.setCompile(isChecked);
                        entry.compileWrapper.setSelectedWrapper(isChecked);
                    }
                    if (isChecked) {
                        entry.checkSettingAsNeed(true);
                    }
                }
                updateIsCheckAll(true);
            }
        }).attach();

        updateIsCheckAll(true);

        return panel;
    }

    private void addButtons() {
        mCancel = new JButton();
        mCancel.setAction(new CancelAction());
        mCancel.setPreferredSize(new Dimension(120, 26));
        mCancel.setText("Cancel");
        mCancel.setVisible(true);

        mConfirm = new JButton();
        mConfirm.setAction(new ConfirmAction());
        mConfirm.setPreferredSize(new Dimension(120, 26));
        mConfirm.setText("Confirm");
        mConfirm.setVisible(true);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(mCancel);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(mConfirm);

        add(buttonPanel, BorderLayout.PAGE_END);
        refresh();
    }

    public JButton getConfirmButton() {
        return mConfirm;
    }

    protected void refresh() {
        revalidate();

        if (mConfirm != null) {
            mConfirm.setVisible(mSettingModules.size() > 0);
        }
    }

    @Override
    public void onListUpdate() {
        sortIfCheckState();
        updateListLayout();
    }

    private class HeaderPanel extends JPanel {
        public HeaderPanel() {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            add(Box.createRigidArea(new Dimension(1, 0)));
            add(configLabel(new JLabel("Settings"), 90));
            add(Box.createRigidArea(new Dimension(11, 0)));
            add(configLabel(new JLabel("Module"), 170));
            add(Box.createRigidArea(new Dimension(12, 0)));
            add(configLabel(new JLabel("Compile"), mIsLargeScreen ? 90 : 60));

            JButton btnClear = new JButton();
            btnClear.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (EntryPanel entry : mEntries) {
                        entry.compileWrapper.setSelectedWrapper(false);
                        entry.element.setCompile(false);
                    }
                    updateIsCheckAll(true);
                }
            });
            btnClear.setPreferredSize(new Dimension(mIsLargeScreen ? 90 : 60, 26));
            btnClear.setText("clear");
            btnClear.setVisible(true);
            add(btnClear);

            add(Box.createHorizontalGlue());
        }
    }

    private class EntryPanel extends JPanel {
        public ModuleElement element;
        public JCheckBox cbSetting;
        public JCheckBox cbCompile;

        public JLabel labelModuleName;
        public CheckBoxWrapper settingWrapper;
        public CheckBoxWrapper compileWrapper;

        public EntryPanel(ModuleElement element) {
            this.element = element;
            cbSetting = new JCheckBox();
            cbSetting.setPreferredSize(new Dimension(90, 26));
            cbSetting.setSelected(element.isEnable());

            labelModuleName = new JLabel(element.getDisplay());
            labelModuleName.setPreferredSize(new Dimension(170, 26));

            cbCompile = new JCheckBox();
            cbCompile.setPreferredSize(new Dimension(40, 26));
            cbCompile.setSelected(element.isCompile());

            if (element.isDisableCompile()) {
                cbCompile.setEnabled(false);
            }

            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setMaximumSize(new Dimension(Short.MAX_VALUE, 54));
            add(cbSetting);
            add(Box.createRigidArea(new Dimension(10, 0)));
            add(labelModuleName);
            add(Box.createRigidArea(new Dimension(10, 0)));
            add(cbCompile);
            add(Box.createHorizontalGlue());

            settingWrapper = new CheckBoxWrapper(cbSetting, new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    element.setEnable(cbSetting.isSelected());

                    if (isInconsistent()) {
                        compileWrapper.setSelected(false);
                    } else {
                        updateIsCheckAll(true);
                    }
                }
            }).attach();
            compileWrapper = new CheckBoxWrapper(cbCompile, new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    element.setCompile(cbCompile.isSelected());

                    if (isInconsistent()) {
                        settingWrapper.setSelected(true);
                    } else {
                        updateIsCheckAll(true);
                    }
                }
            }).attach();
        }

        public boolean isInconsistent() {
            return element.isCompile() && !element.isEnable();
        }

        public void checkSettingAsNeed(boolean wrap) {
            if (isInconsistent()) {
                if (wrap) {
                    settingWrapper.setSelectedWrapper(true);
                } else {
                    settingWrapper.setSelected(true);
                }
                element.setEnable(true);
            }
        }

        public void unCheckCompileAsNeed(boolean wrap) {
            if (isInconsistent()) {
                if (wrap) {
                    compileWrapper.setSelectedWrapper(false);
                } else {
                    compileWrapper.setSelected(false);
                }
                element.setCompile(false);
            }
        }
    }

    private void updateIsCheckAll(boolean wrap) {
        CheckAllResult result = getCheckAllResult();
        if (wrap) {
            mCheckAllCompileWrapper.setSelectedWrapper(result.compile);
            mCheckAllSettingWrapper.setSelectedWrapper(result.setting);
        } else {
            mCheckAllCompileWrapper.setSelected(result.compile);
            mCheckAllSettingWrapper.setSelected(result.setting);
        }
        sortIfCheckState();
    }

    private class ConfirmAction extends AbstractAction {
        public void actionPerformed(ActionEvent event) {
            if (mConfirmListener != null) {
                mConfirmListener.confirm();
            }
        }
    }

    private class CancelAction extends AbstractAction {
        public void actionPerformed(ActionEvent event) {
            if (mCancelListener != null) {
                mCancelListener.onCancel();
            }
        }
    }

    private JLabel configLabel(JLabel label, int width) {
        label.setPreferredSize(new Dimension(width, 26));
        label.setFont(new Font(label.getFont().getFontName(), Font.BOLD, label.getFont().getSize()));
        return label;
    }

    private static class CheckBoxWrapper {
        public JCheckBox checkBox;
        public ItemListener listener;

        public CheckBoxWrapper(JCheckBox checkBox, ItemListener listener) {
            this.checkBox = checkBox;
            this.listener = listener;
        }

        public CheckBoxWrapper attach() {
            if (listener != null) {
                detach();
                checkBox.addItemListener(listener);
            }
            return this;
        }

        public CheckBoxWrapper detach() {
            checkBox.removeItemListener(listener);
            return this;
        }

        public void setSelectedWrapper(boolean isSelected) {
            detach();
            checkBox.setSelected(isSelected);
            attach();
        }

        public void setSelected(boolean isSelected) {
            checkBox.setSelected(isSelected);
        }

        public boolean isSelected() {
            return checkBox.isSelected();
        }
    }

    private static class CheckAllResult {
        public boolean setting = false;
        public boolean compile = false;
        public CheckAllResult(boolean setting, boolean compile) {
            this.compile = compile;
            this.setting = setting;
        }
    }

    private class FilterPanel extends JPanel {
        private String lastSearchText;
        private List<MenuElement> backupList;
        public FilterPanel() {
            final JRadioButton rbName = new JRadioButton("名称");
            final JRadioButton rbCheck = new JRadioButton("选中");
            final JRadioButton rbFile = new JRadioButton("文件");

            ButtonGroup group = new ButtonGroup();
            group.add(rbName);
            group.add(rbFile);
            group.add(rbCheck);

            int type = mOrderManager.getOrderType();
            if (type == OrderManager.ORDER_NAME) {
                rbName.setSelected(true);
            } else if (type == OrderManager.ORDER_CHECK_STATE) {
                rbCheck.setSelected(true);
            } else if (type == OrderManager.ORDER_FILE) {
                rbFile.setSelected(true);
            }

            ItemListener listener = new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (rbName.isSelected()) {
                        mOrderManager.updateOrder(OrderManager.ORDER_NAME);
                        updateListLayout();
                    } else if (rbFile.isSelected()) {
                        mOrderManager.updateOrder(OrderManager.ORDER_FILE);
                        updateListLayout();
                    } else if (rbCheck.isSelected()) {
                        mOrderManager.updateOrder(OrderManager.ORDER_CHECK_STATE);
                        updateListLayout();
                    }
                }
            };

            rbName.addItemListener(listener);
            rbFile.addItemListener(listener);
            rbCheck.addItemListener(listener);

            JTextField searchBox = new JTextField();
            searchBox.setPreferredSize(new Dimension(50, 26));
            searchBox.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(DocumentEvent documentEvent) {
                    String text = searchBox.getText();

                    if (!Util.equals(text, lastSearchText)) {
                        mShowModules.clear();
                        if (Util.isEmpty(text)) {
                            mShowModules.addAll(mSettingModules);
                        } else {
                            for (ModuleElement element : mSettingModules) {
                                if (element.lowerCaseName().contains(text.toLowerCase())) {
                                    mShowModules.add(element);
                                }
                            }
                        }
                        sortIfCheckState();
                        updateListLayout();
                    }

                    lastSearchText = text;
                }
            });

            JButton btnClearSearch = new JButton();
            btnClearSearch.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    searchBox.setText("");
                }
            });
            btnClearSearch.setPreferredSize(new Dimension(60, 26));
            btnClearSearch.setText("x");
            btnClearSearch.setVisible(true);

            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            add(Box.createRigidArea(new Dimension(1, 0)));
            add(configLabel(new JLabel("排序："), mIsLargeScreen ? 60 : 40));
            add(Box.createRigidArea(new Dimension(5, 0)));
            add(rbName);
            add(rbCheck);
            add(rbFile);
            add(Box.createRigidArea(new Dimension(10, 0)));
            add(configLabel(new JLabel("搜索："), mIsLargeScreen ? 60 : 40));
            add(searchBox);
            add(btnClearSearch);
            add(Box.createHorizontalGlue());
        }
    }

    private void sortIfCheckState() {
        if (mOrderManager.getOrderType() == OrderManager.ORDER_CHECK_STATE) {
            mOrderManager.sort();
            updateListLayout();
        }
    }

    private void updateListLayout() {
        buildListLayout();
        mListPanel.revalidate();
        mListPanel.repaint();
    }
}
