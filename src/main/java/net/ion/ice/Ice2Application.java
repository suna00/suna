package net.ion.ice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.web.servlet.spring.SpringBootWebPluginInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

@SpringBootApplication
@ComponentScan({ "net.ion.ice" })
@EnableAsync
public class Ice2Application {

	public static final String USE_HAZELCAST = "true";

	public static void main(String[] args) {
        Stagemonitor.init();
		ApplicationContext ctx =  SpringApplication.run(Ice2Application.class, args);
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(5);
		taskExecutor.setMaxPoolSize(10);
		taskExecutor.setQueueCapacity(100);
		return taskExecutor;
	}

	@Component
    public static class StagemonitorEnabler implements EmbeddedServletContainerCustomizer {
        @Override
        public void customize(ConfigurableEmbeddedServletContainer container) {
            container.addInitializers(new ServletContextInitializer() {
                @Override
                public void onStartup(ServletContext servletContext) throws ServletException {
//                    이건 어디서 난거
//                    new WebPlugin().onStartup(null, servletContext);

                }
            });
        }
    }

}
