import { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';

const USER_ID = 'patient-42';

export const xUserIdInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  if (!req.url.startsWith('/api')) {
    return next(req);
  }
  const cloned = req.clone({ setHeaders: { 'X-User-Id': USER_ID } });
  return next(cloned);
};
