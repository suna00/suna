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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.stagemonitor.core.Stagemonitor;

@SpringBootApplication(scanBasePackages = {"net.ion.ice"},exclude = {DataSourceAutoConfiguration.class})
@EnableAsync
public class Ice2Application {
	private static Logger logger = LoggerFactory.getLogger(Ice2Application.class);

	public static void main(String[] args) {
//	    System.setProperty("spring.devtools.restart.enabled","false");
//        System.setProperty("spring.devtools.livereload.enabled","true");
//        Stagemonitor.init();
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


	@Bean
	public ThreadPoolTaskScheduler threadPoolTaskScheduler(){
		ThreadPoolTaskScheduler threadPoolTaskScheduler	= new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(5);
		threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
		return threadPoolTaskScheduler;
	}

}
