package net.ion.ice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;

@SpringBootApplication
@ComponentScan({ "net.ion.ice" })
public class Ice2CmApplication {

	public static AnnotationConfigEmbeddedWebApplicationContext context ;

	public static void main(String[] args) {
        context = (AnnotationConfigEmbeddedWebApplicationContext) SpringApplication.run(Ice2CmApplication.class, args);
	}
}
