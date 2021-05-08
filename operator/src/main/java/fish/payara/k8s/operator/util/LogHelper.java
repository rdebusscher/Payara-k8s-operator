package fish.payara.k8s.operator.util;

import fish.payara.k8s.operator.PayaraOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogHelper {

    private static Logger logger = LoggerFactory.getLogger(PayaraOperator.class);

    public static void exception(Throwable error) {
        logger.error("{}: {}", error.getMessage(), error);
    }

    public static void log(String action) {
        logger.info(action);
    }
}
