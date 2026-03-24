import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DialogueRequest, DialogueResponse } from '../models/api.models';
import { v4 as uuidv4 } from 'uuid';
const API = 'http://localhost:8080/api';
/**
 * Service HTTP pour le dialogue conversationnel (FSM Phase 1).
 * Gere automatiquement le sessionId (UUID) par instance.
 */
@Injectable({ providedIn: 'root' })
export class DialogueService {
  private sessionId: string = uuidv4();
  constructor(private http: HttpClient) {}
  getSessionId(): string { return this.sessionId; }
  /** POST /api/dialogue — Envoyer un message */
  send(message: string): Observable<DialogueResponse> {
    const req: DialogueRequest = { sessionId: this.sessionId, message };
    return this.http.post<DialogueResponse>(`${API}/dialogue`, req);
  }
  /** Demarre une nouvelle session (nouveau UUID) */
  reset(): Observable<void> {
    return new Observable(obs => {
      this.http.delete<void>(`${API}/dialogue/${this.sessionId}`).subscribe({
        complete: () => {
          this.sessionId = uuidv4();
          obs.next();
          obs.complete();
        },
        error: () => {
          this.sessionId = uuidv4();
          obs.next();
          obs.complete();
        }
      });
    });
  }
}
