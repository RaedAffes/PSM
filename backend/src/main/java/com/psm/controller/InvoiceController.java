package com.psm.controller;

import com.psm.dto.InvoiceFileResponse;
import com.psm.dto.InvoiceRequest;
import com.psm.dto.InvoiceResponse;
import com.psm.dto.InvoiceStats;
import com.psm.entity.InvoiceStatus;
import com.psm.service.InvoiceFileService;
import com.psm.service.InvoiceService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceFileService invoiceFileService;

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> findAll(
        @RequestParam(required = false) InvoiceStatus status,
        @RequestParam(required = false) Long clientId,
        @RequestParam(required = false) Long projectId
    ) {
        return ResponseEntity.ok(invoiceService.findAll(status, clientId, projectId));
    }

    @GetMapping("/stats")
    public ResponseEntity<InvoiceStats> stats() {
        return ResponseEntity.ok(invoiceService.stats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.findById(id));
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> update(@PathVariable Long id, @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.update(id, request));
    }

    @PatchMapping("/{id}/paid")
    public ResponseEntity<InvoiceResponse> markPaid(
        @PathVariable Long id,
        @RequestParam(required = false) String paymentMethod
    ) {
        return ResponseEntity.ok(invoiceService.markPaid(id, paymentMethod));
    }

    @PatchMapping("/{id}/unpaid")
    public ResponseEntity<InvoiceResponse> markUnpaid(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.markUnpaid(id));
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<List<InvoiceFileResponse>> files(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceFileService.findAll(id));
    }

    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceFileResponse> uploadFile(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceFileService.upload(id, file));
    }

    @GetMapping("/{invoiceId}/files/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long invoiceId, @PathVariable Long fileId) {
        InvoiceFileService.Download download = invoiceFileService.download(invoiceId, fileId);
        ContentDisposition disposition = ContentDisposition
            .builder("attachment")
            .filename(download.fileName() == null ? "facture" : download.fileName(), StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(download.contentType()))
            .contentLength(download.size())
            .cacheControl(CacheControl.noCache().cachePrivate())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(download.resource());
    }

    @DeleteMapping("/{invoiceId}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long invoiceId, @PathVariable Long fileId) {
        invoiceFileService.delete(invoiceId, fileId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
