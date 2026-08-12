import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

// Applied to the group, not a single control, since it compares two sibling fields.
export function passwordsMatch(passwordControl: string, confirmControl: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const password = group.get(passwordControl)?.value;
    const confirm = group.get(confirmControl)?.value;
    return password === confirm ? null : { passwordsMismatch: true };
  };
}
