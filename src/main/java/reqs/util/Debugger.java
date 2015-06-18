package reqs.util;

/**
 * Created by maksing on 14/6/15.
 **/
public class Debugger {
    public static final boolean ENABLED = true;

    public static void log(String message) {
        if (ENABLED) {
            System.out.printf(message + "\n");
        }
    }
}