# SmartWork 배포 요약

AWS EC2 프리티어 + Docker 단일 컨테이너 배포 구성

---

## 🎯 배포 아키텍처

```
GitHub Repository
       ↓
 [GitHub Actions]
       ↓
  Docker Build
       ↓
  Docker Hub Push
       ↓
   EC2 Instance (t2.micro)
       ↓
  Docker Container (SmartWork App)
       ↓
  AWS RDS (Oracle/PostgreSQL)
```

---

## 📦 생성된 파일

| 파일 | 용도 |
|------|------|
| `Dockerfile` | Docker 이미지 빌드 정의 |
| `docker-compose.yml` | 로컬 개발 및 배포 설정 |
| `.github/workflows/deploy.yml` | CI/CD 자동화 워크플로우 |
| `deploy.sh` | EC2 배포 스크립트 |
| `EC2_DEPLOYMENT_GUIDE.md` | 상세 배포 가이드 |
| `.env.example` | 환경 변수 템플릿 |

---

## 🚀 빠른 시작

### 1. 사전 준비
```bash
# 필요한 계정
- AWS 계정 (프리티어)
- Docker Hub 계정
- GitHub Repository
```

### 2. EC2 설정
```bash
# EC2 인스턴스 생성 (t2.micro)
# Docker 설치
# Swap 메모리 추가 (1GB)
# .env 파일 설정
```

### 3. 수동 배포
```bash
# EC2에서 실행
git clone https://github.com/yourusername/smartwork.git
cd smartwork
cp .env.example .env
# .env 파일 수정 (DB 정보, JWT 시크릿 등)
chmod +x deploy.sh
./deploy.sh deploy
```

### 4. 자동 배포 (CI/CD)
```bash
# GitHub Secrets 설정 후
git push origin main
# GitHub Actions가 자동으로 배포 실행
```

---

## 📊 리소스 사용량

### EC2 t2.micro (프리티어)
- **CPU**: 1 vCPU
- **RAM**: 1 GB (Swap +1GB = 총 2GB)
- **Storage**: 8 GB
- **컨테이너 메모리 제한**: 768MB
- **JVM 힙 메모리**: 최대 512MB

### 예상 비용
- **EC2 t2.micro**: 월 750시간 무료 (프리티어)
- **RDS db.t3.micro**: 월 750시간 무료 (프리티어)
- **EBS 20GB**: 무료 (프리티어)
- **Elastic IP**: $0 (인스턴스 실행 중)
- **총 비용**: **$0/월** (프리티어 한도 내)

---

## 🔧 주요 명령어

```bash
# 배포
./deploy.sh deploy

# 재시작
./deploy.sh restart

# 중지
./deploy.sh stop

# 시작
./deploy.sh start

# 로그 확인 (최근 100줄)
./deploy.sh logs 100

# 실시간 로그
./deploy.sh logs 1000

# 상태 확인
./deploy.sh status

# 리소스 정리
./deploy.sh cleanup
```

---

## 🌐 접속 주소

```
# 애플리케이션
http://[EC2_PUBLIC_IP]:8080

# API 문서 (Swagger UI)
http://[EC2_PUBLIC_IP]:8080/swagger-ui.html

# 헬스체크
http://[EC2_PUBLIC_IP]:8080/actuator/health

# H2 Console (H2 사용 시)
http://[EC2_PUBLIC_IP]:8080/h2-console
```

---

## 🔐 GitHub Secrets

| Secret 이름 | 설명 | 예시 |
|-------------|------|------|
| `DOCKER_USERNAME` | Docker Hub 사용자명 | `yourusername` |
| `DOCKER_PASSWORD` | Docker Hub 비밀번호 | `yourpassword` |
| `EC2_HOST` | EC2 퍼블릭 IP | `54.123.45.67` |
| `EC2_USERNAME` | EC2 사용자명 | `ec2-user` |
| `EC2_PRIVATE_KEY` | SSH 키 (.pem 전체) | `-----BEGIN RSA...` |
| `DB_URL` | 데이터베이스 URL | `jdbc:oracle:thin:@...` |
| `DB_USERNAME` | DB 사용자명 | `admin` |
| `DB_PASSWORD` | DB 비밀번호 | `dbpassword` |
| `JWT_SECRET` | JWT 시크릿 (Base64) | `c21hcnR3b3Jr...` |

---

## 📝 배포 체크리스트

### 초기 설정
- [ ] AWS EC2 인스턴스 생성 (t2.micro)
- [ ] Elastic IP 할당
- [ ] 보안 그룹 설정 (22, 80, 8080 포트)
- [ ] SSH 키 다운로드 및 보관
- [ ] Docker 설치
- [ ] Swap 메모리 추가 (1GB)
- [ ] RDS 인스턴스 생성 (또는 H2 설정)
- [ ] Docker Hub 계정 생성

### GitHub 설정
- [ ] Repository에 코드 push
- [ ] GitHub Secrets 설정 (9개)
- [ ] GitHub Actions 활성화 확인

### 배포 확인
- [ ] `./deploy.sh deploy` 성공
- [ ] `http://[EC2_IP]:8080` 접속 가능
- [ ] `http://[EC2_IP]:8080/actuator/health` 응답 확인
- [ ] Swagger UI 접속 확인
- [ ] 로그 정상 확인

### 모니터링
- [ ] CloudWatch 알람 설정
- [ ] 주기적 헬스체크
- [ ] 로그 모니터링

---

## 🐛 트러블슈팅

### 메모리 부족
```bash
# Swap 확인
free -h

# JVM 메모리 줄이기
# docker-compose.yml에서 JAVA_OPTS 수정:
JAVA_OPTS=-Xmx384m -Xms192m
```

### 애플리케이션 시작 실패
```bash
# 로그 확인
docker logs smartwork-app

# 컨테이너 재시작
./deploy.sh restart
```

### 데이터베이스 연결 실패
```bash
# 환경 변수 확인
docker exec smartwork-app env | grep DB

# RDS 보안 그룹 확인
# EC2 보안 그룹에서 1521 포트 허용
```

---

## 📚 상세 가이드

전체 배포 과정은 **[EC2_DEPLOYMENT_GUIDE.md](./EC2_DEPLOYMENT_GUIDE.md)** 참고

---

## 🎓 다음 단계

1. **도메인 연결**: Route 53으로 커스텀 도메인
2. **HTTPS 적용**: Let's Encrypt SSL 인증서
3. **Nginx 리버스 프록시**: 80 포트로 리다이렉트
4. **CloudWatch 모니터링**: 메트릭 수집 및 알람
5. **로그 중앙화**: CloudWatch Logs 또는 ELK 스택
6. **백업 자동화**: RDS 스냅샷 자동화

---

**배포 성공을 기원합니다! 🚀**
