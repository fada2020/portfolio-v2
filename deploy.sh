#!/bin/bash

###############################################################################
# SmartWork EC2 배포 스크립트
#
# 사용법:
#   ./deploy.sh [start|stop|restart|logs|status]
###############################################################################

set -e  # 에러 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 설정
CONTAINER_NAME="smartwork-app"
IMAGE_NAME="smartwork:latest"
DOCKER_USERNAME="${DOCKER_USERNAME:-your-dockerhub-username}"

# 함수: 로그 출력
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 함수: 컨테이너 상태 확인
check_container_status() {
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
            return 0  # 실행 중
        else
            return 1  # 중지됨
        fi
    else
        return 2  # 존재하지 않음
    fi
}

# 함수: 컨테이너 중지 및 제거
stop_container() {
    log_info "컨테이너 중지 중..."

    if check_container_status; then
        docker stop ${CONTAINER_NAME}
        log_success "컨테이너 중지 완료"
    else
        log_warning "실행 중인 컨테이너가 없습니다"
    fi

    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        docker rm ${CONTAINER_NAME}
        log_success "컨테이너 제거 완료"
    fi
}

# 함수: 컨테이너 시작
start_container() {
    log_info "컨테이너 시작 중..."

    # 환경 변수 로드 (.env 파일이 있는 경우)
    if [ -f .env ]; then
        log_info ".env 파일 로드 중..."
        export $(cat .env | grep -v '^#' | xargs)
    fi

    # Docker Compose 사용
    if [ -f docker-compose.yml ]; then
        docker-compose up -d
        log_success "Docker Compose로 컨테이너 시작 완료"
    else
        # docker run 직접 사용
        docker run -d \
            --name ${CONTAINER_NAME} \
            --restart unless-stopped \
            -p 8080:8080 \
            -e SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod} \
            -e DB_URL="${DB_URL}" \
            -e DB_USERNAME="${DB_USERNAME}" \
            -e DB_PASSWORD="${DB_PASSWORD}" \
            -e JWT_SECRET="${JWT_SECRET}" \
            -e TZ=Asia/Seoul \
            --memory="768m" \
            --memory-reservation="512m" \
            ${IMAGE_NAME}

        log_success "컨테이너 시작 완료"
    fi

    # 헬스체크 대기
    log_info "애플리케이션 시작 대기 중..."
    sleep 15

    # 헬스체크
    if docker exec ${CONTAINER_NAME} wget --spider -q http://localhost:8080/actuator/health 2>/dev/null; then
        log_success "애플리케이션이 정상적으로 시작되었습니다"
    else
        log_warning "헬스체크 실패 - 로그를 확인하세요"
    fi
}

# 함수: 이미지 Pull
pull_image() {
    log_info "최신 Docker 이미지 Pull 중..."

    if [ -n "${DOCKER_USERNAME}" ] && [ "${DOCKER_USERNAME}" != "your-dockerhub-username" ]; then
        docker pull ${DOCKER_USERNAME}/${IMAGE_NAME}
        docker tag ${DOCKER_USERNAME}/${IMAGE_NAME} ${IMAGE_NAME}
        log_success "이미지 Pull 완료"
    else
        log_warning "DOCKER_USERNAME이 설정되지 않았습니다. 로컬 이미지를 사용합니다."
    fi
}

# 함수: 로그 확인
show_logs() {
    if check_container_status; then
        if [ -n "$1" ]; then
            docker logs -f --tail "$1" ${CONTAINER_NAME}
        else
            docker logs -f --tail 100 ${CONTAINER_NAME}
        fi
    else
        log_error "실행 중인 컨테이너가 없습니다"
        exit 1
    fi
}

# 함수: 상태 확인
show_status() {
    log_info "=== 컨테이너 상태 ==="
    docker ps -a --filter name=${CONTAINER_NAME} --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

    echo ""
    log_info "=== 리소스 사용량 ==="
    docker stats --no-stream ${CONTAINER_NAME} 2>/dev/null || log_warning "실행 중인 컨테이너가 없습니다"

    echo ""
    log_info "=== 디스크 사용량 ==="
    docker system df
}

# 함수: 이전 이미지/컨테이너 정리
cleanup() {
    log_info "사용하지 않는 Docker 리소스 정리 중..."
    docker system prune -f
    log_success "정리 완료"
}

# 메인 로직
case "${1:-}" in
    start)
        start_container
        ;;
    stop)
        stop_container
        ;;
    restart)
        log_info "=== 컨테이너 재시작 ==="
        stop_container
        sleep 2
        pull_image
        start_container
        ;;
    pull)
        pull_image
        ;;
    logs)
        show_logs "${2:-100}"
        ;;
    status)
        show_status
        ;;
    cleanup)
        cleanup
        ;;
    deploy)
        log_info "=== 전체 배포 시작 ==="
        stop_container
        pull_image
        cleanup
        start_container
        show_status
        log_success "=== 배포 완료 ==="
        ;;
    *)
        echo "사용법: $0 {start|stop|restart|pull|logs|status|cleanup|deploy}"
        echo ""
        echo "명령어:"
        echo "  start    - 컨테이너 시작"
        echo "  stop     - 컨테이너 중지"
        echo "  restart  - 컨테이너 재시작"
        echo "  pull     - 최신 이미지 Pull"
        echo "  logs     - 로그 확인 (예: logs 50)"
        echo "  status   - 상태 확인"
        echo "  cleanup  - 사용하지 않는 리소스 정리"
        echo "  deploy   - 전체 배포 (stop → pull → cleanup → start)"
        exit 1
        ;;
esac

exit 0
