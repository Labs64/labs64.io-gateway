FROM python:3-alpine

# Define build arguments
ARG USER_ID=1064
ARG GROUP_ID=1064
ARG USER_NAME=l64user
ARG GROUP_NAME=l64group

# Create group and user with the specified IDs
RUN addgroup -g ${GROUP_ID} ${GROUP_NAME} && \
    adduser -D -u ${USER_ID} -G ${GROUP_NAME} ${USER_NAME}

# Set working directory
WORKDIR /home/${USER_NAME}

# Copy the requirements file into the container at the working directory
COPY --chown=${USER_NAME}:${GROUP_NAME} requirements.txt .

# Copy the main FastAPI application file
COPY --chown=${USER_NAME}:${GROUP_NAME} traefik_authproxy.py .

# Install any needed packages specified in requirements.txt
RUN python -m pip install --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Make port 8081 available outside this container
EXPOSE 8081

# Set user context
USER ${USER_NAME}

# Run the FastAPI application using Uvicorn
CMD ["uvicorn", "traefik_authproxy:app", "--host", "0.0.0.0", "--port", "8081"]
