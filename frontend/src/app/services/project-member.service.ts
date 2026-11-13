import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ProjectMemberModel, ProjectRole } from '../models';

@Injectable({ providedIn: 'root' })
export class ProjectMemberService {
  private readonly apiUrl = '/api/projects';

  constructor(private http: HttpClient) {}

  getAll(projectId: number): Observable<ProjectMemberModel[]> {
    return this.http.get<ProjectMemberModel[]>(`${this.apiUrl}/${projectId}/members`);
  }

  add(projectId: number, userId: number, projectRole: ProjectRole): Observable<ProjectMemberModel> {
    return this.http.post<ProjectMemberModel>(`${this.apiUrl}/${projectId}/members`, { userId, projectRole });
  }

  updateRole(projectId: number, memberId: number, projectRole: ProjectRole): Observable<ProjectMemberModel> {
    return this.http.put<ProjectMemberModel>(`${this.apiUrl}/${projectId}/members/${memberId}`, {
      userId: 0,
      projectRole,
    });
  }

  remove(projectId: number, memberId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${projectId}/members/${memberId}`);
  }
}
