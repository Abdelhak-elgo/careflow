export interface AuditLogEntry {
  id: string;
  occurredAt: string;
  actor: string;
  action: string;
  entityType: string;
  entityId: string | null;
  details: string | null;
}

export interface AuditQuery {
  entityType?: string;
  entityId?: string;
  actor?: string;
  page?: number;
  size?: number;
}
