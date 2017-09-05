package net.ion.ice.core.node;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "i18n")
public class I18nConfiguration {
    private String defaults ;

    public String getDefaults() {
        return defaults;
    }

    public void setDefaults(String defaults) {
        this.defaults = defaults;
    }
}
