package com.amcbank.simple.controller;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.amcbank.simple.dto.FinancialHealthResponse;
import com.amcbank.simple.dto.FixedDepositRequest;
import com.amcbank.simple.dto.ForexConversionRequest;
import com.amcbank.simple.dto.ForexConversionResponse;
import com.amcbank.simple.dto.FraudEvaluationResult;
import com.amcbank.simple.dto.StatementResponse;
import com.amcbank.simple.dto.TransferRequest;
import com.amcbank.simple.model.Account;
import com.amcbank.simple.model.AppUser;
import com.amcbank.simple.model.Customer;
import com.amcbank.simple.model.FixedDeposit;
import com.amcbank.simple.model.Loan;
import com.amcbank.simple.repository.AccountRepository;
import com.amcbank.simple.repository.UserRepository;
import com.amcbank.simple.security.JwtService;
import com.amcbank.simple.service.BankService;
import com.amcbank.simple.service.FraudDetectionService;

@RestController
@RequestMapping("/api")
public class BankController {

    private final BankService service;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final FraudDetectionService fraudDetectionService;

    public BankController(BankService service,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          AccountRepository accountRepository,
                          FraudDetectionService fraudDetectionService) {
        this.service = service;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
        this.fraudDetectionService = fraudDetectionService;
    }

    // Public login for both admin and customer.
    // Admin username: admin. Customer username: customer's email address.
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        AppUser user = userRepository
                .findByUsernameIgnoreCase(username == null ? "" : username.trim())
                .orElse(null);

        if (user == null || password == null
                || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username/email or password"));
        }

        // Normalize legacy database values such as ADMIN -> ROLE_ADMIN.
        String normalizedRole = normalizeRole(user.getRole());
        if (!normalizedRole.equals(user.getRole())) {
            user.setRole(normalizedRole);
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getUsername(), normalizedRole);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", normalizedRole));
    }

    // Lets the frontend confirm the identity/authority established by the JWT filter.
    @GetMapping("/auth/me")
    public Map<String, Object> authenticatedUser(Authentication authentication) {
        return Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .toList());
    }

    // -------------------- FOREX & MULTI-CURRENCY (PUBLIC) --------------------

    @GetMapping("/forex/rates")
    public Map<String, Object> forexRates() {
        return service.getForexRates();
    }

    @PostMapping("/forex/convert")
    public ForexConversionResponse convertForex(@RequestBody ForexConversionRequest request) {
        return service.convertForex(request);
    }

    // -------------------- ADMIN APIs --------------------

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return service.getRichDashboard();
    }

    @GetMapping("/customers")
    public Object customers(@RequestParam(defaultValue = "") String search) {
        return service.getCustomers(search);
    }

    @GetMapping("/customers/{id}")
    public Customer customer(@PathVariable Long id) {
        return service.getCustomer(id);
    }

    @PostMapping("/customers")
    public ResponseEntity<Customer> addCustomer(@RequestBody Customer customer) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.addCustomer(customer));
    }

    @PutMapping("/customers/{id}")
    public Customer updateCustomer(@PathVariable Long id,
                                   @RequestBody Customer customer) {
        return service.updateCustomer(id, customer);
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        service.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customers/{customerId}/accounts")
    public Object accounts(@PathVariable Long customerId) {
        return service.getAccounts(customerId);
    }

    @PostMapping("/customers/{customerId}/accounts")
    public ResponseEntity<Account> openAccount(@PathVariable Long customerId,
                                               @RequestBody Account account) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.openAccount(customerId, account));
    }

    @PostMapping("/accounts/{accountId}/deposit")
    public Account deposit(@PathVariable Long accountId,
                           @RequestParam BigDecimal amount) {
        return service.deposit(accountId, amount);
    }

    @PostMapping("/accounts/{accountId}/withdraw")
    public Account withdraw(@PathVariable Long accountId,
                            @RequestParam BigDecimal amount) {
        return service.withdraw(accountId, amount);
    }

    // Admin P2P Fund Transfer
    @PostMapping("/accounts/transfer")
    public ResponseEntity<?> transferFundsAdmin(@RequestBody TransferRequest request) {
        return ResponseEntity.ok(service.transferFunds(null, request));
    }

    // Transaction & Audit History for Admin
    @GetMapping("/transactions")
    public Object allTransactions() {
        return service.getAllTransactions();
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public Object accountTransactions(@PathVariable Long accountId) {
        return service.getAccountTransactions(null, accountId);
    }

    @GetMapping("/accounts/{accountId}/statement")
    public StatementResponse accountStatementAdmin(@PathVariable Long accountId) {
        return service.generateStatement(null, accountId);
    }

    // Security & Fraud Detection Center
    @GetMapping("/security/fraud-alerts")
    public Object fraudAlerts() {
        return service.getFlaggedTransactions();
    }

    @PostMapping("/security/evaluate-risk")
    public FraudEvaluationResult evaluateRisk(@RequestBody Map<String, Object> payload) {
        Long accountId = Long.valueOf(payload.get("accountId").toString());
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        String type = payload.getOrDefault("type", "WITHDRAWAL").toString();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));

        return fraudDetectionService.evaluateTransaction(account, amount, type);
    }

    // Term Investments (Fixed Deposits) for Admin
    @GetMapping("/fixed-deposits")
    public Object allFixedDeposits() {
        return service.getAllFixedDeposits();
    }

    // Customer Credit Assessment for Admin
    @GetMapping("/customers/{customerId}/financial-health")
    public FinancialHealthResponse customerFinancialHealth(@PathVariable Long customerId) {
        return service.getCustomerFinancialHealth(customerId);
    }

    @GetMapping("/loans")
    public Object allLoans() {
        return service.getAllLoans();
    }

    @GetMapping("/customers/{customerId}/loans")
    public Object loans(@PathVariable Long customerId) {
        return service.getLoans(customerId);
    }

    @PostMapping("/customers/{customerId}/loans")
    public ResponseEntity<Loan> sanctionLoan(@PathVariable Long customerId,
                                             @RequestBody Loan loan) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.sanctionLoan(customerId, loan));
    }

    // -------------------- CUSTOMER SELF-SERVICE APIs --------------------

    @GetMapping("/customer/me")
    public Customer myProfile(Authentication authentication) {
        return service.getCustomerForUsername(authentication.getName());
    }

    @GetMapping("/customer/me/accounts")
    public Object myAccounts(Authentication authentication) {
        return service.getMyAccounts(authentication.getName());
    }

    @GetMapping("/customer/me/loans")
    public Object myLoans(Authentication authentication) {
        return service.getMyLoans(authentication.getName());
    }

    @PostMapping("/customer/me/accounts/{accountId}/deposit")
    public Account depositMyAccount(Authentication authentication,
                                    @PathVariable Long accountId,
                                    @RequestParam BigDecimal amount) {
        return service.depositMyAccount(authentication.getName(), accountId, amount);
    }

    @PostMapping("/customer/me/accounts/{accountId}/withdraw")
    public Account withdrawMyAccount(Authentication authentication,
                                     @PathVariable Long accountId,
                                     @RequestParam BigDecimal amount) {
        return service.withdrawMyAccount(authentication.getName(), accountId, amount);
    }

    // Customer Peer-to-Peer Fund Transfer
    @PostMapping("/customer/me/transfer")
    public ResponseEntity<?> transferMyFunds(Authentication authentication,
                                             @RequestBody TransferRequest request) {
        return ResponseEntity.ok(service.transferFunds(authentication.getName(), request));
    }

    // Customer Transaction Ledger & History
    @GetMapping("/customer/me/transactions")
    public Object myTransactions(Authentication authentication) {
        return service.getMyTransactions(authentication.getName());
    }

    @GetMapping("/customer/me/accounts/{accountId}/transactions")
    public Object myAccountTransactions(Authentication authentication,
                                        @PathVariable Long accountId) {
        return service.getAccountTransactions(authentication.getName(), accountId);
    }

    // Printable & Structured Account Statement
    @GetMapping("/customer/me/accounts/{accountId}/statement")
    public StatementResponse myAccountStatement(Authentication authentication,
                                                @PathVariable Long accountId) {
        return service.generateStatement(authentication.getName(), accountId);
    }

    // Term Deposits / Fixed Deposits (FDs)
    @PostMapping("/customer/me/fixed-deposits")
    public ResponseEntity<FixedDeposit> createFixedDeposit(Authentication authentication,
                                                           @RequestBody FixedDepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.openFixedDeposit(authentication.getName(), request));
    }

    @GetMapping("/customer/me/fixed-deposits")
    public Object myFixedDeposits(Authentication authentication) {
        return service.getMyFixedDeposits(authentication.getName());
    }

    @PostMapping("/customer/me/fixed-deposits/{fdId}/close")
    public ResponseEntity<?> closeMyFixedDeposit(Authentication authentication,
                                                 @PathVariable Long fdId) {
        return ResponseEntity.ok(service.closeFixedDeposit(authentication.getName(), fdId));
    }

    // Customer Financial Health & CIBIL-style Credit Score
    @GetMapping("/customer/me/financial-health")
    public FinancialHealthResponse myFinancialHealth(Authentication authentication) {
        return service.getMyFinancialHealth(authentication.getName());
    }

    @PostMapping("/customer/me/password")
    public ResponseEntity<?> changeMyPassword(Authentication authentication,
                                              @RequestBody Map<String, String> request) {
        service.changeCustomerPassword(
                authentication.getName(),
                request.get("currentPassword"),
                request.get("newPassword"));

        return ResponseEntity.ok(Map.of(
                "message", "Password changed successfully. Use the new password next time you login."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", ex.getMessage()));
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
