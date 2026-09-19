# 어디갔지 Frontend

어디갔지의 Frontend는 Next.js 15.5.25, React 19.1.x, JavaScript로 구성되어 있으며 npm을 사용합니다.

## 개발 환경

- Node.js 24.20.0
- npm 11.6.2

Node.js 버전은 `.nvmrc`에 기록되어 있습니다. `package.json`은 Node.js 24.20.0 이상 25 미만, npm 11.6.2 이상 12 미만을 허용합니다.

## 설치 및 실행

Lock 파일과 일치하는 의존성을 설치합니다.

```bash
npm ci
```

개발 서버를 실행합니다.

```bash
npm run dev
```

브라우저에서 http://localhost:3000 에 접속합니다.

프로덕션 빌드를 확인합니다.

```bash
npm run build
```

## 환경변수

Frontend 환경변수에는 브라우저에 공개되어도 되는 값만 작성합니다. API Key, Database 비밀번호, Token과 같은 비밀값은 Frontend 코드나 환경변수에 저장하지 않습니다.

실제 `.env` 및 `.env.*` 파일은 Git에서 제외합니다. 향후 Frontend 환경변수가 필요해질 때만 실제 값이 없는 `.env.example`을 추가합니다.
