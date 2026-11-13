package com.amcbank.simple;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.amcbank.simple.model.Account;
import com.amcbank.simple.model.AppUser;
import com.amcbank.simple.model.Customer;
import com.amcbank.simple.model.Transaction;
import com.amcbank.simple.repository.AccountRepository;
import com.amcbank.simple.repository.CustomerRepository;
import com.amcbank.simple.repository.TransactionRepository;
import com.amcbank.simple.repository.UserRepository;

@SpringBootApplication
public class AmcBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmcBankApplication.class, args);
    }

    @Bean
    CommandLineRunner initializeUsers(UserRepository userRepository,
                                      CustomerRepository customerRepository,
                                      AccountRepository accountRepository,
                                      TransactionRepository transactionRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            // Repair legacy role values such as ADMIN/CUSTOMER if an older database
            // is reused. Spring Security will then consistently see ROLE_ADMIN or
            // ROLE_CUSTOMER.
            userRepository.findAll().forEach(user -> {
                String normalized = normalizeRole(user.getRole());
                if (!normalized.equals(user.getRole())) {
                    user.setRole(normalized);
                    userRepository.save(user);
                }
            });

            // 1. Seed Classroom Admin (admin / admin123)
            AppUser admin = userRepository.findByUsernameIgnoreCase("admin").orElse(null);
            if (admin == null) {
                userRepository.save(new AppUser(
                        "admin",
                        passwordEncoder.encode("admin123"),
                        "ROLE_ADMIN"));
            } else if (!"ROLE_ADMIN".equals(admin.getRole())) {
                admin.setRole("ROLE_ADMIN");
                userRepository.save(admin);
            }

            // 2. Seed Default Demo Customer & Accounts for Instant Classroom Presentations
            if (customerRepository.count() == 0) {
                Customer demoCustomer = new Customer("Rahul Sharma", "rahul@gmail.com", "Mumbai", "ABCDE1234F");
                Customer savedCustomer = customerRepository.save(demoCustomer);

                // Create customer login
                AppUser customerUser = new AppUser(
                        savedCustomer.getEmail(),
                        passwordEncoder.encode("password123"),
                        "ROLE_CUSTOMER",
                        savedCustomer
                );
                userRepository.save(customerUser);

                // Create Savings Account
                Account savingsAccount = new Account();
                savingsAccount.setAccountNumber("AC10012026");
                savingsAccount.setType("SAVINGS");
                savingsAccount.setBalance(new BigDecimal("75000.00"));
                savingsAccount.setStatus("ACTIVE");
                savingsAccount.setCustomer(savedCustomer);
                Account savedSavings = accountRepository.save(savingsAccount);

                // Create Current Account
                Account currentAccount = new Account();
                currentAccount.setAccountNumber("AC20022026");
                currentAccount.setType("CURRENT");
                currentAccount.setBalance(new BigDecimal("25000.00"));
                currentAccount.setStatus("ACTIVE");
                currentAccount.setCustomer(savedCustomer);
                Account savedCurrent = accountRepository.save(currentAccount);

                // Log opening transactions
                Transaction t1 = new Transaction(
                        "TXN-INIT-001",
                        "DEPOSIT",
                        new BigDecimal("75000.00"),
                        new BigDecimal("75000.00"),
                        "Welcome Bonus & Initial Deposit",
                        "AMC_BRANCH_MUMBAI",
                        savedSavings.getAccountNumber(),
                        0,
                        "SUCCESS",
                        savedSavings
                );
                t1.setTimestamp(LocalDateTime.now().minusDays(2));
                transactionRepository.save(t1);

                Transaction t2 = new Transaction(
                        "TXN-INIT-002",
                        "DEPOSIT",
                        new BigDecimal("25000.00"),
                        new BigDecimal("25000.00"),
                        "Current Account Opening Balance",
                        "AMC_BRANCH_MUMBAI",
                        savedCurrent.getAccountNumber(),
                        0,
                        "SUCCESS",
                        savedCurrent
                );
                t2.setTimestamp(LocalDateTime.now().minusDays(1));
                transactionRepository.save(t2);
            }
        };
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_CUSTOMER";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_")
                ? normalized
                : "ROLE_" + normalized;
    }
}
