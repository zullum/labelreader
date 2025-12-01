import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Submission, PagedSubmissions } from './submission.service';

export interface Rating {
  id: number;
  submissionId: number;
  labelId: number;
  rating: number;
  reviewText?: string;
  isInterested: boolean;
  listenedDurationSeconds?: number;
  createdAt: string;
  updatedAt: string;
}

export interface RatingRequest {
  rating: number;
  reviewText?: string;
  isInterested?: boolean;
  listenedDurationSeconds?: number;
}

@Injectable({ providedIn: 'root' })
export class DiscoveryService {
  private readonly API_URL = 'http://localhost:8080/api/label';

  constructor(private http: HttpClient) {}

  discoverSubmissions(
    page: number = 0,
    size: number = 20,
    genre?: string,
    status?: string
  ): Observable<PagedSubmissions> {
    let url = `${this.API_URL}/discover?page=${page}&size=${size}`;
    if (genre) url += `&genre=${genre}`;
    if (status) url += `&status=${status}`;
    
    return this.http.get<PagedSubmissions>(url);
  }

  getSubmission(id: number): Observable<Submission> {
    return this.http.get<Submission>(`${this.API_URL}/submissions/${id}`);
  }

  rateSubmission(submissionId: number, request: RatingRequest): Observable<Rating> {
    return this.http.post<Rating>(
      `${this.API_URL}/ratings?submissionId=${submissionId}`,
      request
    );
  }

  getSubmissionRating(submissionId: number): Observable<Rating> {
    return this.http.get<Rating>(
      `${this.API_URL}/submissions/${submissionId}/rating`
    );
  }

  getMyRatings(page: number = 0, size: number = 10): Observable<{ content: Rating[] }> {
    return this.http.get<{ content: Rating[] }>(
      `${this.API_URL}/ratings?page=${page}&size=${size}`
    );
  }
}
