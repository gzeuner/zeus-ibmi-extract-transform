package de.zeus.ibmi.infrastructure.cli;

import java.util.Map;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "de.zeus.ibmi")
@ConfigurationPropertiesScan(basePackages = "de.zeus.ibmi.infrastructure.config")
public class ZeusIbmiSpringLauncher {

  @Bean
  CliApplication cliApplication() {
    return new CliApplication(System.getenv());
  }

  @Bean
  Map<String, String> processEnvironment() {
    return System.getenv();
  }
}
