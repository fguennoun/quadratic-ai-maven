import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EquationRequest, SolveResponse, NeuralResponse, NeuralStatusResponse, TrainingResponse } from '../models/api.models';
const API = 'http://localhost:8080/api';
/**
 * Service HTTP pour les appels a l API Spring Boot.
 * Gere la resolution symbolique et les operations du reseau neuronal.
 */
@Injectable({ providedIn: 'root' })
export class EquationService {
  constructor(private http: HttpClient) {}
  /** POST /api/solve — Resolution symbolique */
  solve(req: EquationRequest): Observable<SolveResponse> {
    return this.http.post<SolveResponse>(`${API}/solve`, req);
  }
  /** GET /api/neural/status — Statut du modele */
  getNeuralStatus(): Observable<NeuralStatusResponse> {
    return this.http.get<NeuralStatusResponse>(`${API}/neural/status`);
  }
  /** POST /api/neural/predict — Prediction par NN */
  predict(req: EquationRequest): Observable<NeuralResponse> {
    return this.http.post<NeuralResponse>(`${API}/neural/predict`, req);
  }
  /** POST /api/neural/train — Entrainement du modele */
  train(epochs: number = 200, datasetSize: number = 10000): Observable<TrainingResponse> {
    return this.http.post<TrainingResponse>(
      `${API}/neural/train?epochs=${epochs}&datasetSize=${datasetSize}`, {}
    );
  }
}
