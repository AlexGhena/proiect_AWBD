export type AddressLabel = 'HOME' | 'BILLING' | 'WORK' | 'OTHER';

export const ADDRESS_LABELS: AddressLabel[] = ['HOME', 'BILLING', 'WORK', 'OTHER'];

// Mirrors userService's ProfileResponse.
export interface Profile {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProfileRequest {
  userId: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  phone?: string | null;
}

// Mirrors userService's AddressResponse.
export interface Address {
  id: string;
  profileId: string;
  label: AddressLabel;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAddressRequest {
  profileId: string;
  label: AddressLabel;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  isDefault: boolean;
}

export interface UpdateAddressRequest {
  label: AddressLabel;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  isDefault: boolean;
}
