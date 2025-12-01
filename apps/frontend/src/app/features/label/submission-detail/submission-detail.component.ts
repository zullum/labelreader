import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DiscoveryService, Rating } from '../../../core/services/discovery.service';
import { Submission } from '../../../core/services/submission.service';

@Component({
  standalone: false,
  selector: 'app-submission-detail',
  templateUrl: './submission-detail.component.html',
  styleUrls: ['./submission-detail.component.scss'],
})
export class SubmissionDetailComponent implements OnInit {
  submission: Submission | null = null;
  existingRating: Rating | null = null;
  ratingForm: FormGroup;
  isLoading = true;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private discoveryService: DiscoveryService
  ) {
    this.ratingForm = this.fb.group({
      rating: [null, [Validators.required, Validators.min(1), Validators.max(5)]],
      reviewText: [''],
      isInterested: [false],
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadSubmission(id);
    this.loadExistingRating(id);
  }

  loadSubmission(id: number): void {
    this.discoveryService.getSubmission(id).subscribe({
      next: (submission) => {
        this.submission = submission;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading submission:', error);
        this.errorMessage = 'Failed to load submission';
        this.isLoading = false;
      },
    });
  }

  loadExistingRating(submissionId: number): void {
    this.discoveryService.getSubmissionRating(submissionId).subscribe({
      next: (rating) => {
        this.existingRating = rating;
        if (rating) {
          this.ratingForm.patchValue({
            rating: rating.rating,
            reviewText: rating.reviewText || '',
            isInterested: rating.isInterested,
          });
        }
      },
      error: () => {
        // No existing rating, that's fine
      },
    });
  }

  setRating(value: number): void {
    this.ratingForm.patchValue({ rating: value });
  }

  onSubmit(): void {
    if (this.ratingForm.valid && this.submission) {
      this.isSubmitting = true;
      this.errorMessage = '';
      this.successMessage = '';

      this.discoveryService
        .rateSubmission(this.submission.id, this.ratingForm.value)
        .subscribe({
          next: (rating) => {
            this.isSubmitting = false;
            this.existingRating = rating;
            this.successMessage = 'Rating submitted successfully!';
            setTimeout(() => {
              this.successMessage = '';
            }, 3000);
          },
          error: (error) => {
            this.isSubmitting = false;
            this.errorMessage =
              error.error?.message || 'Failed to submit rating';
          },
        });
    }
  }

  goBack(): void {
    this.router.navigate(['/label/discover']);
  }
}
