import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Submission {
  id: number;
  artistId: number;
  title: string;
  artistName: string;
  genre?: string;
  subGenre?: string;
  bpm?: number;
  keySignature?: string;
  filePath: string;
  fileSizeBytes: number;
  durationSeconds?: number;
  description?: string;
  lyrics?: string;
  isPublished: boolean;
  submissionStatus: string;
  playCount: number;
  averageRating: number;
  totalRatings: number;
  createdAt: string;
  updatedAt: string;
}

export interface SubmissionRequest {
  title: string;
  artistName: string;
  genre?: string;
  subGenre?: string;
  bpm?: number;
  keySignature?: string;
  description?: string;
  lyrics?: string;
}

export interface PagedSubmissions {
  content: Submission[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ArtistStats {
  totalSubmissions: number;
  totalPlays: number;
  averageRating: number;
  pendingSubmissions: number;
  approvedSubmissions: number;
  rejectedSubmissions: number;
  signingRequests: number;
}

@Injectable({ providedIn: 'root' })
export class SubmissionService {
  private readonly API_URL = 'http://localhost:8080/api/artist/submissions';

  constructor(private http: HttpClient) {}

  uploadSubmission(
    file: File,
    metadata: SubmissionRequest
  ): Observable<HttpEvent<Submission>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('metadata', JSON.stringify(metadata));

    return this.http.post<Submission>(this.API_URL, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  getSubmissions(
    page: number = 0,
    size: number = 10
  ): Observable<PagedSubmissions> {
    return this.http.get<PagedSubmissions>(
      `${this.API_URL}?page=${page}&size=${size}`
    );
  }

  getSubmission(id: number): Observable<Submission> {
    return this.http.get<Submission>(`${this.API_URL}/${id}`);
  }

  deleteSubmission(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  getStats(): Observable<ArtistStats> {
    return this.http.get<ArtistStats>('http://localhost:8080/api/artist/stats');
  }
}
