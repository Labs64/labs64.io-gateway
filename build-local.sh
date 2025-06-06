## Toolset
# Local: k8s, kubectl, helm, docker, maven, git, java

# Local docker repository
# docker run -d -p 5005:5000 --name registry registry:2

docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" | grep "api-gateway" | awk '{print $2}' | xargs -r docker rmi -f

# Build project
mvn clean package

# Build docker images
docker build -t api-gateway:latest .
docker tag api-gateway:latest localhost:5005/api-gateway:latest
docker push localhost:5005/api-gateway:latest

docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" | grep "api-gateway"

# Start application
#mvn spring-boot:run -Dspring-boot.run.profiles=local
