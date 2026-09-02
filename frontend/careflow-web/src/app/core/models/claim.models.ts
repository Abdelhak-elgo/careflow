export type CareType = 'DENTAL' | 'OPTICAL' | 'GENERAL';

export const CARE_TYPES: readonly CareType[] = ['DENTAL', 'OPTICAL', 'GENERAL'] as const;

export type ClaimStatus = 'APPROVED' | 'REJECTED' | 'PENDING';

export const CLAIM_STATUSES: readonly ClaimStatus[] = ['APPROVED', 'REJECTED', 'PENDING'] as const;

export interface SubmitClaimRequest {
  patientId: string;
  careType: CareType;
  amount: number;
  currency: string;
  careDate: string;
}

export interface ClaimResponse {
  id: string;
  patientId: string;
  careType: CareType;
  amount: number;
  currency: string;
  careDate: string;
  status: ClaimStatus;
  decisionReason: string | null;
  submittedAt: string;
  decidedAt: string | null;
}

export type AdminDecision = Extract<ClaimStatus, 'APPROVED' | 'REJECTED'>;

export interface AdminDecisionRequest {
  decision: AdminDecision;
  reason: string;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  violations?: Array<{ field: string; message: string }>;
  claimId?: string;
}
