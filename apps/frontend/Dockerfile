# Frontend Dockerfile
FROM node:22-alpine AS builder

WORKDIR /app
COPY . .
RUN npm install --legacy-peer-deps

RUN npx nx build frontend --configuration=production

FROM nginx:1.25-alpine
COPY --from=builder /app/dist/apps/frontend/browser /usr/share/nginx/html
COPY apps/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
