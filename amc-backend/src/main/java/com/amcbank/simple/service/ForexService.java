package com.amcbank.simple.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.amcbank.simple.dto.ForexConversionRequest;
import com.amcbank.simple.dto.ForexConversionResponse;

@Service
public class ForexService {

    // Exchange rates expressed as: 1 Unit of Currency = X INR
    private static final Map<String, BigDecimal> RATES_TO_INR;

    static {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        map.put("INR", BigDecimal.ONE);
        map.put("USD", new BigDecimal("83.50"));
        map.put("EUR", new BigDecimal("91.20"));
        map.put("GBP", new BigDecimal("106.80"));
        map.put("AED", new BigDecimal("22.75"));
        map.put("JPY", new BigDecimal("0.55"));
        map.put("CAD", new BigDecimal("61.30"));
        map.put("SGD", new BigDecimal("62.40"));
        map.put("AUD", new BigDecimal("55.10"));
        RATES_TO_INR = Collections.unmodifiableMap(map);
    }

    public Map<String, Object> getSupportedRates() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("baseCurrency", "INR");
        response.put("ratesInInr", RATES_TO_INR);
        response.put("lastUpdated", LocalDateTime.now());
        response.put("bankSpreadFeePercent", new BigDecimal("0.50"));
        return response;
    }

    public ForexConversionResponse convert(ForexConversionRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Conversion amount must be greater than zero");
        }

        String from = request.getFromCurrency() == null ? "" : request.getFromCurrency().trim().toUpperCase();
        String to = request.getToCurrency() == null ? "" : request.getToCurrency().trim().toUpperCase();

        if (!RATES_TO_INR.containsKey(from)) {
            throw new IllegalArgumentException("Unsupported source currency: " + from + ". Supported: " + RATES_TO_INR.keySet());
        }
        if (!RATES_TO_INR.containsKey(to)) {
            throw new IllegalArgumentException("Unsupported destination currency: " + to + ". Supported: " + RATES_TO_INR.keySet());
        }

        BigDecimal fromRateInInr = RATES_TO_INR.get(from);
        BigDecimal toRateInInr = RATES_TO_INR.get(to);

        // Effective rate = (from / to)
        BigDecimal effectiveRate = fromRateInInr.divide(toRateInInr, 6, RoundingMode.HALF_UP);
        BigDecimal grossConverted = request.getAmount().multiply(effectiveRate).setScale(2, RoundingMode.HALF_UP);

        // 0.5% standard bank FX margin fee
        BigDecimal feePercent = new BigDecimal("0.50");
        BigDecimal feeAmount = grossConverted.multiply(feePercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netPayout = grossConverted.subtract(feeAmount);

        ForexConversionResponse response = new ForexConversionResponse();
        response.setFromCurrency(from);
        response.setToCurrency(to);
        response.setOriginalAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        response.setExchangeRate(effectiveRate);
        response.setConvertedAmount(grossConverted);
        response.setFeePercent(feePercent);
        response.setFeeAmount(feeAmount);
        response.setNetPayoutAmount(netPayout);
        response.setTimestamp(LocalDateTime.now());

        return response;
    }
}
