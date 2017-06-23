package net.ion.ice.core.configuration;

import ch.qos.logback.access.tomcat.LogbackValve;
import net.logstash.logback.appender.LogstashAccessTcpSocketAppender;
import net.logstash.logback.encoder.LogstashAccessEncoder;
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
    private boolean useLogbackValve = true;

    public void ableLogbackValve(boolean use){
        this.useLogbackValve = use;
    }

    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        try{
            if (container instanceof TomcatEmbeddedServletContainerFactory) {
                TomcatEmbeddedServletContainerFactory factory = (TomcatEmbeddedServletContainerFactory) container;
                if(useLogbackValve) {
                    LogbackValve logbackValve = new LogbackValve();
                    LogstashAccessTcpSocketAppender logstashAccessAppender = new LogstashAccessTcpSocketAppender();
                    logstashAccessAppender.addDestination("125.131.88.156:5000");
                    logstashAccessAppender.setEncoder(new LogstashAccessEncoder());
                    logbackValve.addAppender(logstashAccessAppender);
                    factory.addContextValves(logbackValve);
                }

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
