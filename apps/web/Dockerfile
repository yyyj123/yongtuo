FROM node:24.20.0-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG NUXT_IMAGE_DOMAINS=
ENV NUXT_IMAGE_DOMAINS=$NUXT_IMAGE_DOMAINS
RUN npm run build

FROM node:24.20.0-alpine AS runtime

ENV NODE_ENV=production \
    NITRO_HOST=0.0.0.0 \
    NITRO_PORT=3000

WORKDIR /app
COPY --from=build --chown=node:node /app/.output ./.output

USER node

EXPOSE 3000

CMD ["node", ".output/server/index.mjs"]
