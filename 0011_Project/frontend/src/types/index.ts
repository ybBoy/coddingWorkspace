export type Role = 'INTERVIEWER' | 'CANDIDATE';

export interface DimensionScore {
  dimension: string;
  score: number;
  comment: string;
}

export interface EvaluationVersion {
  versionId: number;
  scores: DimensionScore[];
  createdAt: number;
  createdBy: string;
}

export interface WsMessage {
  type: string;
  formId?: string;
  userId?: string;
  userName?: string;
  role?: Role;
  score?: DimensionScore;
  scores?: DimensionScore[];
  versionId?: number;
  version?: EvaluationVersion;
  versions?: EvaluationVersion[];
  users?: Record<string, string>;
  timestamp?: number;
}
