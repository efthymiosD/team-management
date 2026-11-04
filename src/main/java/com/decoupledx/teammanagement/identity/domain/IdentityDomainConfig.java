package com.decoupledx.teammanagement.identity.domain;

import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.identity.domain.port.AccountRepository;
import com.decoupledx.teammanagement.identity.domain.port.AccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain-internal wiring; nothing here is visible to other modules.
 */
@Configuration
class IdentityDomainConfig {

    @Bean
    AccountService accountService(AccountRepository repository, TransactionRunner tx) {
        return new AccountServiceImpl(repository, tx);
    }
}
