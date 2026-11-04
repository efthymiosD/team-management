package com.decoupledx.teammanagement.identity.adapter.api;

import com.decoupledx.teammanagement.identity.domain.port.AccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IdentityApiConfig {

    @Bean
    AccountDirectoryApi accountDirectoryApi(AccountService accountService) {
        return new AccountDirectoryApiImpl(accountService);
    }
}
