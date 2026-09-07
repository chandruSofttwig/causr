FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci || npm install
COPY . .
ARG VITE_BFF_API_BASE=http://localhost:8090
ARG VITE_BFF_WS_URL=ws://localhost:8090/ws/anomalies
ARG VITE_API_KEY=causr-local-key
ARG VITE_TENANT_ID=default
ENV VITE_BFF_API_BASE=$VITE_BFF_API_BASE \
    VITE_BFF_WS_URL=$VITE_BFF_WS_URL \
    VITE_API_KEY=$VITE_API_KEY \
    VITE_TENANT_ID=$VITE_TENANT_ID
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
