// Modeles TypeScript alignes sur les DTOs Spring Boot
export interface EquationRequest {
  a: number;
  b: number;
  c: number;
}
export interface SolveResponse {
  type: 'TWO_REAL' | 'ONE_DOUBLE' | 'COMPLEX';
  delta: number;
  x1: number;
  x2: number;
  equation: string;
  steps: string[];
}
export interface NeuralResponse {
  type: 'TWO_REAL' | 'ONE_DOUBLE' | 'COMPLEX';
  x1: number;
  x2: number;
  delta: number;
  confidence: number;
  modelLoaded: boolean;
}
export interface NeuralStatusResponse {
  modelLoaded: boolean;
  modelPath: string;
  architecture: string;
  message: string;
}
export interface TrainingResponse {
  success: boolean;
  epochs: number;
  finalLoss: number;
  finalValLoss: number;
  message: string;
}
export interface DialogueRequest {
  sessionId: string;
  message: string;
}
export interface DialogueResponse {
  sessionId: string;
  response: string;
  state: string;
  finished: boolean;
}
