package net.ion.ice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.stagemonitor.core.Stagemonitor;

@SpringBootApplication(scanBasePackages = {"net.ion.ice"},exclude = {DataSourceAutoConfiguration.class})
@EnableAsync
public class Ice2Application {

	public static final String USE_HAZELCAST = "true";

	public static void main(String[] args) {
//	    System.setProperty("spring.devtools.restart.enabled","false");
//        System.setProperty("spring.devtools.livereload.enabled","true");
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
}
