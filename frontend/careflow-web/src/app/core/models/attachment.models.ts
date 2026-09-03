export interface Attachment {
  id: string;
  claimId: string;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  uploadedBy: string;
  uploadedAt: string;
}

export interface RenameAttachmentRequest {
  originalName: string;
}
