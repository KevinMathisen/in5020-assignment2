FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y \
    build-essential \
    wget \
    gcc \
    make \
    && rm -rf /var/lib/apt/lists/*

# Crete spread user
RUN groupadd -r spread && useradd -r -g spread spread

# Copy files to image
WORKDIR /app
COPY . /app

# Set working dir to daemon
WORKDIR /app/spread-src-4.0.0/daemon

# Make spread user owner of files
RUN chown -R spread:spread /app

EXPOSE 4803

USER spread

CMD ["./spread", "-l", "y", "-n", "spreadserver", "-c", "../../spread.conf"]
