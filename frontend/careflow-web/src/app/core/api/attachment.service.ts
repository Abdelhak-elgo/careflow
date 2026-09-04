import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Attachment, RenameAttachmentRequest } from '../models/attachment.models';

@Injectable({ providedIn: 'root' })
export class AttachmentService {
  private readonly http = inject(HttpClient);

  upload(claimId: string, file: File): Observable<Attachment> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<Attachment>(`/api/claims/${claimId}/attachments`, form);
  }

  listByClaim(claimId: string): Observable<Attachment[]> {
    return this.http.get<Attachment[]>(`/api/claims/${claimId}/attachments`);
  }

  download(id: string): Observable<Blob> {
    return this.http.get(`/api/attachments/${id}`, { responseType: 'blob' });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`/api/attachments/${id}`);
  }

  rename(id: string, request: RenameAttachmentRequest): Observable<Attachment> {
    return this.http.patch<Attachment>(`/api/attachments/${id}`, request);
  }

  adminListAll(): Observable<Attachment[]> {
    return this.http.get<Attachment[]>('/api/admin/attachments');
  }
}
