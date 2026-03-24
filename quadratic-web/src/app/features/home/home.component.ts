import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="hero">
      <div class="hero-content">
        <h1>QuadraticAI</h1>
        <p class="subtitle">Resolution intelligente d equations du 2eme degre</p>
        <p class="description">
          Deux approches : IA symbolique (resolution exacte, etapes detaillees)
          et reseau de neurones MLP (prediction par apprentissage).
        </p>
        <div class="cards">
          <div class="card" routerLink="/symbolic">
            <div class="card-icon">&#9651;</div>
            <h2>IA Symbolique</h2>
            <p>Resolution exacte avec etapes pedagogiques. Entrez a, b, c et obtenez x1, x2 avec le discriminant.</p>
            <button class="btn-primary">Resoudre</button>
          </div>
          <div class="card" routerLink="/neural">
            <div class="card-icon">&#9632;</div>
            <h2>Reseau Neuronal</h2>
            <p>Prediction par MLP entraine sur 10 000 equations. Architecture [3, 32, 32, 16, 3] + Adam.</p>
            <button class="btn-primary">Predire</button>
          </div>
          <div class="card" routerLink="/chat">
            <div class="card-icon">&#9675;</div>
            <h2>Chat IA</h2>
            <p>Dialogue conversationnel guide. La FSM vous guide pas a pas pour saisir et resoudre votre equation.</p>
            <button class="btn-primary">Dialoguer</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .hero { min-height: calc(100vh - 60px); display: flex; align-items: center; justify-content: center; padding: 2rem; }
    .hero-content { max-width: 1000px; text-align: center; }
    h1 { font-size: 3rem; color: #1a237e; margin-bottom: 0.5rem; }
    .subtitle { font-size: 1.3rem; color: #3949ab; margin-bottom: 1rem; }
    .description { color: #555; max-width: 600px; margin: 0 auto 2.5rem; line-height: 1.6; }
    .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.5rem; }
    .card { background: white; border-radius: 12px; padding: 2rem; box-shadow: 0 4px 16px rgba(0,0,0,0.1); cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; text-align: left; }
    .card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.15); }
    .card-icon { font-size: 2.5rem; color: #1a237e; margin-bottom: 1rem; }
    .card h2 { color: #1a237e; margin-bottom: 0.5rem; }
    .card p { color: #666; line-height: 1.5; margin-bottom: 1.5rem; }
    .btn-primary { background: #1a237e; color: white; border: none; padding: 0.6rem 1.5rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem; transition: background 0.2s; }
    .btn-primary:hover { background: #283593; }
  `]
})
export class HomeComponent {}
