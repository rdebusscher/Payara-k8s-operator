package fish.payara.k8s.operator.util;

import fish.payara.k8s.operator.PayaraOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogHelper {

    private static Logger logger = LoggerFactory.getLogger(PayaraOperator.class);

    public static void log(String action, Object obj) {
        logger.info("{}: {}", action, obj);
    }

    public static void log(String action) {
        logger.info(action);
    }
}
