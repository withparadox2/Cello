package cn.com.egova.mobile.tools.cello;

/**
 * Created by withparadox2 on 2018/2/7.
 */
public class ModuleElement {
    private String name;
    private boolean isEnable;
    private boolean isCompile;

    private boolean isDisableCompile;
    private int definedIndex;

    public ModuleElement(String name, boolean isEnable) {
        this.name = name;
        this.isEnable = isEnable;
    }


    public String getName() {
        return name;
    }

    public String toCompileModuleName() {
        return name.replace(":", "");
    }

    public String getDisplay() {
        return toCompileModuleName();
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

    public int getDefinedIndex() {
        return definedIndex;
    }

    public void setDefinedIndex(int definedIndex) {
        this.definedIndex = definedIndex;
    }
}
