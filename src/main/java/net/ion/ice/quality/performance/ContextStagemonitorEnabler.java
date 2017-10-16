package net.ion.ice.quality.performance;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.stereotype.Component;
import org.stagemonitor.web.servlet.ServletPlugin;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Created by juneyoungoh on 2017. 6. 13..
 */
@Component
public class ContextStagemonitorEnabler implements EmbeddedServletContainerCustomizer {
    @Override
    public void customize(ConfigurableEmbeddedServletContainer container){
        container.addInitializers(new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                    new ServletPlugin().onStartup(null, servletContext);
            }
        });
    }
}
