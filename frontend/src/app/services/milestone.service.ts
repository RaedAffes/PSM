import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MilestoneModel, MilestoneRequest, MilestoneStatus } from '../models';

@Injectable({ providedIn: 'root' })
export class MilestoneService {
  private readonly apiUrl = '/api/milestones';

  constructor(private http: HttpClient) {}

  getAll(projectId?: number): Observable<MilestoneModel[]> {
    const params = projectId ? new HttpParams().set('projectId', projectId) : undefined;
    return this.http.get<MilestoneModel[]>(this.apiUrl, { params });
  }

  create(milestone: MilestoneRequest): Observable<MilestoneModel> {
    return this.http.post<MilestoneModel>(this.apiUrl, milestone);
  }

  update(id: number, milestone: MilestoneRequest): Observable<MilestoneModel> {
    return this.http.put<MilestoneModel>(`${this.apiUrl}/${id}`, milestone);
  }

  updateStatus(id: number, status: MilestoneStatus): Observable<MilestoneModel> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<MilestoneModel>(`${this.apiUrl}/${id}/status`, null, { params });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
