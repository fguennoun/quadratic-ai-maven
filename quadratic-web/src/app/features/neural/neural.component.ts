import { Component, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { EquationService } from '../../core/services/equation.service';
import { NeuralResponse, NeuralStatusResponse } from '../../core/models/api.models';
@Component({
  selector: 'app-neural',
  standalone: true,
  imports: [FormsModule, CommonModule],
  template: `
    <div class="page">
      <div class="panel">
        <h2>Reseau Neuronal MLP</h2>
        <p class="hint">Prediction par apprentissage automatique. Architecture [3, 32, 32, 16, 3] + Adam.</p>
        <div class="status-bar" [class.loaded]="status()?.modelLoaded">
          <span class="dot"></span>
          <span>{{ status()?.message ?? 'Chargement...' }}</span>
        </div>
        <form (ngSubmit)="predict()" class="form">
          <div class="fields">
            <label>a <input type="number" [(ngModel)]="a" name="a" step="any" placeholder="1" /></label>
            <label>b <input type="number" [(ngModel)]="b" name="b" step="any" placeholder="-5" /></label>
            <label>c <input type="number" [(ngModel)]="c" name="c" step="any" placeholder="6" /></label>
          </div>
          <div class="actions">
            <button type="submit" [disabled]="loadingPredict()">
              {{ loadingPredict() ? 'Prediction...' : 'Predire' }}
            </button>
            <button type="button" class="btn-train" (click)="train()" [disabled]="loadingTrain()">
              {{ loadingTrain() ? 'Entrainement (' + trainEpochs + ' epochs)...' : 'Entrainer le modele' }}
            </button>
          </div>
        </form>
        @if (error()) { <div class="error">{{ error() }}</div> }
        @if (trainMessage()) { <div class="train-msg">{{ trainMessage() }}</div> }
        @if (result()) {
          <div class="result">
            <h3>Prediction NN</h3>
            <p class="type-badge">{{ typeLabel(result()!.type) }}</p>
            <p>&#x394; (predit) = {{ result()!.delta | number:'1.4-4' }}</p>
            @if (result()!.type === 'TWO_REAL') {
              <p>x&#8321; &asymp; {{ result()!.x1 | number:'1.4-4' }}</p>
              <p>x&#8322; &asymp; {{ result()!.x2 | number:'1.4-4' }}</p>
            }
            @if (result()!.type === 'COMPLEX') {
              <p>Partie reelle &asymp; {{ result()!.x1 | number:'1.4-4' }}</p>
              <p>Partie imaginaire &asymp; {{ result()!.x2 | number:'1.4-4' }}</p>
            }
            <p class="confidence">Confiance : {{ result()!.confidence * 100 | number:'1.0-0' }}%
              {{ result()!.modelLoaded ? '(modele entraine)' : '(poids aleatoires)' }}</p>
          </div>
        }
        <div class="train-config">
          <label>Epochs <input type="number" [(ngModel)]="trainEpochs" name="ep" min="50" max="1000" /></label>
          <label>Dataset <input type="number" [(ngModel)]="trainDataset" name="ds" min="1000" max="50000" step="1000" /></label>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page { padding: 2rem; display: flex; justify-content: center; }
    .panel { background: white; border-radius: 12px; padding: 2.5rem; max-width: 700px; width: 100%; box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
    h2 { color: #1a237e; margin-bottom: 0.3rem; }
    .hint { color: #888; margin-bottom: 1rem; font-size: 0.9rem; }
    .status-bar { display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 1rem; background: #ffebee; border-radius: 6px; margin-bottom: 1.2rem; font-size: 0.85rem; color: #b71c1c; }
    .status-bar.loaded { background: #e8f5e9; color: #2e7d32; }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: currentColor; }
    .form .fields { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 1rem; margin-bottom: 1rem; }
    label { display: flex; flex-direction: column; gap: 0.3rem; font-weight: 500; color: #333; font-size: 0.9rem; }
    input[type=number] { padding: 0.6rem; border: 1px solid #ddd; border-radius: 6px; font-size: 1rem; }
    .actions { display: flex; gap: 1rem; margin-bottom: 1rem; }
    button { padding: 0.7rem 1.5rem; border: none; border-radius: 6px; cursor: pointer; font-size: 0.95rem; }
    button[type=submit] { background: #1a237e; color: white; }
    .btn-train { background: #e65100; color: white; }
    button:disabled { background: #9e9e9e; cursor: not-allowed; }
    .error { background: #ffebee; color: #c62828; padding: 1rem; border-radius: 6px; }
    .train-msg { background: #e8f5e9; color: #2e7d32; padding: 1rem; border-radius: 6px; font-size: 0.9rem; }
    .result { margin-top: 1.5rem; padding: 1.5rem; background: #e3f2fd; border-radius: 8px; border-left: 4px solid #1565c0; }
    .result h3 { color: #1a237e; margin-bottom: 0.5rem; }
    .result p { margin: 0.3rem 0; }
    .type-badge { font-weight: bold; color: #1565c0; }
    .confidence { color: #888; font-size: 0.85rem; margin-top: 0.5rem; }
    .train-config { display: flex; gap: 1rem; margin-top: 1.5rem; padding-top: 1rem; border-top: 1px solid #eee; }
    .train-config label { flex: 1; }
  `]
})
export class NeuralComponent implements OnInit {
  a = 1; b = -5; c = 6;
  trainEpochs = 200;
  trainDataset = 10000;
  status = signal<NeuralStatusResponse | null>(null);
  result = signal<NeuralResponse | null>(null);
  loadingPredict = signal(false);
  loadingTrain = signal(false);
  error = signal('');
  trainMessage = signal('');
  constructor(private svc: EquationService) {}
  ngOnInit() {
    this.svc.getNeuralStatus().subscribe({ next: s => this.status.set(s), error: () => {} });
  }
  predict() {
    this.loadingPredict.set(true); this.error.set(''); this.result.set(null);
    this.svc.predict({ a: this.a, b: this.b, c: this.c }).subscribe({
      next: r => { this.result.set(r); this.loadingPredict.set(false); },
      error: e => { this.error.set('Erreur: ' + (e.message || 'API indisponible')); this.loadingPredict.set(false); }
    });
  }
  train() {
    this.loadingTrain.set(true); this.trainMessage.set('');
    this.svc.train(this.trainEpochs, this.trainDataset).subscribe({
      next: r => {
        this.trainMessage.set(r.message + ' | Loss finale: ' + r.finalLoss.toFixed(6));
        this.loadingTrain.set(false);
        this.svc.getNeuralStatus().subscribe({ next: s => this.status.set(s), error: () => {} });
      },
      error: e => { this.error.set('Erreur entrainement: ' + e.message); this.loadingTrain.set(false); }
    });
  }
  typeLabel(type: string): string {
    return { TWO_REAL: 'Deux racines reelles', ONE_DOUBLE: 'Racine double', COMPLEX: 'Racines complexes' }[type] ?? type;
  }
}
