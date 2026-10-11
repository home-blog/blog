# 화면 컨테이너. 배포용: 빌드한 화면 파일을 Nginx가 내려 주고, /api는 서버 컨테이너로 넘긴다
FROM node:22-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 80
