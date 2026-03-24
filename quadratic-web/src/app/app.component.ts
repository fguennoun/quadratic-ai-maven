import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <div class="brand">
        <span class="logo">&#9632;</span>
        <span class="title">QuadraticAI</span>
      </div>
      <div class="nav-links">
        <a routerLink="/home"     routerLinkActive="active">Accueil</a>
        <a routerLink="/symbolic" routerLinkActive="active">Symbolique</a>
        <a routerLink="/neural"   routerLinkActive="active">Neuronal</a>
        <a routerLink="/chat"     routerLinkActive="active">Chat</a>
      </div>
    </nav>
    <main class="content">
      <router-outlet />
    </main>
  `,
  styleUrl: './app.component.scss'
})
export class AppComponent {}
