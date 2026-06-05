package com.capstone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("com.capstone.domain")
@EnableJpaRepositories("com.capstone.domain")
@ComponentScan(
    basePackages = "com.capstone",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = {
            "com\\.capstone\\.domain\\.(user|workspaceUser|workspaceInvite|workspaceInvitation|canvas|chat|voicesession|voicesessionUser)\\..*",
            "com\\.capstone\\.domain\\.workspace\\..*",
            "com\\.capstone\\.domain\\.idea\\.(IdeaController|IdeaService|IdeasContentColumnMigration)",
            "com\\.capstone\\.global\\.oauth\\.controller\\..*"
        }
    )
)
public class CapstoneApplication {

  public static void main(String[] args) {
    SpringApplication.run(CapstoneApplication.class, args);
  }
}
