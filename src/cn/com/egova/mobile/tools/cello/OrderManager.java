package cn.com.egova.mobile.tools.cello;

import com.intellij.ide.util.PropertiesComponent;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by withparadox2 on 2018/2/8.
 */
public class OrderManager {
    private static final String KEY_ORDER = "cello-key-display-order";
    /**Alphabet order of name*/
    public static final int ORDER_NAME = 0;
    /**Checked item order first*/
    public static final int ORDER_CHECK_STATE = 1;
    /**Default order defined in file*/
    public static final int ORDER_FILE = 2;

    private static final int DEFAULT_ORDER = ORDER_NAME;

    private int mOrderType = DEFAULT_ORDER;

    private List<ModuleElement> mElements;

    public OrderManager(List<ModuleElement> elements) {
        this.mElements = elements;
        setOrderType(PropertiesComponent.getInstance().getInt(KEY_ORDER, DEFAULT_ORDER));
    }

    public OrderManager(List<ModuleElement> elements, int type) {
        this.mElements = elements;
        setOrderType(type);
    }

    public void updateOrder(int orderType) {
        setOrderType(orderType);
        sort();
    }

    public void setOrderType(int type) {
        this.mOrderType = type;
        PropertiesComponent.getInstance().setValue(KEY_ORDER, type, DEFAULT_ORDER);
    }

    public int getOrderType() {
        return this.mOrderType;
    }

    public void sort() {
        if (mOrderType == ORDER_NAME) {
            Collections.sort(mElements, new Comparator<ModuleElement>() {
                @Override
                public int compare(ModuleElement o1, ModuleElement o2) {
                    return o1.lowerCaseName().compareTo(o2.lowerCaseName());
                }
            });
        } else if (mOrderType == ORDER_FILE) {
            Collections.sort(mElements, new Comparator<ModuleElement>() {
                @Override
                public int compare(ModuleElement o1, ModuleElement o2) {
                    return o1.getDefinedIndex() - o2.getDefinedIndex();
                }
            });
        } else if (mOrderType == ORDER_CHECK_STATE) {
            Collections.sort(mElements, new Comparator<ModuleElement>() {
                @Override
                public int compare(ModuleElement o1, ModuleElement o2) {
                    if (o1.isEnable() && o2.isEnable()) {
                        if (o1.isCompile()) {
                            return -1;
                        } else if (o2.isCompile()) {
                            return 1;
                        } else {
                            return o1.lowerCaseName().compareTo(o2.lowerCaseName());
                        }
                    } else if (o1.isEnable()) {
                        return -1;
                    } else if (o2.isEnable()) {
                        return 1;
                    } else {
                        return o1.lowerCaseName().compareTo(o2.lowerCaseName());
                    }
                }
            });
        }
    }

}
