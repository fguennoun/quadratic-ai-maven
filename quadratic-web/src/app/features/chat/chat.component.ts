import { Component, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DialogueService } from '../../core/services/dialogue.service';
interface Message {
  role: 'ai' | 'user';
  text: string;
}
@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [FormsModule, CommonModule],
  template: `
    <div class="page">
      <div class="chat-panel">
        <div class="chat-header">
          <h2>Chat IA — Dialogue conversationnel</h2>
          <button class="btn-reset" (click)="resetSession()">Nouvelle session</button>
        </div>
        <p class="hint">La FSM vous guide pas a pas. Tapez "go" pour commencer ou entrez une equation directement.</p>
        <div class="chat-window" #chatWindow>
          @for (msg of messages(); track $index) {
            <div class="bubble" [class.user]="msg.role === 'user'" [class.ai]="msg.role === 'ai'">
              <span class="avatar">{{ msg.role === 'ai' ? 'AI' : 'Vous' }}</span>
              <pre class="text">{{ msg.text }}</pre>
            </div>
          }
          @if (loading()) {
            <div class="bubble ai">
              <span class="avatar">AI</span>
              <span class="typing">...</span>
            </div>
          }
        </div>
        @if (!finished()) {
          <div class="input-bar">
            <input
              #inputEl
              type="text"
              [(ngModel)]="userInput"
              (keydown.enter)="send()"
              placeholder="Votre message..."
              [disabled]="loading()"
            />
            <button (click)="send()" [disabled]="loading() || !userInput.trim()">Envoyer</button>
          </div>
        } @else {
          <div class="finished-bar">
            Session terminee. <button (click)="resetSession()">Recommencer</button>
          </div>
        }
        <div class="state-badge">Etat FSM : {{ state() }}</div>
      </div>
    </div>
  `,
  styles: [`
    .page { padding: 2rem; display: flex; justify-content: center; }
    .chat-panel { background: white; border-radius: 12px; padding: 1.5rem; max-width: 760px; width: 100%; box-shadow: 0 4px 16px rgba(0,0,0,0.1); display: flex; flex-direction: column; height: calc(100vh - 120px); }
    .chat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.3rem; }
    h2 { color: #1a237e; font-size: 1.2rem; }
    .hint { color: #888; font-size: 0.85rem; margin-bottom: 1rem; }
    .btn-reset { background: #e53935; color: white; border: none; padding: 0.4rem 1rem; border-radius: 6px; cursor: pointer; font-size: 0.85rem; }
    .chat-window { flex: 1; overflow-y: auto; padding: 1rem; background: #f9f9f9; border-radius: 8px; margin-bottom: 1rem; display: flex; flex-direction: column; gap: 0.8rem; }
    .bubble { display: flex; gap: 0.6rem; max-width: 90%; }
    .bubble.ai { align-self: flex-start; }
    .bubble.user { align-self: flex-end; flex-direction: row-reverse; }
    .avatar { font-size: 0.7rem; font-weight: bold; padding: 0.3rem 0.5rem; border-radius: 50%; min-width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; }
    .ai .avatar { background: #1a237e; color: white; }
    .user .avatar { background: #e65100; color: white; }
    pre.text { background: white; border-radius: 8px; padding: 0.8rem 1rem; margin: 0; white-space: pre-wrap; font-family: inherit; font-size: 0.9rem; line-height: 1.5; box-shadow: 0 1px 4px rgba(0,0,0,0.08); max-width: 600px; }
    .user pre.text { background: #e3f2fd; }
    .typing { font-size: 1.5rem; letter-spacing: 2px; color: #1a237e; }
    .input-bar { display: flex; gap: 0.7rem; }
    .input-bar input { flex: 1; padding: 0.7rem; border: 1px solid #ddd; border-radius: 6px; font-size: 1rem; outline: none; }
    .input-bar input:focus { border-color: #1a237e; }
    .input-bar button { background: #1a237e; color: white; border: none; padding: 0.7rem 1.5rem; border-radius: 6px; cursor: pointer; }
    .input-bar button:disabled { background: #9e9e9e; cursor: not-allowed; }
    .finished-bar { text-align: center; color: #888; padding: 1rem; }
    .finished-bar button { background: #1a237e; color: white; border: none; padding: 0.4rem 1rem; border-radius: 6px; cursor: pointer; margin-left: 0.5rem; }
    .state-badge { font-size: 0.75rem; color: #aaa; text-align: right; margin-top: 0.3rem; }
  `]
})
export class ChatComponent implements AfterViewChecked {
  @ViewChild('chatWindow') chatWindow!: ElementRef;
  @ViewChild('inputEl') inputEl!: ElementRef;
  userInput = '';
  messages = signal<Message[]>([]);
  loading = signal(false);
  finished = signal(false);
  state = signal('GREETING');
  constructor(private svc: DialogueService) {
    // Initialisation : salutation automatique
    this.sendToApi('');
  }
  send() {
    const msg = this.userInput.trim();
    if (!msg || this.loading()) return;
    this.messages.update(m => [...m, { role: 'user', text: msg }]);
    this.userInput = '';
    this.sendToApi(msg);
  }
  private sendToApi(message: string) {
    this.loading.set(true);
    this.svc.send(message).subscribe({
      next: r => {
        this.messages.update(m => [...m, { role: 'ai', text: r.response }]);
        this.state.set(r.state);
        this.finished.set(r.finished);
        this.loading.set(false);
      },
      error: e => {
        this.messages.update(m => [...m, { role: 'ai', text: 'Erreur de connexion. Verifiez que le serveur est lance sur localhost:8080.' }]);
        this.loading.set(false);
      }
    });
  }
  resetSession() {
    this.finished.set(false);
    this.state.set('GREETING');
    this.messages.set([]);
    this.svc.reset().subscribe({ complete: () => this.sendToApi('') });
  }
  ngAfterViewChecked() {
    if (this.chatWindow) {
      const el = this.chatWindow.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }
}
