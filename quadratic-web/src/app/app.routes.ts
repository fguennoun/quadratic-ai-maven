import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  {
    path: 'home',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'symbolic',
    loadComponent: () => import('./features/symbolic/symbolic.component').then(m => m.SymbolicComponent)
  },
  {
    path: 'neural',
    loadComponent: () => import('./features/neural/neural.component').then(m => m.NeuralComponent)
  },
  {
    path: 'chat',
    loadComponent: () => import('./features/chat/chat.component').then(m => m.ChatComponent)
  },
  { path: '**', redirectTo: 'home' }
];
