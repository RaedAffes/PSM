import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  TaskCommentModel,
  TaskModel,
  TaskRequest,
  TaskStats,
  TaskStatus,
  TimeEntryModel,
  TimeEntryRequest,
} from '../models';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly apiUrl = '/api/tasks';

  constructor(private http: HttpClient) {}

  getAll(filters: { projectId?: number; status?: TaskStatus; assignedUserId?: number } = {}): Observable<TaskModel[]> {
    let params = new HttpParams();
    if (filters.projectId) {
      params = params.set('projectId', filters.projectId);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.assignedUserId) {
      params = params.set('assignedUserId', filters.assignedUserId);
    }
    return this.http.get<TaskModel[]>(this.apiUrl, { params });
  }

  getStats(): Observable<TaskStats> {
    return this.http.get<TaskStats>(`${this.apiUrl}/stats`);
  }

  create(task: TaskRequest): Observable<TaskModel> {
    return this.http.post<TaskModel>(this.apiUrl, task);
  }

  update(id: number, task: TaskRequest): Observable<TaskModel> {
    return this.http.put<TaskModel>(`${this.apiUrl}/${id}`, task);
  }

  updateStatus(id: number, status: TaskStatus): Observable<TaskModel> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<TaskModel>(`${this.apiUrl}/${id}/status`, null, { params });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getComments(taskId: number): Observable<TaskCommentModel[]> {
    return this.http.get<TaskCommentModel[]>(`${this.apiUrl}/${taskId}/comments`);
  }

  addComment(taskId: number, content: string): Observable<TaskCommentModel> {
    return this.http.post<TaskCommentModel>(`${this.apiUrl}/${taskId}/comments`, { content });
  }

  deleteComment(taskId: number, commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${taskId}/comments/${commentId}`);
  }

  getTimeEntries(taskId: number): Observable<TimeEntryModel[]> {
    return this.http.get<TimeEntryModel[]>(`${this.apiUrl}/${taskId}/time-entries`);
  }

  addTimeEntry(taskId: number, entry: TimeEntryRequest): Observable<TimeEntryModel> {
    return this.http.post<TimeEntryModel>(`${this.apiUrl}/${taskId}/time-entries`, entry);
  }

  deleteTimeEntry(taskId: number, entryId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${taskId}/time-entries/${entryId}`);
  }
}
