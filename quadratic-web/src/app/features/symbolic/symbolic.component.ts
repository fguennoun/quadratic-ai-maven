import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { EquationService } from '../../core/services/equation.service';
import { SolveResponse } from '../../core/models/api.models';
@Component({
  selector: 'app-symbolic',
  standalone: true,
  imports: [FormsModule, CommonModule],
  template: `
    <div class="page">
      <div class="panel">
        <h2>IA Symbolique — ax&#178; + bx + c = 0</h2>
        <p class="hint">Resolution exacte via discriminant. Entrez les coefficients.</p>
        <form (ngSubmit)="solve()" class="form">
          <div class="fields">
            <label>a (terme x&#178;)
              <input type="number" [(ngModel)]="a" name="a" step="any" required placeholder="ex: 1" />
            </label>
            <label>b (terme x)
              <input type="number" [(ngModel)]="b" name="b" step="any" required placeholder="ex: -5" />
            </label>
            <label>c (constante)
              <input type="number" [(ngModel)]="c" name="c" step="any" required placeholder="ex: 6" />
            </label>
          </div>
          <button type="submit" [disabled]="loading()">
            {{ loading() ? 'Calcul...' : 'Resoudre' }}
          </button>
        </form>
        @if (error()) {
          <div class="error">{{ error() }}</div>
        }
        @if (result()) {
          <div class="result" [class]="'result-' + result()!.type.toLowerCase()">
            <h3>{{ result()!.equation }} = 0</h3>
            <div class="badge" [class]="'badge-' + result()!.type.toLowerCase()">
              {{ typeLabel(result()!.type) }}
            </div>
            <p><strong>Discriminant &Delta; = {{ result()!.delta | number:'1.2-4' }}</strong></p>
            @if (result()!.type === 'TWO_REAL') {
              <p>x&#8321; = {{ result()!.x1 | number:'1.4-4' }}</p>
              <p>x&#8322; = {{ result()!.x2 | number:'1.4-4' }}</p>
            }
            @if (result()!.type === 'ONE_DOUBLE') {
              <p>x&#8320; = {{ result()!.x1 | number:'1.4-4' }} (racine double)</p>
            }
            @if (result()!.type === 'COMPLEX') {
              <p>z&#8321; = {{ result()!.x1 | number:'1.4-4' }} + {{ result()!.x2 | number:'1.4-4' }}i</p>
              <p>z&#8322; = {{ result()!.x1 | number:'1.4-4' }} - {{ result()!.x2 | number:'1.4-4' }}i</p>
            }
            <details class="steps">
              <summary>Etapes de resolution ({{ result()!.steps.length }})</summary>
              <ol>
                @for (step of result()!.steps; track $index) {
                  <li>{{ step }}</li>
                }
              </ol>
            </details>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .page { padding: 2rem; display: flex; justify-content: center; }
    .panel { background: white; border-radius: 12px; padding: 2.5rem; max-width: 700px; width: 100%; box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
    h2 { color: #1a237e; margin-bottom: 0.3rem; }
    .hint { color: #888; margin-bottom: 1.5rem; font-size: 0.9rem; }
    .form .fields { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 1rem; margin-bottom: 1.2rem; }
    label { display: flex; flex-direction: column; gap: 0.3rem; font-weight: 500; color: #333; font-size: 0.9rem; }
    input { padding: 0.6rem; border: 1px solid #ddd; border-radius: 6px; font-size: 1rem; outline: none; transition: border 0.2s; }
    input:focus { border-color: #1a237e; }
    button[type=submit] { background: #1a237e; color: white; border: none; padding: 0.7rem 2rem; border-radius: 6px; font-size: 1rem; cursor: pointer; transition: background 0.2s; }
    button:disabled { background: #9e9e9e; cursor: not-allowed; }
    .error { background: #ffebee; color: #c62828; padding: 1rem; border-radius: 6px; margin-top: 1rem; }
    .result { margin-top: 1.5rem; padding: 1.5rem; border-radius: 8px; border-left: 4px solid; }
    .result-two_real { background: #e8f5e9; border-color: #4caf50; }
    .result-one_double { background: #fff3e0; border-color: #ff9800; }
    .result-complex { background: #fce4ec; border-color: #e91e63; }
    .result h3 { color: #1a237e; margin-bottom: 0.5rem; }
    .result p { margin: 0.3rem 0; font-size: 1.05rem; }
    .badge { display: inline-block; padding: 0.2rem 0.7rem; border-radius: 20px; font-size: 0.8rem; font-weight: bold; margin-bottom: 0.8rem; }
    .badge-two_real { background: #4caf50; color: white; }
    .badge-one_double { background: #ff9800; color: white; }
    .badge-complex { background: #e91e63; color: white; }
    .steps { margin-top: 1rem; }
    .steps summary { cursor: pointer; color: #1a237e; font-weight: 500; }
    .steps ol { margin-top: 0.5rem; padding-left: 1.2rem; }
    .steps li { margin: 0.3rem 0; font-size: 0.9rem; font-family: monospace; color: #444; }
  `]
})
export class SymbolicComponent {
  a = 1; b = -5; c = 6;
  result = signal<SolveResponse | null>(null);
  loading = signal(false);
  error = signal('');
  constructor(private svc: EquationService) {}
  solve() {
    if (this.a === 0) { this.error.set("a ne peut pas etre 0"); return; }
    this.loading.set(true);
    this.error.set('');
    this.result.set(null);
    this.svc.solve({ a: this.a, b: this.b, c: this.c }).subscribe({
      next: r => { this.result.set(r); this.loading.set(false); },
      error: e => { this.error.set("Erreur API: " + (e.message || 'Serveur indisponible')); this.loading.set(false); }
    });
  }
  typeLabel(type: string): string {
    return { TWO_REAL: 'Deux racines reelles', ONE_DOUBLE: 'Racine double', COMPLEX: 'Racines complexes' }[type] ?? type;
  }
}
