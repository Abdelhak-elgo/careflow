import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { AuditLogEntry, AuditQuery } from '../models/audit.models';

export interface AuditPage {
  items: AuditLogEntry[];
  total: number;
}

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/admin/audit';

  list(query: AuditQuery = {}): Observable<AuditPage> {
    let params = new HttpParams();
    if (query.entityType) params = params.set('entityType', query.entityType);
    if (query.entityId) params = params.set('entityId', query.entityId);
    if (query.actor) params = params.set('actor', query.actor);
    params = params.set('page', String(query.page ?? 0));
    params = params.set('size', String(query.size ?? 50));

    return this.http
      .get<AuditLogEntry[]>(this.baseUrl, { params, observe: 'response' })
      .pipe(
        map((response) => ({
          items: response.body ?? [],
          total: Number(response.headers.get('X-Total-Count') ?? 0),
        })),
      );
  }
}
