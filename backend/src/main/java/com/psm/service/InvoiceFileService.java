package com.psm.service;

import com.psm.dto.InvoiceFileResponse;
import com.psm.entity.Invoice;
import com.psm.entity.InvoiceFile;
import com.psm.repository.InvoiceFileRepository;
import com.psm.repository.InvoiceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class InvoiceFileService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceFileRepository invoiceFileRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<InvoiceFileResponse> findAll(Long invoiceId) {
        requireInvoice(invoiceId);
        return invoiceFileRepository.findByInvoiceIdOrderByCreatedAtAscIdAsc(invoiceId).stream()
            .map(InvoiceFileResponse::from)
            .toList();
    }

    @Transactional
    public InvoiceFileResponse upload(Long invoiceId, MultipartFile file) {
        Invoice invoice = requireInvoice(invoiceId);
        FileStorageService.StoredFile stored = fileStorageService.store(file);

        InvoiceFile invoiceFile = new InvoiceFile();
        invoiceFile.setInvoice(invoice);
        invoiceFile.setStorageKey(stored.storageKey());
        invoiceFile.setFileName(stored.originalName());
        invoiceFile.setContentType(stored.contentType());
        invoiceFile.setSize(stored.size());

        try {
            return InvoiceFileResponse.from(invoiceFileRepository.save(invoiceFile));
        } catch (RuntimeException exception) {
            fileStorageService.delete(stored.storageKey());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Download download(Long invoiceId, Long fileId) {
        InvoiceFile file = findFile(invoiceId, fileId);
        return new Download(
            file.getFileName(),
            file.getContentType(),
            fileStorageService.load(file.getStorageKey()),
            file.getSize()
        );
    }

    @Transactional
    public void delete(Long invoiceId, Long fileId) {
        InvoiceFile file = findFile(invoiceId, fileId);
        String storageKey = file.getStorageKey();
        invoiceFileRepository.delete(file);
        invoiceFileRepository.flush();
        fileStorageService.delete(storageKey);
    }

    @Transactional
    public void deleteAll(Long invoiceId) {
        List<InvoiceFile> files = invoiceFileRepository.findByInvoiceId(invoiceId);
        if (files.isEmpty()) {
            return;
        }
        invoiceFileRepository.deleteAll(files);
        invoiceFileRepository.flush();
        files.forEach(file -> fileStorageService.delete(file.getStorageKey()));
    }

    private InvoiceFile findFile(Long invoiceId, Long fileId) {
        return invoiceFileRepository.findByIdAndInvoiceId(fileId, invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice file not found"));
    }

    private Invoice requireInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    public record Download(String fileName, String contentType, Resource resource, long size) {
    }
}
