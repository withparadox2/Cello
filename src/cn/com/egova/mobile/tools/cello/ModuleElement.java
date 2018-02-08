package cn.com.egova.mobile.tools.cello;

/**
 * Created by withparadox2 on 2018/2/7.
 */
public class ModuleElement {
    private String name;
    private boolean isEnable;
    private boolean isCompile;

    private boolean isDisableCompile;

    public ModuleElement(String name, boolean isEnable) {
        this.name = name;
        this.isEnable = isEnable;
    }


    public String getName() {
        return name;
    }

    public String toConfigModuleName() {
        return name.replace(":", "");
    }

    public String getDisplay() {
        return toConfigModuleName();
    }

    public boolean isDisableCompile() {
        return isDisableCompile;
    }

    public boolean isCompile() {
        return !isDisableCompile && isCompile;
    }

    public boolean isRawCompile() {
        return isCompile;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setCompile(boolean compile) {
        this.isCompile = compile;
    }

    public void setEnable(boolean enable) {
        this.isEnable = enable;
    }

    public void setDisableCompile(boolean disableCompile) {
        isDisableCompile = disableCompile;
    }

    public String lowerCaseName() {
        return name.toLowerCase();
    }
}
