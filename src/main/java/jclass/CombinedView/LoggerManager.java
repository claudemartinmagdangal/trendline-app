/**
 *
 */
package jclass.CombinedView;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import java.net.URI;
import java.nio.file.Paths;

/**
 * @author Ghanshyam Vaghasiya
 *
 */
public class LoggerManager {
    public static void initializeLoggingContext() {
        initializeLoggingContext(Paths.get("src", "main", "resources", "log4j2.xml").toUri());
    }

    public static void initializeLoggingContext(URI configUri) {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        loggerContext.setConfigLocation(configUri);
    }
}
