# 1. 构建阶段：使用 Node 环境打包 Vue
FROM node:22 AS builder
WORKDIR /app

# 拷贝 package.json 安装依赖
COPY package.json package-lock.json ./
RUN npm config set registry https://registry.npmmirror.com
RUN npm install

# 拷贝源码并构建
COPY . .
RUN npm run build

# 2. 运行阶段：使用 Nginx 托管构建好的文件
FROM nginx:stable-alpine
# 拷贝构建好的 dist 目录到 Nginx 默认目录
COPY --from=builder /app/dist /usr/share/nginx/html
# 拷贝我们自定义的 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]