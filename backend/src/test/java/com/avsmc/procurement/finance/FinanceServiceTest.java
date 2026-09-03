package com.avsmc.procurement.finance;

import com.avsmc.procurement.finance.dto.*;
import com.avsmc.procurement.finance.service.FinanceService;
import com.avsmc.procurement.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FinanceServiceTest {

    @Autowired
    private FinanceService financeService;

    @Test
    void createManualJournal_balancedEntry_succeeds() {
        // This test requires accounts to exist — will work with seed data
        var accounts = financeService.listAccounts();
        if (accounts.size() < 2) return;

        CreateJournalRequest req = new CreateJournalRequest();
        req.setJournalType("MANUAL");
        req.setDescription("Test journal entry");

        JournalLineRequest debit = new JournalLineRequest();
        debit.setAccountId(accounts.get(0).getId());
        debit.setDebitAmount(new BigDecimal("100.00"));
        debit.setCreditAmount(BigDecimal.ZERO);

        JournalLineRequest credit = new JournalLineRequest();
        credit.setAccountId(accounts.get(1).getId());
        credit.setDebitAmount(BigDecimal.ZERO);
        credit.setCreditAmount(new BigDecimal("100.00"));

        req.setLines(List.of(debit, credit));

        JournalDto result = financeService.createManualJournal(req);
        assertNotNull(result.getId());
        assertEquals("DRAFT", result.getStatus());
        assertEquals(0, result.getTotalDebit().compareTo(result.getTotalCredit()));
    }

    @Test
    void createManualJournal_unbalancedEntry_throws() {
        var accounts = financeService.listAccounts();
        if (accounts.size() < 2) return;

        CreateJournalRequest req = new CreateJournalRequest();
        req.setJournalType("MANUAL");
        req.setDescription("Unbalanced journal");

        JournalLineRequest debit = new JournalLineRequest();
        debit.setAccountId(accounts.get(0).getId());
        debit.setDebitAmount(new BigDecimal("100.00"));
        debit.setCreditAmount(BigDecimal.ZERO);

        JournalLineRequest credit = new JournalLineRequest();
        credit.setAccountId(accounts.get(1).getId());
        credit.setDebitAmount(BigDecimal.ZERO);
        credit.setCreditAmount(new BigDecimal("50.00"));

        req.setLines(List.of(debit, credit));

        assertThrows(BusinessRuleException.class, () -> financeService.createManualJournal(req));
    }
}
