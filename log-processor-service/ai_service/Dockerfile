FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY generated ./generated
COPY app.py .
ENV CH_HOST=clickhouse
ENV HTTP_PORT=8000
ENV GRPC_PORT=50051
EXPOSE 8000 50051
CMD ["python", "app.py"]
