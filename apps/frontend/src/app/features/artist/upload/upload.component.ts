import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpEventType } from '@angular/common/http';
import { SubmissionService } from '../../../core/services/submission.service';

@Component({
  standalone: false,
  selector: 'app-upload',
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.scss'],
})
export class UploadComponent {
  uploadForm: FormGroup;
  selectedFile: File | null = null;
  uploadProgress = 0;
  isUploading = false;
  isDragOver = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private submissionService: SubmissionService,
    private router: Router
  ) {
    this.uploadForm = this.fb.group({
      title: ['', Validators.required],
      artistName: ['', Validators.required],
      genre: [''],
      subGenre: [''],
      bpm: ['', [Validators.min(1), Validators.max(300)]],
      keySignature: [''],
      description: [''],
      lyrics: [''],
    });
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFileSelection(files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFileSelection(input.files[0]);
    }
  }

  handleFileSelection(file: File): void {
    // Validate file type
    const validTypes = ['audio/mpeg', 'audio/wav', 'audio/x-wav', 'audio/flac', 'audio/x-flac'];
    if (!validTypes.includes(file.type)) {
      this.errorMessage = 'Invalid file type. Please upload MP3, WAV, or FLAC files.';
      return;
    }

    // Validate file size (50MB)
    const maxSize = 50 * 1024 * 1024;
    if (file.size > maxSize) {
      this.errorMessage = 'File size exceeds 50MB limit.';
      return;
    }

    this.selectedFile = file;
    this.errorMessage = '';
  }

  removeFile(): void {
    this.selectedFile = null;
    this.uploadProgress = 0;
  }

  onSubmit(): void {
    if (this.uploadForm.valid && this.selectedFile) {
      this.isUploading = true;
      this.errorMessage = '';

      this.submissionService
        .uploadSubmission(this.selectedFile, this.uploadForm.value)
        .subscribe({
          next: (event) => {
            if (event.type === HttpEventType.UploadProgress && event.total) {
              this.uploadProgress = Math.round((100 * event.loaded) / event.total);
            } else if (event.type === HttpEventType.Response) {
              this.isUploading = false;
              this.router.navigate(['/artist/dashboard']);
            }
          },
          error: (error) => {
            this.isUploading = false;
            this.errorMessage =
              error.error?.message || 'Upload failed. Please try again.';
          },
        });
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  cancel(): void {
    this.router.navigate(['/artist/dashboard']);
  }
}
