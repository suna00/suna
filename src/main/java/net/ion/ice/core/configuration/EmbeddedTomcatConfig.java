package net.ion.ice.core.configuration;

import ch.qos.logback.access.tomcat.LogbackValve;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import net.logstash.logback.appender.LogstashAccessTcpSocketAppender;
import net.logstash.logback.encoder.LogstashAccessEncoder;
import org.apache.catalina.valves.AccessLogValve;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;


/**
 * Created by juneyoungoh on 2017. 6. 19..
 * 해당 클래스는 Spring Boot Embedded Tomcat 설정을 Java 로 수정하는 Class
 * @Confifuration 주석 해제시 access logs 를 파일로 출력함
 * LogbackValve 의 경우는 logback-dev#.xml 설정과 중복되므로 해제시에는 프로파일에서 xml 제거할 것
 */
//@Configuration
public class EmbeddedTomcatConfig implements EmbeddedServletContainerCustomizer {
    private org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmbeddedTomcatConfig.class);
    private boolean useLogbackValve = true;

    public void ableLogbackValve(boolean use){
        this.useLogbackValve = use;
    }

    @Override
    public void customize(ConfigurableEmbeddedServletContainer container) {
        try{
            if (container instanceof TomcatEmbeddedServletContainerFactory) {
                TomcatEmbeddedServletContainerFactory factory = (TomcatEmbeddedServletContainerFactory) container;

                //logstash appender
                if(useLogbackValve) {
                    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                    LoggerContext loggerContext = rootLogger.getLoggerContext();

                    LogbackValve logbackValve = new LogbackValve();
                    LogstashAccessTcpSocketAppender logstashAccessAppender = new LogstashAccessTcpSocketAppender();

                    logstashAccessAppender.setName("access-logstash");
//                    logstashAccessAppender.setContext(loggerContext);
                    logstashAccessAppender.addDestination("125.131.88.156:5001");

                    LogstashAccessEncoder lae = new LogstashAccessEncoder();
//                    lae.setWriteVersionAsString(true);
                    lae.setContext(loggerContext);
                    logstashAccessAppender.setEncoder(lae);
                    logbackValve.addAppender(logstashAccessAppender);
                    logbackValve.setAsyncSupported(true);

                    factory.addContextValves(logbackValve);
                }


                // file appender
                AccessLogValve accessLogValve = new AccessLogValve();
                accessLogValve.setDirectory("/resource/ice2/tomcat/access-logs");
                accessLogValve.setPattern("common");
                accessLogValve.setSuffix(".log");
                factory.addContextValves(accessLogValve);


                // debugging

                logger.info("============================================================");
                logger.info("==================== Context Valves ========================");
                logger.info("============================================================");
                factory.getContextValves().stream().forEach(v -> {
                    logger.info("context valve :: " + v.getClass().getName());
                });
                logger.info("============================================================");
                logger.info("============================================================");
                logger.info("============================================================");



            } else {
                logger.error("WARNING! this customizer does not support your configured container");
            }

        } catch (Exception e) {
            logger.error("Access Log Activation failed :: ", e);
        }
    }
}
