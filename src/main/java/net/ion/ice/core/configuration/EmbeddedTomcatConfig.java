package net.ion.ice.core.configuration;

import org.apache.catalina.valves.AccessLogValve;
import org.apache.log4j.Logger;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Configuration;


/**
 * Created by juneyoungoh on 2017. 6. 19..
 */
@Configuration
public class EmbeddedTomcatConfig implements EmbeddedServletContainerCustomizer {
    private Logger logger = Logger.getLogger(EmbeddedTomcatConfig.class);

    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        try{
            if (container instanceof TomcatEmbeddedServletContainerFactory) {
                TomcatEmbeddedServletContainerFactory factory = (TomcatEmbeddedServletContainerFactory) container;
                AccessLogValve accessLogValve = new AccessLogValve();
                accessLogValve.setDirectory("/resource/ice2/tomcat/access-logs");
                accessLogValve.setPattern("common");
                accessLogValve.setSuffix(".log");
                factory.addContextValves(accessLogValve);
            } else {
                logger.error("WARNING! this customizer does not support your configured container");
            }

        } catch (Exception e) {
            logger.error("Access Log Activation failed :: ", e);
        }
    }
}
