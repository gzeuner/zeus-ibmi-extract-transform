package de.zeus.ibmi.infrastructure.cli;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

public final class Main {

  private Main() {}

  public static void main(String[] args) {
    SpringApplication springApplication = new SpringApplication(ZeusIbmiSpringLauncher.class);
    springApplication.setBannerMode(Banner.Mode.OFF);
    springApplication.setLogStartupInfo(false);
    springApplication.setWebApplicationType(WebApplicationType.NONE);
    ConfigurableApplicationContext context = springApplication.run();
    try {
      CliApplication cliApplication = context.getBean(CliApplication.class);
      int exitCode = cliApplication.run(args, System.out, System.err);
      System.exit(exitCode);
    } finally {
      context.close();
    }
  }
}
