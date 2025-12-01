import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { SubmissionService, ArtistStats, Submission } from '../../../core/services/submission.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: false
})
export class DashboardComponent implements OnInit {
  stats: ArtistStats | null = null;
  recentSubmissions: Submission[] = [];
  isLoading = true;

  constructor(
    private submissionService: SubmissionService,
    public authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.log('ngOnInit called');
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;
    console.log('Loading dashboard data...');

    forkJoin({
      stats: this.submissionService.getStats(),
      submissions: this.submissionService.getSubmissions(0, 5)
    }).subscribe({
      next: ({ stats, submissions }) => {
        console.log('Data loaded successfully:', { stats, submissions });
        this.stats = stats;
        this.recentSubmissions = submissions.content;
        this.isLoading = false;
        console.log('isLoading set to false');
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading dashboard data:', error);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  navigateToUpload(): void {
    this.router.navigate(['/artist/upload']);
  }

  navigateToProfile(): void {
    this.router.navigate(['/artist/profile']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
