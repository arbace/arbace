package demo.outside;

import java.awt.Desktop;
import javax.sql.DataSource;

/** A field of java.sql's DataSource and a method of java.awt.Desktop (as Ghidra's FileUtilities), neither run. */
public class Store {
    private DataSource source;

    public static String kind() {
        return "store";
    }

    public Object connect() throws java.sql.SQLException {
        return source.getConnection();
    }

    public static boolean browse() {
        return Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }
}
