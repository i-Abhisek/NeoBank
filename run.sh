#!/bin/bash

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$BASE_DIR/logs"
PID_DIR="$BASE_DIR/pids"

mkdir -p "$LOG_DIR"
mkdir -p "$PID_DIR"

# Startup order
SERVICES=(
    "discovery-service"
    "api-gateway"
    "account-service"
    "transaction-service"
    "payment-service"
    "fraud-detection-service"
    "notification-service"
)

start_service() {
    SERVICE=$1

    echo "Starting $SERVICE..."

    cd "$BASE_DIR/$SERVICE" || exit 1

    # Check existing PID
    if [ -f "$PID_DIR/$SERVICE.pid" ]; then
        OLD_PID=$(cat "$PID_DIR/$SERVICE.pid")

        if kill -0 "$OLD_PID" 2>/dev/null; then
            echo "$SERVICE is already running (PID: $OLD_PID)"
            return 0
        fi

        rm -f "$PID_DIR/$SERVICE.pid"
    fi

    nohup ./mvnw spring-boot:run \
        > "$LOG_DIR/$SERVICE.log" 2>&1 &

    PID=$!

    echo "$PID" > "$PID_DIR/$SERVICE.pid"

    echo "$SERVICE started (PID: $PID)"
}

stop_service() {
    SERVICE=$1

    if [ ! -f "$PID_DIR/$SERVICE.pid" ]; then
        echo "$SERVICE is not running"
        return
    fi

    PID=$(cat "$PID_DIR/$SERVICE.pid")

    if kill -0 "$PID" 2>/dev/null; then
        echo "Stopping $SERVICE (PID: $PID)..."
        kill "$PID"
    else
        echo "$SERVICE is already stopped"
    fi

    rm -f "$PID_DIR/$SERVICE.pid"
}

status_service() {
    SERVICE=$1

    if [ -f "$PID_DIR/$SERVICE.pid" ]; then
        PID=$(cat "$PID_DIR/$SERVICE.pid")

        if kill -0 "$PID" 2>/dev/null; then
            echo "RUNNING : $SERVICE (PID: $PID)"
            return
        fi
    fi

    echo "STOPPED : $SERVICE"
}

wait_for_eureka() {

    echo ""
    echo "Waiting for Eureka Server..."
    echo ""

    MAX_ATTEMPTS=30
    ATTEMPT=1

    while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do

        if curl -s http://localhost:8761/ > /dev/null 2>&1; then
            echo "Eureka Server is UP."
            return 0
        fi

        echo "Waiting for Eureka... ($ATTEMPT/$MAX_ATTEMPTS)"

        sleep 2

        ATTEMPT=$((ATTEMPT + 1))
    done

    echo ""
    echo "ERROR: Eureka Server did not start."
    echo "Check:"
    echo "$LOG_DIR/discovery-service.log"

    return 1
}

case "$1" in

    start|"")
        echo "======================================"
        echo " Starting Banking System"
        echo "======================================"
        echo ""

        # ======================================
        # 1. Start Discovery Service
        # ======================================

        start_service "discovery-service"

        # Wait until Eureka is actually available
        if ! wait_for_eureka; then
            exit 1
        fi

        echo ""

        # ======================================
        # 2. Start API Gateway
        # ======================================

        start_service "api-gateway"

        echo ""
        echo "Waiting for API Gateway..."
        sleep 5

        # ======================================
        # 3. Start Business Services
        # ======================================

        echo ""
        echo "Starting business services..."
        echo ""

        start_service "account-service"
        start_service "transaction-service"
        start_service "payment-service"
        start_service "fraud-detection-service"
        start_service "notification-service"

        echo ""
        echo "======================================"
        echo " Banking System Started"
        echo "======================================"
        echo ""

        echo "Eureka Dashboard:"
        echo "http://localhost:8761"

        echo ""
        echo "API Gateway:"
        echo "http://localhost:8080"

        echo ""
        echo "Services:"
        echo "Account Service       : 8081"
        echo "Transaction Service   : 8082"
        echo "Payment Service       : 8083"
        echo "Fraud Detection       : 8084"
        echo "Notification Service  : 8085"

        echo ""
        echo "Logs:"
        echo "$LOG_DIR"

        echo ""
        ;;

    stop)
        echo "======================================"
        echo " Stopping Banking System"
        echo "======================================"
        echo ""

        # Stop in reverse dependency order

        stop_service "notification-service"
        stop_service "fraud-detection-service"
        stop_service "payment-service"
        stop_service "transaction-service"
        stop_service "account-service"

        # Gateway after business services
        stop_service "api-gateway"

        # Eureka last
        stop_service "discovery-service"

        echo ""
        echo "======================================"
        echo " All services stopped"
        echo "======================================"
        ;;

    restart)
        "$0" stop

        echo ""
        echo "Waiting before restart..."
        sleep 3

        "$0" start
        ;;

    status)
        echo "======================================"
        echo " Banking System Status"
        echo "======================================"
        echo ""

        for SERVICE in "${SERVICES[@]}"; do
            status_service "$SERVICE"
        done

        echo ""
        ;;

    logs)
        SERVICE=$2

        if [ -z "$SERVICE" ]; then

            echo "Usage:"
            echo "./run.sh logs <service-name>"
            echo ""

            echo "Available services:"

            for SERVICE_NAME in "${SERVICES[@]}"; do
                echo "  $SERVICE_NAME"
            done

            exit 1
        fi

        if [ ! -f "$LOG_DIR/$SERVICE.log" ]; then
            echo "Log file not found: $SERVICE.log"
            exit 1
        fi

        echo "======================================"
        echo " Logs: $SERVICE"
        echo "======================================"
        echo ""
        echo "Press Ctrl+C to exit."
        echo ""

        tail -f "$LOG_DIR/$SERVICE.log"
        ;;

    *)
        echo "Usage:"
        echo ""
        echo "  ./run.sh start"
        echo "  ./run.sh stop"
        echo "  ./run.sh restart"
        echo "  ./run.sh status"
        echo "  ./run.sh logs <service-name>"
        echo ""
        echo "Available services:"

        for SERVICE in "${SERVICES[@]}"; do
            echo "  $SERVICE"
        done
        ;;

esac