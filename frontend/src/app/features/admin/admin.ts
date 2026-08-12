import { Component, signal } from '@angular/core';

import { Topbar } from '../../layout/topbar/topbar';
import { DeletedUsers } from './deleted/deleted-users';
import { Pending } from './pending/pending';
import { RolesList } from './roles/roles-list';
import { UsersList } from './users/users-list';

type AdminTab = 'pending' | 'users' | 'deleted' | 'roles';

@Component({
  selector: 'app-admin',
  imports: [Topbar, Pending, UsersList, DeletedUsers, RolesList],
  templateUrl: './admin.html',
  styleUrl: './admin.scss',
})
export class Admin {
  protected readonly activeTab = signal<AdminTab>('pending');

  protected selectTab(tab: AdminTab): void {
    this.activeTab.set(tab);
  }
}
