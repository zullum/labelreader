import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DiscoveryService } from '../../../core/services/discovery.service';
import { Submission } from '../../../core/services/submission.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  standalone: false,
  selector: 'app-discovery',
  templateUrl: './discovery.component.html',
  styleUrls: ['./discovery.component.scss'],
})
export class DiscoveryComponent implements OnInit {
  submissions: Submission[] = [];
  isLoading = true;
  currentPage = 0;
  totalPages = 0;
  selectedGenre = '';
  selectedStatus = '';

  genres = [
    'All',
    'Electronic',
    'Hip Hop',
    'Rock',
    'Pop',
    'R&B',
    'Jazz',
    'Classical',
    'Country',
    'Reggae',
  ];

  statuses = [
    { value: '', label: 'All' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'UNDER_REVIEW', label: 'Under Review' },
    { value: 'APPROVED', label: 'Approved' },
  ];

  constructor(
    private discoveryService: DiscoveryService,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadSubmissions();
  }

  loadSubmissions(): void {
    this.isLoading = true;

    const genre = this.selectedGenre && this.selectedGenre !== 'All' 
      ? this.selectedGenre 
      : undefined;

    this.discoveryService
      .discoverSubmissions(this.currentPage, 20, genre, this.selectedStatus)
      .subscribe({
        next: (response) => {
          this.submissions = response.content;
          this.totalPages = response.totalPages;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading submissions:', error);
          this.isLoading = false;
        },
      });
  }

  onGenreChange(genre: string): void {
    this.selectedGenre = genre;
    this.currentPage = 0;
    this.loadSubmissions();
  }

  onStatusChange(status: string): void {
    this.selectedStatus = status;
    this.currentPage = 0;
    this.loadSubmissions();
  }

  viewSubmission(id: number): void {
    this.router.navigate(['/label/submissions', id]);
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadSubmissions();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadSubmissions();
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
