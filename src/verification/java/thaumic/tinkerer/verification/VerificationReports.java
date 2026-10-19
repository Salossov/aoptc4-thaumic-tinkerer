package thaumic.tinkerer.verification;

/** One shared XML reporter for the whole server test run, including failures. */
public final class VerificationReports {
    private static boolean installed;
    public static synchronized void install() {
        if (installed) return;
        try {
            var file = new java.io.File(System.getProperty("thaumictinkerer.testReport", "gametest.xml"));
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            net.minecraft.gametest.framework.GlobalTestReporter.replaceWith(
                    new net.minecraft.gametest.framework.JUnitLikeTestReporter(file));
            installed = true;
        } catch (javax.xml.parsers.ParserConfigurationException error) {
            throw new IllegalStateException("Cannot configure GameTest XML output", error);
        }
    }
    private VerificationReports() {}
}
