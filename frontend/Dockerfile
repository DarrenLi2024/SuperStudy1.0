# ---- Build Stage ----
# 注意：构建上下文为项目根目录（通过 docker-compose 的 context: . 指定）
# 所有 COPY 路径需以 frontend/ 为前缀
FROM node:18-alpine AS build

WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ .
RUN npm run build

# ---- Runtime Stage ----
FROM nginx:1.25-alpine

WORKDIR /usr/share/nginx/html

RUN rm -rf ./*

COPY --from=build /app/dist .

# nginx.conf 位于项目根目录
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
