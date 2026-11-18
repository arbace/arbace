package scan.plug;

import scan.api.Plugin;

/** A plugin of the module app, in the package core's Alpha is in, with a member class (listed as Beta$Helper). */
public class Beta implements Plugin {
    static final class Helper {
        static String tag() {
            return "beta";
        }
    }

    public String name() {
        return Helper.tag();
    }
}
