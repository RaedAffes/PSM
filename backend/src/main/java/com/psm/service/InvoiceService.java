package com.psm.service;

import com.psm.dto.InvoiceRequest;
import com.psm.dto.InvoiceResponse;
import com.psm.dto.InvoiceStats;
import com.psm.entity.Client;
import com.psm.entity.Invoice;
import com.psm.entity.InvoiceStatus;
import com.psm.entity.Project;
import com.psm.repository.ClientRepository;
import com.psm.repository.InvoiceRepository;
import com.psm.repository.ProjectRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final InvoiceFileService invoiceFileService;

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll(InvoiceStatus status, Long clientId, Long projectId) {
        return invoiceRepository.findAllByOrderByIssueDateDescIdDesc().stream()
            .filter(invoice -> status == null || invoice.getStatus() == status)
            .filter(invoice -> clientId == null
                || (invoice.getClient() != null && clientId.equals(invoice.getClient().getId())))
            .filter(invoice -> projectId == null
                || (invoice.getProject() != null && projectId.equals(invoice.getProject().getId())))
            .map(InvoiceResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Long id) {
        return InvoiceResponse.from(findInvoice(id));
    }

    @Transactional(readOnly = true)
    public InvoiceStats stats() {
        List<Invoice> invoices = invoiceRepository.findAll();
        BigDecimal paid = sumTotal(invoices, InvoiceStatus.PAID);
        BigDecimal unpaid = sumTotal(invoices, InvoiceStatus.UNPAID);
        long paidCount = invoices.stream().filter(invoice -> invoice.getStatus() == InvoiceStatus.PAID).count();
        long unpaidCount = invoices.stream().filter(invoice -> invoice.getStatus() == InvoiceStatus.UNPAID).count();
        long overdueCount = invoices.stream()
            .filter(invoice -> invoice.getStatus() == InvoiceStatus.UNPAID)
            .filter(invoice -> invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now()))
            .count();

        return new InvoiceStats(
            paid.add(unpaid),
            paid,
            unpaid,
            paidCount,
            unpaidCount,
            overdueCount
        );
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        Invoice invoice = new Invoice();
        apply(invoice, request, true);
        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse update(Long id, InvoiceRequest request) {
        Invoice invoice = findInvoice(id);
        apply(invoice, request, false);
        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse markPaid(Long id, String paymentMethod) {
        Invoice invoice = findInvoice(id);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidDate(LocalDate.now());
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            invoice.setPaymentMethod(paymentMethod.trim());
        }
        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse markUnpaid(Long id) {
        Invoice invoice = findInvoice(id);
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setPaidDate(null);
        invoice.setPaymentMethod(null);
        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional
    public void delete(Long id) {
        Invoice invoice = findInvoice(id);
        invoiceFileService.deleteAll(invoice.getId());
        invoiceRepository.delete(invoice);
    }

    private void apply(Invoice invoice, InvoiceRequest request, boolean creating) {
        Client client = clientRepository.findById(request.getClientId())
            .orElseThrow(() -> new RuntimeException("Client not found"));

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
            if (!client.getId().equals(project.getClient().getId())) {
                throw new RuntimeException("Project does not belong to client");
            }
        }

        String numero = request.getNumero() == null ? "" : request.getNumero().trim();
        if (numero.isBlank()) {
            numero = creating ? nextNumber() : invoice.getNumero();
        } else if (invoiceRepository.existsByNumeroIgnoreCase(numero)
            && (creating || !numero.equalsIgnoreCase(invoice.getNumero()))) {
            throw new RuntimeException("Invoice number already exists");
        }

        BigDecimal amount = request.getAmount() == null ? BigDecimal.ZERO : request.getAmount();
        BigDecimal taxRate = request.getTaxRate() == null ? BigDecimal.ZERO : request.getTaxRate();
        BigDecimal total = amount
            .multiply(BigDecimal.ONE.add(taxRate.divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP)))
            .setScale(2, RoundingMode.HALF_UP);

        invoice.setNumero(numero);
        invoice.setClient(client);
        invoice.setProject(project);
        invoice.setIssueDate(request.getIssueDate() == null ? LocalDate.now() : request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setAmount(amount);
        invoice.setTaxRate(taxRate);
        invoice.setTotalAmount(total);
        invoice.setPaymentMethod(blankToNull(request.getPaymentMethod()));
        invoice.setNotes(blankToNull(request.getNotes()));
        if (creating) {
            invoice.setStatus(InvoiceStatus.UNPAID);
        }
    }

    private String nextNumber() {
        int year = LocalDate.now().getYear();
        long sequence = invoiceRepository.count() + 1;
        String candidate;
        do {
            candidate = "FAC-" + year + "-" + String.format("%04d", sequence++);
        } while (invoiceRepository.existsByNumeroIgnoreCase(candidate));
        return candidate;
    }

    private BigDecimal sumTotal(List<Invoice> invoices, InvoiceStatus status) {
        return invoices.stream()
            .filter(invoice -> invoice.getStatus() == status)
            .map(invoice -> invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Invoice findInvoice(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new RuntimeException("Invoice not found"));
    }
}
