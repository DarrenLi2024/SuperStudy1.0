# ---- Build Stage ----
FROM node:18-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Runtime Stage ----
FROM nginx:1.25-alpine

WORKDIR /usr/share/nginx/html

RUN rm -rf ./*

COPY --from=build /app/dist .

COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
