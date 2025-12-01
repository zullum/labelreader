import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { DiscoveryComponent } from './discovery/discovery.component';
import { SubmissionDetailComponent } from './submission-detail/submission-detail.component';

const routes: Routes = [
  { path: 'discover', component: DiscoveryComponent },
  { path: 'submissions/:id', component: SubmissionDetailComponent },
];

@NgModule({
  declarations: [DiscoveryComponent, SubmissionDetailComponent],
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule.forChild(routes)],
})
export class LabelModule {}
