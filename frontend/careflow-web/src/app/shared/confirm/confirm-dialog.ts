import { Component, inject } from '@angular/core';
import { ConfirmService } from './confirm.service';

@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.html',
})
export class ConfirmDialog {
  protected readonly confirm = inject(ConfirmService);
}
