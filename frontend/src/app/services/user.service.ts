import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UserModel } from '../models';

export interface UserWriteRequest {
  email: string;
  password?: string;
  tel: string;
  nom: string;
  prenom: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly usersUrl = '/api/users';

  constructor(private http: HttpClient) {}

  getAll(): Observable<UserModel[]> {
    return this.http.get<UserModel[]>(this.usersUrl);
  }

  getDirectory(): Observable<UserModel[]> {
    return this.http.get<UserModel[]>(`${this.usersUrl}/directory`);
  }

  create(user: UserWriteRequest): Observable<UserModel> {
    return this.http.post<UserModel>(this.usersUrl, user);
  }

  update(id: number, user: UserWriteRequest): Observable<UserModel> {
    return this.http.put<UserModel>(`${this.usersUrl}/${id}`, user);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.usersUrl}/${id}`);
  }
}
