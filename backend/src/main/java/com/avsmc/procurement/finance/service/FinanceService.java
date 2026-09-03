package com.avsmc.procurement.finance.service;

import com.avsmc.procurement.finance.dto.*;
import com.avsmc.procurement.finance.entity.*;
import com.avsmc.procurement.finance.repository.*;
import com.avsmc.procurement.security.SecurityUtils;
import com.avsmc.procurement.shared.exception.BusinessRuleException;
import com.avsmc.procurement.shared.exception.ResourceNotFoundException;
import com.avsmc.procurement.shared.util.DocumentNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final JournalRepository journalRepository;
    private final ChartOfAccountRepository chartRepository;
    private final CashAccountRepository cashAccountRepository;
    private final CashbookEntryRepository cashbookRepository;
    private final SecurityUtils securityUtils;
    private final DocumentNumberGenerator docNumberGenerator;

    @Transactional(readOnly = true)
    public List<ChartOfAccountDto> listAccounts() {
        return chartRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toAccountDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CashAccountDto> listCashAccounts() {
        return cashAccountRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toCashAccountDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JournalDto> listJournals() {
        return journalRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toJournalDto).collect(Collectors.toList());
    }

    @Transactional
    public JournalDto createManualJournal(CreateJournalRequest req) {
        BigDecimal totalDebits = req.getLines().stream().map(JournalLineRequest::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = req.getLines().stream().map(JournalLineRequest::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebits.compareTo(totalCredits) != 0)
            throw new BusinessRuleException("Journal is not balanced. Debits (" + totalDebits + ") != Credits (" + totalCredits + ")");
        if (totalDebits.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessRuleException("Journal must have a positive amount");

        Journal j = new Journal();
        j.setOrganisationId(securityUtils.currentOrgId());
        j.setJournalNumber(docNumberGenerator.nextNumber(securityUtils.currentOrgId(), "JOURNAL"));
        j.setJournalType(req.getJournalType());
        j.setDescription(req.getDescription());
        j.setTotalDebit(totalDebits);
        j.setTotalCredit(totalCredits);
        j.setCreatedBy(securityUtils.currentUserId());

        List<JournalLine> lines = new ArrayList<>();
        int lineNum = 1;
        for (var lr : req.getLines()) {
            JournalLine jl = new JournalLine();
            jl.setJournalId(j.getId());
            jl.setLineNumber(lineNum++);
            jl.setAccountId(lr.getAccountId());
            jl.setDescription(lr.getDescription());
            jl.setDebitAmount(lr.getDebitAmount());
            jl.setCreditAmount(lr.getCreditAmount());
            lines.add(jl);
        }
        j.setLines(lines);
        return toJournalDto(journalRepository.save(j));
    }

    @Transactional
    public JournalDto postJournal(UUID id) {
        Journal j = journalRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Journal", id));
        if (!"DRAFT".equals(j.getStatus())) throw new BusinessRuleException("Only draft journals can be posted");
        for (JournalLine line : j.getLines()) {
            ChartOfAccount acc = chartRepository.findById(line.getAccountId()).orElseThrow(() -> new ResourceNotFoundException("Account", line.getAccountId()));
            BigDecimal net = line.getDebitAmount().subtract(line.getCreditAmount());
            if ("DEBIT".equals(acc.getNormalBalance())) acc.setCurrentBalance(acc.getCurrentBalance().add(net));
            else acc.setCurrentBalance(acc.getCurrentBalance().subtract(net));
            chartRepository.save(acc);
        }
        j.setStatus("POSTED");
        j.setPostedBy(securityUtils.currentUserId());
        j.setPostedAt(Instant.now());
        return toJournalDto(journalRepository.save(j));
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> listInvoices() {
        return invoiceRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toInvoiceDto).collect(Collectors.toList());
    }

    @Transactional
    public InvoiceDto createInvoice(CreateInvoiceRequest req) {
        Invoice inv = new Invoice();
        inv.setOrganisationId(securityUtils.currentOrgId());
        inv.setAvOrderId(req.getAvOrderId());
        inv.setRepairJobId(req.getRepairJobId());
        inv.setInvoiceNumber(docNumberGenerator.nextNumber(securityUtils.currentOrgId(), "INVOICE"));
        inv.setFromEntity(req.getFromEntity());
        inv.setToEntity(req.getToEntity());
        inv.setTaxRate(req.getTaxRate() != null ? req.getTaxRate() : BigDecimal.ZERO);
        inv.setDueDate(req.getDueDate());
        inv.setNotes(req.getNotes());

        List<InvoiceItem> items = new ArrayList<>();
        int lineNum = 1;
        for (var ir : req.getItems()) {
            InvoiceItem ii = new InvoiceItem();
            ii.setInvoiceId(inv.getId());
            ii.setOrderItemId(ir.getOrderItemId());
            ii.setLineNumber(lineNum++);
            ii.setDescription(ir.getDescription());
            ii.setQuantity(ir.getQuantity());
            ii.setUnitPrice(ir.getUnitPrice());
            ii.setLineTotal(ir.getQuantity().multiply(ir.getUnitPrice()));
            items.add(ii);
        }
        inv.setItems(items);
        BigDecimal sub = items.stream().map(InvoiceItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = sub.multiply(inv.getTaxRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        inv.setSubtotal(sub);
        inv.setTaxAmount(tax);
        inv.setTotalAmount(sub.add(tax));
        return toInvoiceDto(invoiceRepository.save(inv));
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> listPayments() {
        return paymentRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toPaymentDto).collect(Collectors.toList());
    }

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest req) {
        Payment p = new Payment();
        p.setOrganisationId(securityUtils.currentOrgId());
        p.setInvoiceId(req.getInvoiceId());
        p.setPaymentNumber(docNumberGenerator.nextNumber(securityUtils.currentOrgId(), "PAYMENT"));
        p.setPaymentType(req.getPaymentType());
        p.setAmount(req.getAmount());
        p.setPaymentMethod(req.getPaymentMethod());
        p.setReference(req.getReference());
        p.setCashAccountId(req.getCashAccountId());
        p.setPaymentDate(req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now());
        p.setNotes(req.getNotes());
        p.setReceivedBy(securityUtils.currentUserId());
        p = paymentRepository.save(p);

        if (req.getInvoiceId() != null) {
            Invoice inv = invoiceRepository.findById(req.getInvoiceId()).orElseThrow(() -> new ResourceNotFoundException("Invoice", req.getInvoiceId()));
            inv.setAmountPaid(inv.getAmountPaid().add(req.getAmount()));
            inv.setStatus(inv.getAmountPaid().compareTo(inv.getTotalAmount()) >= 0 ? "PAID" : "PARTIAL_PAYMENT");
            invoiceRepository.save(inv);
        }
        if (req.getCashAccountId() != null) {
            CashAccount ca = cashAccountRepository.findById(req.getCashAccountId()).orElseThrow(() -> new ResourceNotFoundException("CashAccount", req.getCashAccountId()));
            if ("RECEIPT".equals(req.getPaymentType())) ca.setBalance(ca.getBalance().add(req.getAmount()));
            else ca.setBalance(ca.getBalance().subtract(req.getAmount()));
            cashAccountRepository.save(ca);
        }
        p.setStatus("COMPLETED");
        return toPaymentDto(paymentRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<CashbookEntryDto> listCashbookEntries() {
        return cashbookRepository.findByOrganisationId(securityUtils.currentOrgId())
                .stream().map(this::toCashbookDto).collect(Collectors.toList());
    }

    @Transactional
    public CashbookEntryDto createCashbookEntry(CreateCashbookEntryRequest req) {
        CashAccount ca = cashAccountRepository.findById(req.getCashAccountId()).orElseThrow(() -> new ResourceNotFoundException("CashAccount", req.getCashAccountId()));
        CashbookEntry e = new CashbookEntry();
        e.setOrganisationId(securityUtils.currentOrgId());
        e.setCashAccountId(req.getCashAccountId());
        e.setEntryType(req.getEntryType());
        e.setAmount(req.getAmount());
        e.setCounterparty(req.getCounterparty());
        e.setNarration(req.getNarration());
        e.setReference(req.getReference());
        e.setEntryDate(req.getEntryDate() != null ? req.getEntryDate() : LocalDate.now());
        e.setCreatedBy(securityUtils.currentUserId());
        if ("RECEIPT".equals(req.getEntryType())) ca.setBalance(ca.getBalance().add(req.getAmount()));
        else ca.setBalance(ca.getBalance().subtract(req.getAmount()));
        cashAccountRepository.save(ca);
        e.setRunningBalance(ca.getBalance());
        return toCashbookDto(cashbookRepository.save(e));
    }

    private ChartOfAccountDto toAccountDto(ChartOfAccount a) {
        return ChartOfAccountDto.builder().id(a.getId()).accountCode(a.getAccountCode())
                .accountName(a.getAccountName()).accountType(a.getAccountType())
                .subType(a.getSubType()).isPostable(a.getIsPostable())
                .normalBalance(a.getNormalBalance()).openingBalance(a.getOpeningBalance())
                .currentBalance(a.getCurrentBalance()).build();
    }

    private CashAccountDto toCashAccountDto(CashAccount c) {
        return CashAccountDto.builder().id(c.getId()).accountName(c.getAccountName())
                .accountType(c.getAccountType()).accountNumber(c.getAccountNumber())
                .bankName(c.getBankName()).currency(c.getCurrency()).balance(c.getBalance()).build();
    }

    private JournalDto toJournalDto(Journal j) {
        return JournalDto.builder().id(j.getId()).journalNumber(j.getJournalNumber())
                .journalType(j.getJournalType()).description(j.getDescription())
                .totalDebit(j.getTotalDebit()).totalCredit(j.getTotalCredit())
                .status(j.getStatus()).postedBy(j.getPostedBy()).postedAt(j.getPostedAt())
                .lines(j.getLines().stream().map(l -> JournalLineDto.builder().id(l.getId())
                        .lineNumber(l.getLineNumber()).accountId(l.getAccountId())
                        .description(l.getDescription()).debitAmount(l.getDebitAmount())
                        .creditAmount(l.getCreditAmount()).build()).collect(Collectors.toList())).build();
    }

    private InvoiceDto toInvoiceDto(Invoice i) {
        return InvoiceDto.builder().id(i.getId()).avOrderId(i.getAvOrderId())
                .repairJobId(i.getRepairJobId()).invoiceNumber(i.getInvoiceNumber())
                .invoiceType(i.getInvoiceType()).status(i.getStatus())
                .invoiceDate(i.getInvoiceDate()).dueDate(i.getDueDate())
                .fromEntity(i.getFromEntity()).toEntity(i.getToEntity())
                .subtotal(i.getSubtotal()).taxRate(i.getTaxRate()).taxAmount(i.getTaxAmount())
                .totalAmount(i.getTotalAmount()).amountPaid(i.getAmountPaid())
                .balanceDue(i.getTotalAmount().subtract(i.getAmountPaid()))
                .currency(i.getCurrency()).notes(i.getNotes())
                .items(i.getItems().stream().map(ii -> InvoiceItemDto.builder().id(ii.getId())
                        .lineNumber(ii.getLineNumber()).description(ii.getDescription())
                        .quantity(ii.getQuantity()).unitPrice(ii.getUnitPrice())
                        .lineTotal(ii.getLineTotal()).build()).collect(Collectors.toList())).build();
    }

    private PaymentDto toPaymentDto(Payment p) {
        return PaymentDto.builder().id(p.getId()).invoiceId(p.getInvoiceId())
                .paymentNumber(p.getPaymentNumber()).paymentType(p.getPaymentType())
                .amount(p.getAmount()).currency(p.getCurrency()).paymentMethod(p.getPaymentMethod())
                .reference(p.getReference()).cashAccountId(p.getCashAccountId())
                .status(p.getStatus()).paymentDate(p.getPaymentDate()).notes(p.getNotes()).build();
    }

    private CashbookEntryDto toCashbookDto(CashbookEntry e) {
        return CashbookEntryDto.builder().id(e.getId()).cashAccountId(e.getCashAccountId())
                .journalId(e.getJournalId()).entryType(e.getEntryType()).amount(e.getAmount())
                .counterparty(e.getCounterparty()).narration(e.getNarration())
                .reference(e.getReference()).runningBalance(e.getRunningBalance())
                .reconciliationStatus(e.getReconciliationStatus()).entryDate(e.getEntryDate()).build();
    }
}
