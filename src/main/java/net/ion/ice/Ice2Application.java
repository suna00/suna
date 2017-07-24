package net.ion.ice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.stagemonitor.core.Stagemonitor;

@SpringBootApplication(scanBasePackages = {"net.ion.ice"},exclude = {DataSourceAutoConfiguration.class})
@EnableAsync
public class Ice2Application {
	private static Logger logger = LoggerFactory.getLogger(Ice2Application.class);

	public static final String USE_HAZELCAST = "true";

	public static void main(String[] args) {
	    System.setProperty("spring.devtools.restart.enabled","false");
        System.setProperty("spring.devtools.livereload.enabled","true");
        Stagemonitor.init();
		logger.info("BEFORE START");
		ApplicationContext ctx =  SpringApplication.run(Ice2Application.class, args);
		ApplicationContextManager.context = ctx ;
		logger.info("AFTER START");

	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(5);
		taskExecutor.setMaxPoolSize(10);
		taskExecutor.setQueueCapacity(100);
		return taskExecutor;
	}
}
