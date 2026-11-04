package com.decoupledx.teammanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "team-management")
@SpringBootApplication
public class TeamManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamManagementApplication.class, args);
    }
}
