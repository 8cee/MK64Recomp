FROM python:3.12-alpine

WORKDIR /app
COPY mk64_server.py /app/mk64_server.py

EXPOSE 6464/udp

USER nobody

ENTRYPOINT ["python3", "/app/mk64_server.py"]
CMD ["--host", "0.0.0.0", "--port", "6464"]
