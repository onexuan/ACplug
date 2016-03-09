package digimagus.csrmesh.acplug.util;

/**
 * 工具类，验证输入文字是否合法
 */
public class Utils {
    public static boolean sqlInjection(String msg) {
        boolean vrification = false;
        msg = msg.toLowerCase();
        for (String str : sqlCategories) {
            if (msg.indexOf(str.toLowerCase()) != -1) {
                vrification = true;
                break;
            }
        }
        return vrification;
    }
    static String[] sqlCategories = new String[]{
            "select", "from", "where", "table", "temp", "master", "database", "like", "exists",
            "schema", "and", "insert", "char", "order","count", "update",
            "delete", "union", "user", "row", "concat", "limit",
            "drop", "truncate", "grant", "use", "column", "declare",
            "information", "or","sqlite_version"
    };
}
