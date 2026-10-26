/**
 * PTMS launcher - one entry point for the whole software.
 *   java -jar PTMS.jar            -> GUI (window) version
 *   java -jar PTMS.jar --console  -> console version
 * If no display is available (headless server / SSH), it falls back to console automatically.
 */
public class PTMS {
    public static void main(String[] args) {
        boolean console = java.awt.GraphicsEnvironment.isHeadless();
        for (String a : args) if (a.equalsIgnoreCase("--console") || a.equalsIgnoreCase("-c")) console = true;
        if (console) PublicTransportConsole.main(args);
        else PublicTransportApp.main(args);
    }
}
