import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ProjectModel } from '../models';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly apiUrl = '/api/projects';

  constructor(private http: HttpClient) {}

  getAll(): Observable<ProjectModel[]> {
    return this.http.get<ProjectModel[]>(this.apiUrl);
  }

  create(project: ProjectModel, clientId: number): Observable<ProjectModel> {
    return this.http.post<ProjectModel>(`${this.apiUrl}?clientId=${clientId}`, project);
  }

  update(id: number, project: ProjectModel, clientId: number): Observable<ProjectModel> {
    return this.http.put<ProjectModel>(`${this.apiUrl}/${id}?clientId=${clientId}`, project);
  }

  uploadImage(id: number, file: File): Observable<ProjectModel> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<ProjectModel>(`${this.apiUrl}/${id}/image`, formData);
  }

  deleteImage(id: number): Observable<ProjectModel> {
    return this.http.delete<ProjectModel>(`${this.apiUrl}/${id}/image`);
  }

  getImage(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/image`, { responseType: 'blob' });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
