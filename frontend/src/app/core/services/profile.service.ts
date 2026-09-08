import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Address,
  CreateAddressRequest,
  CreateProfileRequest,
  Profile,
  UpdateAddressRequest,
  UpdateProfileRequest,
} from '../models/profile.model';

const PROFILES_BASE = '/api/profiles';
const ADDRESSES_BASE = '/api/addresses';
const USERS_BASE = '/api/users';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);

  getByUser(userId: string): Observable<Profile> {
    return this.http.get<Profile>(`${USERS_BASE}/${userId}/profile`);
  }

  createProfile(request: CreateProfileRequest): Observable<Profile> {
    return this.http.post<Profile>(PROFILES_BASE, request);
  }

  updateProfile(profileId: string, request: UpdateProfileRequest): Observable<Profile> {
    return this.http.put<Profile>(`${PROFILES_BASE}/${profileId}`, request);
  }

  listAddresses(profileId: string): Observable<Address[]> {
    return this.http.get<Address[]>(`${PROFILES_BASE}/${profileId}/addresses`);
  }

  createAddress(request: CreateAddressRequest): Observable<Address> {
    return this.http.post<Address>(ADDRESSES_BASE, request);
  }

  updateAddress(addressId: string, request: UpdateAddressRequest): Observable<Address> {
    return this.http.put<Address>(`${ADDRESSES_BASE}/${addressId}`, request);
  }

  deleteAddress(addressId: string): Observable<void> {
    return this.http.delete<void>(`${ADDRESSES_BASE}/${addressId}`);
  }
}
