package com.amcbank.simple.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.amcbank.simple.model.Transaction;
import com.amcbank.simple.repository.AccountRepository;
import com.amcbank.simple.repository.CustomerRepository;
import com.amcbank.simple.repository.FixedDepositRepository;
import com.amcbank.simple.repository.LoanRepository;
import com.amcbank.simple.repository.TransactionRepository;
import com.amcbank.simple.repository.UserRepository;

@Service
public class BankService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionRepository transactionRepository;
    private final FixedDepositRepository fixedDepositRepository;
    private final FraudDetectionService fraudDetectionService;
    private final FinancialHealthService financialHealthService;
    private final ForexService forexService;

    public BankService(CustomerRepository customerRepository,
                       AccountRepository accountRepository,
                       LoanRepository loanRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TransactionRepository transactionRepository,
                       FixedDepositRepository fixedDepositRepository,
                       FraudDetectionService fraudDetectionService,
                       FinancialHealthService financialHealthService,
                       ForexService forexService) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionRepository = transactionRepository;
        this.fixedDepositRepository = fixedDepositRepository;
        this.fraudDetectionService = fraudDetectionService;
        this.financialHealthService = financialHealthService;
        this.forexService = forexService;
    }

    public List<Customer> getCustomers(String search) {
        if (search == null || search.isBlank()) {
            return customerRepository.findAll();
        }
        return customerRepository
                .findByNameContainingIgnoreCaseOrCityContainingIgnoreCase(search, search);
    }

    public Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    @Transactional
    public Customer addCustomer(Customer customer) {
        normalizeCustomer(customer);
        validateCustomer(customer, null);
        validateNewPassword(customer.getInitialPassword());

        if (userRepository.existsByUsernameIgnoreCase(customer.getEmail())) {
            throw new IllegalArgumentException("A login already exists for this email");
        }

        Customer saved = customerRepository.save(customer);

        AppUser login = new AppUser(
                saved.getEmail(),
                passwordEncoder.encode(customer.getInitialPassword()),
                "ROLE_CUSTOMER",
                saved);
        userRepository.save(login);

        return saved;
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer input) {
        Customer customer = getCustomer(id);
        normalizeCustomer(input);
        validateCustomer(input, customer);

        String oldEmail = customer.getEmail();
        boolean emailChanged = !input.getEmail().equalsIgnoreCase(oldEmail);

        if (emailChanged && userRepository.existsByUsernameIgnoreCase(input.getEmail())) {
            throw new IllegalArgumentException("A login already exists for this email");
        }

        customer.setName(input.getName());
        customer.setEmail(input.getEmail());
        customer.setCity(input.getCity());
        customer.setPanNumber(input.getPanNumber());
        Customer saved = customerRepository.save(customer);

        if (emailChanged) {
            AppUser login = userRepository.findByCustomerId(id)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Customer login account not found"));
            login.setUsername(saved.getEmail());
            userRepository.save(login);
        }

        return saved;
    }

    @Transactional
    public void deleteCustomer(Long id) {
        getCustomer(id);
        if (!accountRepository.findByCustomerId(id).isEmpty()
                || !loanRepository.findByCustomerId(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "Customer cannot be deleted while accounts or loans exist");
        }

        userRepository.findByCustomerId(id).ifPresent(user -> {
            userRepository.delete(user);
            userRepository.flush();
        });
        customerRepository.deleteById(id);
    }

    private void normalizeCustomer(Customer customer) {
        if (customer.getName() != null) {
            customer.setName(customer.getName().trim());
        }
        if (customer.getEmail() != null) {
            customer.setEmail(customer.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        if (customer.getCity() != null) {
            customer.setCity(customer.getCity().trim());
        }
        if (customer.getPanNumber() != null) {
            customer.setPanNumber(customer.getPanNumber().trim().toUpperCase(Locale.ROOT));
        }
    }

    private void validateCustomer(Customer input, Customer existing) {
        if (input.getName() == null || input.getName().isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (input.getEmail() == null || !input.getEmail().contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        if (input.getPanNumber() == null || input.getPanNumber().isBlank()) {
            throw new IllegalArgumentException("PAN number is required");
        }

        boolean emailChanged = existing == null
                || !input.getEmail().equalsIgnoreCase(existing.getEmail());
        boolean panChanged = existing == null
                || !input.getPanNumber().equalsIgnoreCase(existing.getPanNumber());

        if (emailChanged && customerRepository.existsByEmail(input.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (panChanged && customerRepository.existsByPanNumber(input.getPanNumber())) {
            throw new IllegalArgumentException("PAN already registered");
        }
    }

    private void validateNewPassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException(
                    "Customer password must contain at least 6 characters");
        }
    }

    // -------------------- CUSTOMER SELF-SERVICE --------------------

    public Customer getCustomerForUsername(String username) {
        AppUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Login account not found"));

        if (user.getCustomer() == null) {
            throw new IllegalArgumentException("No customer is linked to this login");
        }
        return user.getCustomer();
    }

    public List<Account> getMyAccounts(String username) {
        Customer customer = getCustomerForUsername(username);
        return accountRepository.findByCustomerId(customer.getId());
    }

    public List<Loan> getMyLoans(String username) {
        Customer customer = getCustomerForUsername(username);
        return loanRepository.findByCustomerId(customer.getId());
    }

    @Transactional
    public Account depositMyAccount(String username, Long accountId, BigDecimal amount) {
        Account account = getOwnedActiveAccount(username, accountId);
        validatePositiveAmount(amount);

        FraudEvaluationResult risk = fraudDetectionService.evaluateTransaction(account, amount, "DEPOSIT");
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        Account saved = accountRepository.save(account);

        recordTransaction(
                account,
                "DEPOSIT",
                amount,
                newBalance,
                "Customer Self-Service Deposit",
                "SELF/CASH",
                account.getAccountNumber(),
                risk.getRiskScore(),
                risk.isFlagged() ? "FLAGGED" : "SUCCESS"
        );

        return saved;
    }

    @Transactional
    public Account withdrawMyAccount(String username, Long accountId, BigDecimal amount) {
        Account account = getOwnedActiveAccount(username, accountId);
        validatePositiveAmount(amount);

        if (amount.compareTo(account.getBalance()) > 0) {
            throw new IllegalArgumentException(
                    "Withdrawal amount cannot exceed available balance of " + account.getBalance());
        }

        FraudEvaluationResult risk = fraudDetectionService.evaluateTransaction(account, amount, "WITHDRAWAL");
        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        Account saved = accountRepository.save(account);

        recordTransaction(
                account,
                "WITHDRAWAL",
                amount,
                newBalance,
                "Customer Self-Service Withdrawal",
                account.getAccountNumber(),
                "ATM/CASH",
                risk.getRiskScore(),
                risk.isFlagged() ? "FLAGGED" : "SUCCESS"
        );

        return saved;
    }

    private Account getOwnedActiveAccount(String username, Long accountId) {
        Customer customer = getCustomerForUsername(username);
        Account account = getActiveAccount(accountId);

        if (account.getCustomer() == null
                || !account.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException(
                    "You can perform transactions only on your own account");
        }
        return account;
    }

    @Transactional
    public void changeCustomerPassword(String username,
                                       String currentPassword,
                                       String newPassword) {
        AppUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Login account not found"));

        if (!"ROLE_CUSTOMER".equals(user.getRole()) || user.getCustomer() == null) {
            throw new IllegalArgumentException("Password change is available to customers here");
        }
        if (currentPassword == null
                || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        validateNewPassword(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException(
                    "New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // -------------------- ACCOUNTS & ADMIN TRANSACTIONS --------------------

    public List<Account> getAccounts(Long customerId) {
        getCustomer(customerId);
        return accountRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Account openAccount(Long customerId, Account input) {
        Customer customer = getCustomer(customerId);

        if (input.getType() == null
                || !(input.getType().equalsIgnoreCase("SAVINGS")
                || input.getType().equalsIgnoreCase("CURRENT"))) {
            throw new IllegalArgumentException("Account type must be SAVINGS or CURRENT");
        }

        BigDecimal openingBalance = input.getBalance() == null
                ? BigDecimal.ZERO : input.getBalance();
        if (openingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative");
        }

        Account account = new Account();
        account.setAccountNumber("AC" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase());
        account.setType(input.getType().toUpperCase());
        account.setBalance(openingBalance);
        account.setStatus("ACTIVE");
        account.setCustomer(customer);
        Account saved = accountRepository.save(account);

        if (openingBalance.compareTo(BigDecimal.ZERO) > 0) {
            recordTransaction(
                    saved,
                    "DEPOSIT",
                    openingBalance,
                    openingBalance,
                    "Account Opening Initial Deposit",
                    "BRANCH/CASH",
                    saved.getAccountNumber(),
                    0,
                    "SUCCESS"
            );
        }

        return saved;
    }

    @Transactional
    public Account deposit(Long accountId, BigDecimal amount) {
        Account account = getActiveAccount(accountId);
        validatePositiveAmount(amount);

        FraudEvaluationResult risk = fraudDetectionService.evaluateTransaction(account, amount, "DEPOSIT");
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        Account saved = accountRepository.save(account);

        recordTransaction(
                saved,
                "DEPOSIT",
                amount,
                newBalance,
                "Branch Cash Deposit",
                "BRANCH_COUNTER",
                saved.getAccountNumber(),
                risk.getRiskScore(),
                risk.isFlagged() ? "FLAGGED" : "SUCCESS"
        );

        return saved;
    }

    @Transactional
    public Account withdraw(Long accountId, BigDecimal amount) {
        Account account = getActiveAccount(accountId);
        validatePositiveAmount(amount);

        if (amount.compareTo(account.getBalance()) > 0) {
            throw new IllegalArgumentException(
                    "Withdrawal amount cannot exceed available balance of " + account.getBalance());
        }

        FraudEvaluationResult risk = fraudDetectionService.evaluateTransaction(account, amount, "WITHDRAWAL");
        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        Account saved = accountRepository.save(account);

        recordTransaction(
                saved,
                "WITHDRAWAL",
                amount,
                newBalance,
                "Branch Cash Withdrawal",
                saved.getAccountNumber(),
                "BRANCH_COUNTER",
                risk.getRiskScore(),
                risk.isFlagged() ? "FLAGGED" : "SUCCESS"
        );

        return saved;
    }

    // -------------------- PEER-TO-PEER FUND TRANSFER --------------------

    @Transactional
    public Map<String, Object> transferFunds(String authenticatedUsername, TransferRequest request) {
        if (request.getFromAccountId() == null) {
            throw new IllegalArgumentException("Source account ID is required");
        }
        if (request.getToAccountNumber() == null || request.getToAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Destination account number is required");
        }
        validatePositiveAmount(request.getAmount());

        Account fromAccount = getActiveAccount(request.getFromAccountId());

        // Verify customer owns the source account if initiated by customer
        if (authenticatedUsername != null) {
            Customer customer = getCustomerForUsername(authenticatedUsername);
            if (fromAccount.getCustomer() == null || !fromAccount.getCustomer().getId().equals(customer.getId())) {
                throw new IllegalArgumentException("You can transfer funds only from your own account");
            }
        }

        String toAccNum = request.getToAccountNumber().trim().toUpperCase();
        Account toAccount = accountRepository.findByAccountNumber(toAccNum)
                .orElseThrow(() -> new IllegalArgumentException("Recipient account " + toAccNum + " not found"));

        if (!"ACTIVE".equalsIgnoreCase(toAccount.getStatus())) {
            throw new IllegalArgumentException("Recipient account is not active");
        }

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("Sender and recipient accounts cannot be the same");
        }

        if (request.getAmount().compareTo(fromAccount.getBalance()) > 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: ₹" + fromAccount.getBalance());
        }

        // Real-time AI/Rule-based Fraud and Anomaly Assessment
        FraudEvaluationResult risk = fraudDetectionService.evaluateTransaction(fromAccount, request.getAmount(), "TRANSFER_OUT");

        // Execute atomic debit and credit
        BigDecimal senderNewBalance = fromAccount.getBalance().subtract(request.getAmount());
        BigDecimal recipientNewBalance = toAccount.getBalance().add(request.getAmount());

        fromAccount.setBalance(senderNewBalance);
        toAccount.setBalance(recipientNewBalance);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        String ref = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String narration = (request.getDescription() != null && !request.getDescription().isBlank())
                ? request.getDescription().trim()
                : "Inter-account fund transfer";

        // Ledger Entry 1: Sender (TRANSFER_OUT)
        Transaction debitTxn = new Transaction(
                ref + "-DR",
                "TRANSFER_OUT",
                request.getAmount(),
                senderNewBalance,
                narration + " to " + toAccount.getAccountNumber() + " (" + toAccount.getCustomer().getName() + ")",
                fromAccount.getAccountNumber(),
                toAccount.getAccountNumber(),
                risk.getRiskScore(),
                risk.isFlagged() ? "FLAGGED" : "SUCCESS",
                fromAccount
        );
        transactionRepository.save(debitTxn);

        // Ledger Entry 2: Recipient (TRANSFER_IN)
        Transaction creditTxn = new Transaction(
                ref + "-CR",
                "TRANSFER_IN",
                request.getAmount(),
                recipientNewBalance,
                narration + " from " + fromAccount.getAccountNumber() + " (" + fromAccount.getCustomer().getName() + ")",
                fromAccount.getAccountNumber(),
                toAccount.getAccountNumber(),
                0,
                "SUCCESS",
                toAccount
        );
        transactionRepository.save(creditTxn);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("referenceNumber", ref);
        response.put("status", "SUCCESS");
        response.put("amount", request.getAmount());
        response.put("fromAccountNumber", fromAccount.getAccountNumber());
        response.put("toAccountNumber", toAccount.getAccountNumber());
        response.put("recipientName", toAccount.getCustomer().getName());
        response.put("senderRemainingBalance", senderNewBalance);
        response.put("riskScore", risk.getRiskScore());
        response.put("riskLevel", risk.getRiskLevel());
        response.put("flaggedForReview", risk.isFlagged());
        response.put("timestamp", LocalDateTime.now());

        return response;
    }

    // -------------------- TRANSACTION & STATEMENT RETRIEVAL --------------------

    public List<Transaction> getMyTransactions(String username) {
        Customer customer = getCustomerForUsername(username);
        return transactionRepository.findByCustomerIdOrderByTimestampDesc(customer.getId());
    }

    public List<Transaction> getAccountTransactions(String username, Long accountId) {
        if (username != null) {
            getOwnedActiveAccount(username, accountId);
        }
        return transactionRepository.findByAccountIdOrderByTimestampDesc(accountId);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByTimestampDesc();
    }

    public List<Transaction> getFlaggedTransactions() {
        return transactionRepository.findByStatusOrderByTimestampDesc("FLAGGED");
    }

    public StatementResponse generateStatement(String username, Long accountId) {
        Account account = (username != null)
                ? getOwnedActiveAccount(username, accountId)
                : accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Customer customer = account.getCustomer();
        List<Transaction> txns = transactionRepository.findByAccountIdOrderByTimestampDesc(accountId);

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;

        for (Transaction t : txns) {
            if ("DEPOSIT".equalsIgnoreCase(t.getType()) || "TRANSFER_IN".equalsIgnoreCase(t.getType())) {
                totalDeposits = totalDeposits.add(t.getAmount());
            } else {
                totalWithdrawals = totalWithdrawals.add(t.getAmount());
            }
        }

        BigDecimal netCashFlow = totalDeposits.subtract(totalWithdrawals);
        BigDecimal closingBalance = account.getBalance();
        BigDecimal openingBalance = closingBalance.subtract(netCashFlow);

        StatementResponse resp = new StatementResponse();
        resp.setAccountNumber(account.getAccountNumber());
        resp.setAccountType(account.getType());
        resp.setCustomerName(customer.getName());
        resp.setCustomerEmail(customer.getEmail());
        resp.setGeneratedAt(LocalDateTime.now());
        resp.setOpeningBalance(openingBalance);
        resp.setClosingBalance(closingBalance);
        resp.setTotalDeposits(totalDeposits);
        resp.setTotalWithdrawals(totalWithdrawals);
        resp.setNetCashFlow(netCashFlow);
        resp.setTotalTransactions(txns.size());
        resp.setTransactions(txns);

        // Generate ASCII printable statement
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
        sb.append("========================================================================================\n");
        sb.append("                                AMC BANK LIMITED                                       \n");
        sb.append("                          OFFICIAL ACCOUNT STATEMENT                                   \n");
        sb.append("========================================================================================\n");
        sb.append(String.format("Customer Name  : %-30s  Date Generated: %s\n", customer.getName(), LocalDateTime.now().format(dtf)));
        sb.append(String.format("Account Number : %-30s  Account Type  : %s\n", account.getAccountNumber(), account.getType()));
        sb.append(String.format("Current Balance: INR %-26s  Status        : %s\n", account.getBalance().setScale(2, RoundingMode.HALF_UP), account.getStatus()));
        sb.append("----------------------------------------------------------------------------------------\n");
        sb.append(String.format("%-18s | %-16s | %-14s | %12s | %12s\n", "Date / Time", "Reference", "Type", "Amount (INR)", "Balance (INR)"));
        sb.append("----------------------------------------------------------------------------------------\n");

        if (txns.isEmpty()) {
            sb.append("                      No transaction history found for this account.                    \n");
        } else {
            for (Transaction t : txns) {
                String sign = ("DEPOSIT".equalsIgnoreCase(t.getType()) || "TRANSFER_IN".equalsIgnoreCase(t.getType())) ? "+" : "-";
                sb.append(String.format("%-18s | %-16s | %-14s | %12s | %12s\n",
                        t.getTimestamp().format(dtf),
                        t.getTransactionReference().length() > 16 ? t.getTransactionReference().substring(0, 16) : t.getTransactionReference(),
                        t.getType(),
                        sign + t.getAmount().setScale(2, RoundingMode.HALF_UP),
                        t.getBalanceAfter().setScale(2, RoundingMode.HALF_UP)));
            }
        }
        sb.append("----------------------------------------------------------------------------------------\n");
        sb.append(String.format("Total Deposits : INR %-18s Total Withdrawals: INR %s\n",
                totalDeposits.setScale(2, RoundingMode.HALF_UP), totalWithdrawals.setScale(2, RoundingMode.HALF_UP)));
        sb.append(String.format("Net Cash Flow  : INR %-18s Closing Balance   : INR %s\n",
                netCashFlow.setScale(2, RoundingMode.HALF_UP), closingBalance.setScale(2, RoundingMode.HALF_UP)));
        sb.append("========================================================================================\n");
        sb.append("                  Thank you for banking with AMC Bank - Always by your side.            \n");
        sb.append("========================================================================================\n");

        resp.setAsciiPassbook(sb.toString());
        return resp;
    }

    // -------------------- FIXED DEPOSITS (TERM INVESTMENTS) --------------------

    @Transactional
    public FixedDeposit openFixedDeposit(String username, FixedDepositRequest request) {
        Customer customer = getCustomerForUsername(username);

        if (request.getSourceAccountId() == null) {
            throw new IllegalArgumentException("Funding account ID is required");
        }
        if (request.getDepositAmount() == null || request.getDepositAmount().compareTo(new BigDecimal("5000")) < 0) {
            throw new IllegalArgumentException("Minimum Fixed Deposit amount is ₹5,000");
        }
        if (request.getTenureMonths() < 3 || request.getTenureMonths() > 120) {
            throw new IllegalArgumentException("Tenure must be between 3 and 120 months");
        }

        Account sourceAccount = getOwnedActiveAccount(username, request.getSourceAccountId());
        if (request.getDepositAmount().compareTo(sourceAccount.getBalance()) > 0) {
            throw new IllegalArgumentException("Insufficient balance in account " + sourceAccount.getAccountNumber());
        }

        // Progressive interest rate curve based on tenure
        double rate;
        if (request.getTenureMonths() < 12) {
            rate = 5.75;
        } else if (request.getTenureMonths() < 24) {
            rate = 7.10;
        } else if (request.getTenureMonths() < 36) {
            rate = 7.50;
        } else {
            rate = 8.00;
        }

        // Debit funding account
        BigDecimal newBal = sourceAccount.getBalance().subtract(request.getDepositAmount());
        sourceAccount.setBalance(newBal);
        accountRepository.save(sourceAccount);

        String fdNum = "FD" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        FixedDeposit fd = new FixedDeposit();
        fd.setFdNumber(fdNum);
        fd.setCustomer(customer);
        fd.setFundingAccountNumber(sourceAccount.getAccountNumber());
        fd.setDepositAmount(request.getDepositAmount());
        fd.setInterestRate(rate);
        fd.setTenureMonths(request.getTenureMonths());
        fd.setStartDate(LocalDate.now());
        fd.setMaturityDate(LocalDate.now().plusMonths(request.getTenureMonths()));
        fd.setStatus("ACTIVE");
        fd.calculateMaturity();

        FixedDeposit saved = fixedDepositRepository.save(fd);

        recordTransaction(
                sourceAccount,
                "WITHDRAWAL",
                request.getDepositAmount(),
                newBal,
                "Fixed Deposit Created: " + fdNum + " (Tenure: " + request.getTenureMonths() + "m @ " + rate + "%)",
                sourceAccount.getAccountNumber(),
                fdNum,
                0,
                "SUCCESS"
        );

        return saved;
    }

    @Transactional
    public Map<String, Object> closeFixedDeposit(String username, Long fdId) {
        Customer customer = getCustomerForUsername(username);
        FixedDeposit fd = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new IllegalArgumentException("Fixed Deposit not found"));

        if (!fd.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("You can close only your own Fixed Deposits");
        }
        if (!"ACTIVE".equalsIgnoreCase(fd.getStatus())) {
            throw new IllegalArgumentException("Fixed Deposit is already closed");
        }

        Account targetAccount = accountRepository.findByAccountNumber(fd.getFundingAccountNumber())
                .orElse(null);

        if (targetAccount == null || !"ACTIVE".equalsIgnoreCase(targetAccount.getStatus())) {
            List<Account> activeAccounts = accountRepository.findByCustomerId(customer.getId());
            targetAccount = activeAccounts.stream()
                    .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No active account available to receive FD proceeds"));
        }

        // If closed on or after maturity date, payout full maturity amount; if premature, payout principal + 4% simple interest
        BigDecimal payout;
        boolean premature = LocalDate.now().isBefore(fd.getMaturityDate());
        if (!premature) {
            payout = fd.getMaturityAmount();
        } else {
            // Premature liquidation: Principal + 4.0% annualized interest
            BigDecimal interest = fd.getDepositAmount().multiply(new BigDecimal("0.04")).setScale(2, RoundingMode.HALF_UP);
            payout = fd.getDepositAmount().add(interest);
        }

        BigDecimal newBal = targetAccount.getBalance().add(payout);
        targetAccount.setBalance(newBal);
        accountRepository.save(targetAccount);

        fd.setStatus("CLOSED");
        fixedDepositRepository.save(fd);

        recordTransaction(
                targetAccount,
                "DEPOSIT",
                payout,
                newBal,
                "Fixed Deposit Liquidation: " + fd.getFdNumber() + (premature ? " (Premature)" : " (Matured)"),
                fd.getFdNumber(),
                targetAccount.getAccountNumber(),
                0,
                "SUCCESS"
        );

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("fdNumber", fd.getFdNumber());
        res.put("status", "CLOSED");
        res.put("payoutAmount", payout);
        res.put("creditedAccountNumber", targetAccount.getAccountNumber());
        res.put("isPremature", premature);
        return res;
    }

    public List<FixedDeposit> getMyFixedDeposits(String username) {
        Customer customer = getCustomerForUsername(username);
        return fixedDepositRepository.findByCustomerId(customer.getId());
    }

    public List<FixedDeposit> getAllFixedDeposits() {
        return fixedDepositRepository.findAll();
    }

    // -------------------- FINANCIAL HEALTH & CREDIT SCORE --------------------

    public FinancialHealthResponse getMyFinancialHealth(String username) {
        Customer customer = getCustomerForUsername(username);
        return financialHealthService.assessCustomer(customer);
    }

    public FinancialHealthResponse getCustomerFinancialHealth(Long customerId) {
        Customer customer = getCustomer(customerId);
        return financialHealthService.assessCustomer(customer);
    }

    // -------------------- FOREX / MULTI-CURRENCY --------------------

    public Map<String, Object> getForexRates() {
        return forexService.getSupportedRates();
    }

    public ForexConversionResponse convertForex(ForexConversionRequest request) {
        return forexService.convert(request);
    }

    // -------------------- LOANS --------------------

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public List<Loan> getLoans(Long customerId) {
        getCustomer(customerId);
        return loanRepository.findByCustomerId(customerId);
    }

    public Loan sanctionLoan(Long customerId, Loan input) {
        Customer customer = getCustomer(customerId);

        if (input.getPrincipal() == null
                || input.getPrincipal().compareTo(new BigDecimal("10000")) < 0) {
            throw new IllegalArgumentException("Loan principal must be at least 10000");
        }
        if (input.getType() == null || input.getType().isBlank()) {
            throw new IllegalArgumentException("Loan type is required");
        }
        if (input.getTenureMonths() <= 0 || input.getTenureMonths() > 480) {
            throw new IllegalArgumentException("Tenure must be between 1 and 480 months");
        }
        if (input.getInterestRate() <= 0 || input.getInterestRate() > 50) {
            throw new IllegalArgumentException("Interest rate must be between 0 and 50");
        }
        if (loanRepository.countByCustomerIdAndStatus(customerId, "ACTIVE") >= 3) {
            throw new IllegalArgumentException("Customer already has 3 active loans");
        }

        BigDecimal totalBalance = accountRepository.findByCustomerId(customerId)
                .stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal requiredBalance = input.getPrincipal().multiply(new BigDecimal("0.10"));
        if (totalBalance.compareTo(requiredBalance) < 0) {
            throw new IllegalArgumentException(
                    "Customer balance must be at least 10% of loan principal");
        }

        input.setCustomer(customer);
        input.setStatus("ACTIVE");
        input.calculateEmi();
        return loanRepository.save(input);
    }

    // -------------------- DASHBOARD & ANALYTICS --------------------

    public long customerCount() {
        return customerRepository.count();
    }

    public long activeAccountCount() {
        return accountRepository.countByStatus("ACTIVE");
    }

    public long activeLoanCount() {
        return loanRepository.countByStatus("ACTIVE");
    }

    public Map<String, Object> getRichDashboard() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("customers", customerRepository.count());
        stats.put("activeAccounts", accountRepository.countByStatus("ACTIVE"));
        stats.put("activeLoans", loanRepository.countByStatus("ACTIVE"));
        stats.put("activeFixedDeposits", fixedDepositRepository.countByStatus("ACTIVE"));
        stats.put("totalBankReserves", accountRepository.sumTotalActiveBalance());
        stats.put("totalLoansSanctioned", loanRepository.sumTotalActivePrincipal());
        stats.put("totalTermDeposits", fixedDepositRepository.sumTotalActiveDeposits());
        stats.put("totalTransactionsCount", transactionRepository.count());
        stats.put("flaggedSecurityAlerts", transactionRepository.countByStatus("FLAGGED"));
        return stats;
    }

    // -------------------- HELPER METHODS --------------------

    private Account getActiveAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalArgumentException("Account " + account.getAccountNumber() + " is not active");
        }
        return account;
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void recordTransaction(Account account, String type, BigDecimal amount,
                                   BigDecimal balanceAfter, String description,
                                   String sourceAccount, String destinationAccount,
                                   int riskScore, String status) {
        String ref = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        Transaction t = new Transaction(
                ref,
                type,
                amount,
                balanceAfter,
                description,
                sourceAccount,
                destinationAccount,
                riskScore,
                status,
                account
        );
        transactionRepository.save(t);
    }
}
