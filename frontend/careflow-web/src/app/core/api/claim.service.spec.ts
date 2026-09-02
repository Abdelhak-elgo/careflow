import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { ClaimService } from './claim.service';

describe('ClaimService', () => {
  let service: ClaimService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ClaimService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('POST /api/claims with submit payload', () => {
    service
      .submit({
        patientId: 'patient-42',
        careType: 'DENTAL',
        amount: 50,
        currency: 'EUR',
        careDate: '2026-08-15',
      })
      .subscribe();
    const req = httpMock.expectOne('/api/claims');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.amount).toBe(50);
    req.flush({});
  });

  it('GET /api/claims without status filter', () => {
    service.list().subscribe();
    const req = httpMock.expectOne((r) => r.url === '/api/claims');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('status')).toBe(false);
    req.flush([]);
  });

  it('GET /api/claims with status filter', () => {
    service.list('PENDING').subscribe();
    const req = httpMock.expectOne((r) => r.url === '/api/claims' && r.params.get('status') === 'PENDING');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
