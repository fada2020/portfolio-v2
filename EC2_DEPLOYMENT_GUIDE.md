# AWS EC2 프리티어 배포 가이드

SmartWork 프로젝트를 AWS EC2 프리티어에 Docker 컨테이너 1개로 배포하는 완전한 가이드입니다.

---

## 📋 목차

1. [사전 준비](#사전-준비)
2. [EC2 인스턴스 생성](#ec2-인스턴스-생성)
3. [EC2 초기 설정](#ec2-초기-설정)
4. [Docker 설치](#docker-설치)
5. [데이터베이스 설정](#데이터베이스-설정)
6. [GitHub Secrets 설정](#github-secrets-설정)
7. [수동 배포](#수동-배포)
8. [자동 배포 (CI/CD)](#자동-배포-cicd)
9. [모니터링 및 관리](#모니터링-및-관리)
10. [문제 해결](#문제-해결)

---

## 🎯 사전 준비

### 1. AWS 계정 생성
- https://aws.amazon.com/ko/ 에서 계정 생성
- 신용카드 등록 필요 (프리티어는 무료)

### 2. Docker Hub 계정 생성
- https://hub.docker.com/ 에서 계정 생성
- Username과 Password 기록

### 3. GitHub Repository
- 이 프로젝트를 GitHub에 push

---

## 🖥 EC2 인스턴스 생성

### 1. EC2 대시보드 접속
```
AWS Console → EC2 → 인스턴스 시작
```

### 2. 인스턴스 설정

#### 이름 및 태그
```
이름: smartwork-server
```

#### AMI 선택
```
Amazon Linux 2023 AMI (프리티어 사용 가능)
```

#### 인스턴스 유형
```
t2.micro (프리티어: 1 vCPU, 1 GiB RAM)
```

#### 키 페어 생성
```
1. "새 키 페어 생성" 클릭
2. 키 페어 이름: smartwork-key
3. 키 페어 유형: RSA
4. 프라이빗 키 파일 형식: .pem
5. "키 페어 생성" 클릭
6. ⚠️ 다운로드된 .pem 파일 안전하게 보관!
```

#### 네트워크 설정
```
보안 그룹 규칙:
1. SSH (22) - 내 IP 주소만 허용
2. HTTP (80) - 0.0.0.0/0 (모든 곳에서 허용)
3. 사용자 지정 TCP (8080) - 0.0.0.0/0 (Spring Boot 포트)
```

#### 스토리지 구성
```
8 GiB gp3 (프리티어 기본값)
```

### 3. 인스턴스 시작
```
"인스턴스 시작" 버튼 클릭
```

### 4. Elastic IP 할당 (선택사항, 권장)
```
1. EC2 → 네트워크 및 보안 → 탄력적 IP
2. "탄력적 IP 주소 할당" 클릭
3. "할당" 클릭
4. 할당된 IP 선택 → 작업 → 탄력적 IP 주소 연결
5. 인스턴스 선택 → "연결" 클릭
```

**이유**: 인스턴스 재시작 시 IP 주소 변경 방지

---

## 🔧 EC2 초기 설정

### 1. SSH 접속

#### macOS/Linux:
```bash
# 키 파일 권한 변경
chmod 400 ~/Downloads/smartwork-key.pem

# SSH 접속
ssh -i ~/Downloads/smartwork-key.pem ec2-user@[EC2_PUBLIC_IP]
```

#### Windows (PowerShell):
```powershell
ssh -i C:\Users\YourName\Downloads\smartwork-key.pem ec2-user@[EC2_PUBLIC_IP]
```

### 2. 시스템 업데이트
```bash
sudo yum update -y
```

### 3. 필수 패키지 설치
```bash
# Git 설치
sudo yum install -y git

# wget 설치 (헬스체크용)
sudo yum install -y wget

# htop 설치 (리소스 모니터링)
sudo yum install -y htop
```

### 4. Swap 메모리 추가 (프리티어 필수!)
```bash
# 1GB Swap 파일 생성
sudo dd if=/dev/zero of=/swapfile bs=1M count=1024

# 권한 설정
sudo chmod 600 /swapfile

# Swap 영역 설정
sudo mkswap /swapfile

# Swap 활성화
sudo swapon /swapfile

# 재부팅 후에도 유지되도록 설정
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab

# Swap 확인
free -h
```

**출력 예시**:
```
              total        used        free      shared  buff/cache   available
Mem:          975Mi       150Mi       600Mi       0.0Ki       224Mi       690Mi
Swap:         1.0Gi          0B       1.0Gi
```

---

## 🐳 Docker 설치

### 1. Docker 설치
```bash
# Docker 설치
sudo yum install -y docker

# Docker 서비스 시작
sudo systemctl start docker

# 부팅 시 자동 실행 설정
sudo systemctl enable docker

# 현재 사용자를 docker 그룹에 추가 (sudo 없이 docker 명령 실행)
sudo usermod -aG docker $USER

# 그룹 변경 적용 (재로그인 필요)
newgrp docker

# Docker 버전 확인
docker --version
```

### 2. Docker Compose 설치
```bash
# Docker Compose 최신 버전 다운로드
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 실행 권한 부여
sudo chmod +x /usr/local/bin/docker-compose

# 버전 확인
docker-compose --version
```

---

## 💾 데이터베이스 설정

프리티어 t2.micro (1GB RAM)에서 Oracle DB를 직접 실행하는 것은 **권장하지 않습니다**.

### 옵션 1: AWS RDS 프리티어 사용 (권장)

#### RDS Oracle 프리티어 생성
```
1. AWS Console → RDS → 데이터베이스 생성
2. 표준 생성 선택
3. 엔진: Oracle (또는 PostgreSQL - 완전 무료)
4. 템플릿: 프리 티어
5. DB 인스턴스 식별자: smartwork-db
6. 마스터 사용자 이름: admin
7. 마스터 암호: [안전한 비밀번호]
8. DB 인스턴스 클래스: db.t3.micro
9. 스토리지: 20GB
10. 퍼블릭 액세스: 예
11. VPC 보안 그룹: EC2 보안 그룹과 동일하게 설정
12. 데이터베이스 생성
```

#### 연결 정보 확인
```
RDS → 데이터베이스 → smartwork-db → 엔드포인트 복사
```

#### 환경 변수 설정
```bash
# EC2에서 .env 파일 생성
nano ~/.env
```

```bash
# .env 파일 내용
DB_URL=jdbc:oracle:thin:@[RDS_ENDPOINT]:1521:ORCL
DB_USERNAME=admin
DB_PASSWORD=[마스터_암호]
JWT_SECRET=[랜덤_시크릿_키_Base64]
```

### 옵션 2: H2 In-Memory Database 사용 (개발/테스트용)

#### application-prod.yml 수정
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:smartwork
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
```

**주의**: H2는 메모리 기반이므로 재시작 시 데이터가 사라집니다.

---

## 🔐 GitHub Secrets 설정

GitHub Actions CI/CD를 위한 Secrets 설정

### 1. GitHub Repository Settings
```
Repository → Settings → Secrets and variables → Actions → New repository secret
```

### 2. 추가할 Secrets

| Secret Name | 설명 | 예시 |
|-------------|------|------|
| `DOCKER_USERNAME` | Docker Hub 사용자명 | `yourusername` |
| `DOCKER_PASSWORD` | Docker Hub 비밀번호 | `yourpassword` |
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 | `54.123.45.67` |
| `EC2_USERNAME` | EC2 사용자명 | `ec2-user` |
| `EC2_PRIVATE_KEY` | EC2 SSH 키 (.pem 파일 전체 내용) | `-----BEGIN RSA PRIVATE KEY-----...` |
| `DB_URL` | 데이터베이스 URL | `jdbc:oracle:thin:@...` |
| `DB_USERNAME` | DB 사용자명 | `admin` |
| `DB_PASSWORD` | DB 비밀번호 | `yourdbpassword` |
| `JWT_SECRET` | JWT 시크릿 키 (Base64) | `c21hcnR3b3Jr...` |

### 3. EC2_PRIVATE_KEY 설정 방법
```bash
# macOS/Linux에서 키 파일 내용 복사
cat ~/Downloads/smartwork-key.pem | pbcopy

# 또는 출력 후 수동 복사
cat ~/Downloads/smartwork-key.pem
```

**전체 내용을 복사**해서 GitHub Secret에 붙여넣기 (BEGIN부터 END까지 모두)

---

## 🚀 수동 배포

### 1. 프로젝트 Clone
```bash
# EC2에서 실행
cd ~
git clone https://github.com/yourusername/smartwork.git
cd smartwork
```

### 2. 환경 변수 설정
```bash
# .env 파일 생성
nano .env
```

```bash
# .env 파일 내용
DOCKER_USERNAME=yourusername
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:oracle:thin:@[RDS_ENDPOINT]:1521:ORCL
DB_USERNAME=admin
DB_PASSWORD=yourdbpassword
JWT_SECRET=c21hcnR3b3JrLWp3dC1zZWNyZXQta2V5...
```

### 3. 배포 스크립트 실행
```bash
# 실행 권한 부여
chmod +x deploy.sh

# 전체 배포 실행
./deploy.sh deploy
```

### 4. 상태 확인
```bash
# 컨테이너 상태 확인
./deploy.sh status

# 로그 확인
./deploy.sh logs 100
```

### 5. 애플리케이션 접속
```
http://[EC2_PUBLIC_IP]:8080
http://[EC2_PUBLIC_IP]:8080/swagger-ui.html
```

---

## 🔄 자동 배포 (CI/CD)

### 1. GitHub Actions 활성화

코드를 `main` 브랜치에 push하면 자동으로 배포됩니다:

```bash
# 로컬에서 작업
git add .
git commit -m "Update application"
git push origin main
```

### 2. 배포 진행 확인
```
GitHub Repository → Actions 탭 → 최신 워크플로우 확인
```

### 3. 배포 과정
```
1. 코드 체크아웃
2. Java 21 설정
3. Gradle 빌드
4. Docker 이미지 빌드 및 푸시
5. EC2 SSH 접속
6. 기존 컨테이너 중지 및 제거
7. 최신 이미지 Pull
8. 새 컨테이너 실행
9. 헬스체크
```

**다운타임**: 약 10-30초 (컨테이너 교체 시간)

---

## 📊 모니터링 및 관리

### 1. 리소스 모니터링
```bash
# 실시간 리소스 사용량
htop

# Docker 컨테이너 리소스 사용량
docker stats smartwork-app

# 디스크 사용량
df -h

# 메모리 사용량
free -h
```

### 2. 로그 확인
```bash
# 실시간 로그 (최근 100줄)
./deploy.sh logs 100

# 전체 로그
docker logs smartwork-app

# 로그 저장
docker logs smartwork-app > ~/app.log 2>&1
```

### 3. 애플리케이션 상태 확인
```bash
# 헬스체크 엔드포인트
curl http://localhost:8080/actuator/health

# 정상 응답:
# {"status":"UP"}
```

### 4. 컨테이너 재시작
```bash
# 일반 재시작
./deploy.sh restart

# 이미지 업데이트 후 재시작
./deploy.sh deploy
```

### 5. 리소스 정리
```bash
# 사용하지 않는 이미지/컨테이너 정리
./deploy.sh cleanup

# 전체 시스템 정리 (신중하게!)
docker system prune -a
```

---

## 🔍 문제 해결

### 1. 메모리 부족 (OOMKilled)

**증상**:
```bash
docker ps -a
# STATUS: Exited (137)
```

**해결**:
```bash
# Swap 메모리 확인
free -h

# Swap이 없다면 추가 (위의 EC2 초기 설정 참고)

# JVM 메모리 줄이기
# docker-compose.yml에서 JAVA_OPTS 수정:
# JAVA_OPTS=-Xmx384m -Xms192m
```

### 2. 애플리케이션이 시작하지 않음

**확인**:
```bash
# 로그 확인
docker logs smartwork-app

# 환경 변수 확인
docker exec smartwork-app env | grep DB

# 데이터베이스 연결 테스트
docker exec smartwork-app wget --spider http://localhost:8080/actuator/health
```

**흔한 원인**:
- DB 연결 실패 (URL, 사용자명, 비밀번호 확인)
- 메모리 부족
- 포트 충돌

### 3. 포트 8080이 이미 사용 중

**확인**:
```bash
sudo netstat -tulpn | grep 8080
```

**해결**:
```bash
# 해당 프로세스 종료
sudo kill -9 [PID]

# 또는 다른 포트 사용
# docker-compose.yml에서 포트 변경: "9090:8080"
```

### 4. Docker Hub Push 실패

**증상**:
```
denied: requested access to the resource is denied
```

**해결**:
```bash
# Docker Hub 로그인 확인
docker login

# Repository가 존재하는지 확인
# https://hub.docker.com/ 에서 repository 생성
```

### 5. GitHub Actions 배포 실패

**확인**:
```
GitHub → Actions → 실패한 워크플로우 → 로그 확인
```

**흔한 원인**:
- GitHub Secrets 누락 또는 오류
- EC2 SSH 키 형식 오류
- EC2 보안 그룹에서 GitHub Actions IP 차단

**해결**:
```bash
# EC2 보안 그룹에서 SSH (22) 포트를 0.0.0.0/0으로 열기 (임시)
# 또는 GitHub Actions IP 대역 허용
```

### 6. 데이터베이스 연결 실패

**RDS 보안 그룹 확인**:
```
1. RDS → 데이터베이스 → smartwork-db → VPC 보안 그룹
2. 인바운드 규칙 → 편집
3. Oracle 포트 (1521) → EC2 보안 그룹 허용
```

---

## 💰 비용 절감 팁

### 1. 프리티어 한도
```
EC2 t2.micro: 월 750시간 (1개 인스턴스 = 무료)
RDS db.t3.micro: 월 750시간 (1개 인스턴스 = 무료)
EBS 스토리지: 30GB까지 무료
데이터 전송: 월 15GB까지 무료
```

### 2. 사용하지 않을 때 중지
```bash
# 인스턴스 중지 (요금 발생 안 함, EBS 스토리지는 소액 과금)
AWS Console → EC2 → 인스턴스 선택 → 인스턴스 중지

# 인스턴스 시작
AWS Console → EC2 → 인스턴스 선택 → 인스턴스 시작
```

**주의**: Elastic IP는 인스턴스가 중지되어 있을 때 요금이 부과됩니다!

### 3. CloudWatch 알람 설정
```
1. CloudWatch → 알람 → 알람 생성
2. EC2 → 인스턴스별 지표 → CPUUtilization
3. 임계값: > 80%
4. 알림 작업: SNS 주제 생성 (이메일 알림)
```

---

## 📝 요약

### 배포 플로우
```
로컬 개발 → Git Push (main) → GitHub Actions 실행
→ Docker 이미지 빌드 → Docker Hub 푸시
→ EC2 SSH 접속 → 컨테이너 교체 → 배포 완료
```

### 주요 명령어
```bash
# 배포
./deploy.sh deploy

# 재시작
./deploy.sh restart

# 로그 확인
./deploy.sh logs

# 상태 확인
./deploy.sh status

# 정리
./deploy.sh cleanup
```

### 접속 URL
```
애플리케이션: http://[EC2_IP]:8080
API 문서: http://[EC2_IP]:8080/swagger-ui.html
헬스체크: http://[EC2_IP]:8080/actuator/health
```

---

## 🎓 다음 단계

1. **도메인 연결**: Route 53으로 커스텀 도메인 설정
2. **HTTPS 적용**: Let's Encrypt + Nginx 리버스 프록시
3. **로드 밸런서**: Application Load Balancer (여러 인스턴스)
4. **Auto Scaling**: 트래픽에 따라 자동 확장
5. **CloudWatch Logs**: 중앙 로그 관리
6. **RDS Multi-AZ**: 데이터베이스 고가용성
7. **CloudFront**: CDN으로 성능 향상
8. **ECS/EKS**: 컨테이너 오케스트레이션

---

## 📚 참고 자료

- [AWS EC2 프리티어](https://aws.amazon.com/ko/free/)
- [Docker 공식 문서](https://docs.docker.com/)
- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [Spring Boot Docker](https://spring.io/guides/gs/spring-boot-docker/)

---

**배포 성공을 기원합니다! 🚀**
