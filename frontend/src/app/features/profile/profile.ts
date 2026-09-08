import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../core/auth/auth.service';
import { ProfileService } from '../../core/services/profile.service';
import { ADDRESS_LABELS, Address, AddressLabel, Profile } from '../../core/models/profile.model';
import { Topbar } from '../../layout/topbar/topbar';

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, Topbar],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly profileService = inject(ProfileService);
  private readonly authService = inject(AuthService);

  protected readonly labels = ADDRESS_LABELS;

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  protected readonly profile = signal<Profile | null>(null);
  // No profile row exists yet for this user: show the create form instead of the edit form.
  protected readonly profileMissing = signal(false);
  protected readonly editingProfile = signal(false);
  protected readonly profileSaving = signal(false);
  protected readonly profileError = signal<string | null>(null);
  protected readonly profileSuccess = signal<string | null>(null);

  protected readonly addresses = signal<Address[]>([]);
  protected readonly addressesError = signal<string | null>(null);
  protected readonly addressFormOpen = signal(false);
  protected readonly editingAddressId = signal<string | null>(null);
  protected readonly addressSaving = signal(false);
  protected readonly addressError = signal<string | null>(null);
  protected readonly confirmingDeleteId = signal<string | null>(null);
  protected readonly deletingId = signal<string | null>(null);

  protected readonly profileForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', [Validators.maxLength(30), Validators.pattern(/^[+0-9 ()-]*$/)]],
  });

  protected readonly addressForm = this.fb.nonNullable.group({
    label: ['HOME' as AddressLabel, [Validators.required]],
    street: ['', [Validators.required, Validators.maxLength(150)]],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    postalCode: ['', [Validators.required, Validators.maxLength(20)]],
    country: ['', [Validators.required, Validators.pattern(/^[A-Z]{2}$/)]],
    isDefault: [false],
  });

  constructor() {
    // The backend requires an uppercase 2-letter ISO code; keep the field uppercase as the user types.
    this.addressForm.controls.country.valueChanges.subscribe((value) => {
      const upper = value.toUpperCase();
      if (upper !== value) {
        this.addressForm.controls.country.setValue(upper, { emitEvent: false });
      }
    });
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  private get userId(): string | null {
    return this.authService.currentUser()?.userId ?? null;
  }

  private loadProfile(): void {
    const userId = this.userId;
    if (!userId) {
      this.loading.set(false);
      this.loadError.set('Your session could not be read. Please sign in again.');
      return;
    }

    this.loading.set(true);
    this.loadError.set(null);
    this.profileService.getByUser(userId).subscribe({
      next: (profile) => {
        this.applyProfile(profile);
        this.loading.set(false);
        this.loadAddresses(profile.id);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 404) {
          this.profileMissing.set(true);
          this.editingProfile.set(true);
        } else {
          this.loadError.set('Could not load your profile. Please try again.');
        }
      },
    });
  }

  private applyProfile(profile: Profile): void {
    this.profile.set(profile);
    this.profileMissing.set(false);
    this.profileForm.reset({
      firstName: profile.firstName,
      lastName: profile.lastName,
      phone: profile.phone ?? '',
    });
  }

  protected startEditProfile(): void {
    const current = this.profile();
    if (current) {
      this.profileForm.reset({
        firstName: current.firstName,
        lastName: current.lastName,
        phone: current.phone ?? '',
      });
    }
    this.profileSuccess.set(null);
    this.editingProfile.set(true);
  }

  protected cancelEditProfile(): void {
    this.editingProfile.set(false);
    this.profileError.set(null);
  }

  protected saveProfile(): void {
    if (this.profileForm.invalid || this.profileSaving()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const userId = this.userId;
    if (!userId) {
      this.profileError.set('Your session could not be read. Please sign in again.');
      return;
    }

    const { firstName, lastName, phone } = this.profileForm.getRawValue();
    const trimmedPhone = phone.trim();
    this.profileSaving.set(true);
    this.profileError.set(null);

    const existing = this.profile();
    const request$ = existing
      ? this.profileService.updateProfile(existing.id, {
          firstName,
          lastName,
          phone: trimmedPhone || null,
        })
      : this.profileService.createProfile({
          userId,
          firstName,
          lastName,
          phone: trimmedPhone || null,
        });

    const wasCreating = existing === null;
    request$.subscribe({
      next: (profile) => {
        this.applyProfile(profile);
        this.profileSaving.set(false);
        this.editingProfile.set(false);
        this.profileSuccess.set(wasCreating ? 'Profile created.' : 'Profile updated.');
        if (wasCreating) {
          this.loadAddresses(profile.id);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.profileSaving.set(false);
        this.profileError.set(
          err.status === 409
            ? 'A profile already exists for this account.'
            : 'Could not save your profile. Please check the fields and try again.',
        );
      },
    });
  }

  private loadAddresses(profileId: string): void {
    this.addressesError.set(null);
    this.profileService.listAddresses(profileId).subscribe({
      next: (addresses) => this.addresses.set(addresses),
      error: () => this.addressesError.set('Could not load your addresses.'),
    });
  }

  protected openCreateAddress(): void {
    this.editingAddressId.set(null);
    this.addressError.set(null);
    this.addressForm.reset({
      label: 'HOME',
      street: '',
      city: '',
      postalCode: '',
      country: '',
      isDefault: false,
    });
    this.addressFormOpen.set(true);
  }

  protected openEditAddress(address: Address): void {
    this.editingAddressId.set(address.id);
    this.addressError.set(null);
    this.confirmingDeleteId.set(null);
    this.addressForm.reset({
      label: address.label,
      street: address.street,
      city: address.city,
      postalCode: address.postalCode,
      country: address.country,
      isDefault: address.isDefault,
    });
    this.addressFormOpen.set(true);
  }

  protected closeAddressForm(): void {
    this.addressFormOpen.set(false);
    this.addressError.set(null);
  }

  protected saveAddress(): void {
    const profile = this.profile();
    if (!profile || this.addressForm.invalid || this.addressSaving()) {
      this.addressForm.markAllAsTouched();
      return;
    }

    const value = this.addressForm.getRawValue();
    this.addressSaving.set(true);
    this.addressError.set(null);

    const editingId = this.editingAddressId();
    const request$ = editingId
      ? this.profileService.updateAddress(editingId, {
          label: value.label,
          street: value.street,
          city: value.city,
          postalCode: value.postalCode,
          country: value.country,
          isDefault: value.isDefault,
        })
      : this.profileService.createAddress({
          profileId: profile.id,
          label: value.label,
          street: value.street,
          city: value.city,
          postalCode: value.postalCode,
          country: value.country,
          isDefault: value.isDefault,
        });

    request$.subscribe({
      next: () => {
        this.addressSaving.set(false);
        this.addressFormOpen.set(false);
        this.loadAddresses(profile.id);
      },
      error: (err: HttpErrorResponse) => {
        this.addressSaving.set(false);
        this.addressError.set(
          err.status === 409
            ? 'You already have a default address. Unset it before making another one default.'
            : 'Could not save this address. Please check the fields and try again.',
        );
      },
    });
  }

  protected askDeleteAddress(id: string): void {
    this.confirmingDeleteId.set(id);
  }

  protected cancelDeleteAddress(): void {
    this.confirmingDeleteId.set(null);
  }

  protected confirmDeleteAddress(id: string): void {
    const profile = this.profile();
    if (!profile) {
      return;
    }
    this.deletingId.set(id);
    this.profileService.deleteAddress(id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.confirmingDeleteId.set(null);
        if (this.editingAddressId() === id) {
          this.addressFormOpen.set(false);
        }
        this.loadAddresses(profile.id);
      },
      error: () => {
        this.deletingId.set(null);
        this.confirmingDeleteId.set(null);
        this.addressesError.set('Could not delete that address. Please try again.');
      },
    });
  }
}
