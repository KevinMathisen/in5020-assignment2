# Use Ubuntu as the base image
FROM ubuntu:20.04

# Set environment variables to avoid interactive prompts
ENV DEBIAN_FRONTEND=noninteractive

# Install necessary dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    wget \
    gcc \
    make \
    && rm -rf /var/lib/apt/lists/*

# Create spread user and group
RUN groupadd -r spread && useradd -r -g spread spread

# Set the working directory to /app
WORKDIR /app

# Copy the entire project into the container
COPY . /app

# Set the working directory to the daemon directory
WORKDIR /app/spread-src-4.0.0/daemon

# Change ownership of the files to the spread user
RUN chown -R spread:spread /app

# Expose any necessary ports here, if needed
EXPOSE 4803

# Switch to the spread user
USER spread

# Command to run the Spread server
CMD ["./spread", "-l", "y", "-n", "spreadserver", "-c", "../../spread.conf"]
