import { Component } from '@angular/core';

@Component({
  standalone: false,
  selector: 'app-profile',
  template: `
    <div class="profile-container">
      <h1>Artist Profile</h1>
      <p>Profile management coming soon...</p>
    </div>
  `,
  styles: [
    `
      .profile-container {
        min-height: 100vh;
        background-color: #1a2e3d;
        padding: 32px 24px;
        max-width: 800px;
        margin: 0 auto;

        h1 {
          color: #ffffff;
          margin-bottom: 24px;
        }

        p {
          color: #a0aec0;
        }
      }
    `,
  ],
})
export class ProfileComponent {}
