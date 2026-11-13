import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { InvoiceFileModel, InvoiceModel, InvoiceRequest, InvoiceStats, InvoiceStatus } from '../models';

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly apiUrl = '/api/invoices';

  constructor(private http: HttpClient) {}

  getAll(filters: {
    status?: InvoiceStatus;
    clientId?: number;
    projectId?: number;
  } = {}): Observable<InvoiceModel[]> {
    let params = new HttpParams();
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.clientId) {
      params = params.set('clientId', filters.clientId);
    }
    if (filters.projectId) {
      params = params.set('projectId', filters.projectId);
    }
    return this.http.get<InvoiceModel[]>(this.apiUrl, { params });
  }

  getStats(): Observable<InvoiceStats> {
    return this.http.get<InvoiceStats>(`${this.apiUrl}/stats`);
  }

  create(invoice: InvoiceRequest): Observable<InvoiceModel> {
    return this.http.post<InvoiceModel>(this.apiUrl, invoice);
  }

  update(id: number, invoice: InvoiceRequest): Observable<InvoiceModel> {
    return this.http.put<InvoiceModel>(`${this.apiUrl}/${id}`, invoice);
  }

  markPaid(id: number, paymentMethod?: string): Observable<InvoiceModel> {
    const params = paymentMethod ? new HttpParams().set('paymentMethod', paymentMethod) : undefined;
    return this.http.patch<InvoiceModel>(`${this.apiUrl}/${id}/paid`, null, { params });
  }

  markUnpaid(id: number): Observable<InvoiceModel> {
    return this.http.patch<InvoiceModel>(`${this.apiUrl}/${id}/unpaid`, null);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getFiles(invoiceId: number): Observable<InvoiceFileModel[]> {
    return this.http.get<InvoiceFileModel[]>(`${this.apiUrl}/${invoiceId}/files`);
  }

  uploadFile(invoiceId: number, file: File): Observable<InvoiceFileModel> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<InvoiceFileModel>(`${this.apiUrl}/${invoiceId}/files`, formData);
  }

  downloadFile(invoiceId: number, fileId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${invoiceId}/files/${fileId}`, { responseType: 'blob' });
  }

  deleteFile(invoiceId: number, fileId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${invoiceId}/files/${fileId}`);
  }
}
