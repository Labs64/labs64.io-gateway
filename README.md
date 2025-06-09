<p align="center"><img src="https://raw.githubusercontent.com/Labs64/.github/refs/heads/master/assets/labs64-io-ecosystem.png"></p>

# Labs64.IO :: API Gateway - A secure, high-performance entry point for managing and routing API traffic across Labs64.IO microservices

Key responsibilities of the API Gateway:

- Request Routing: Directs incoming HTTP/S requests to the appropriate backend service or RabbitMQ exchange/queue.
- Protocol Translation: Translates synchronous HTTP/S requests from clients into asynchronous messages for RabbitMQ, and vice-versa for responses.
- Authentication and Authorization: Enforces security policies before requests reach your internal services.
- Rate Limiting and Throttling: Protects your backend from abuse and ensures fair usage.
- Caching: Caches responses for frequently accessed data, reducing load on your backend.
- Request/Response Transformation: Modifies request or response bodies/headers to fit external API contracts.
- Load Balancing: Distributes requests across multiple instances of your services.
- Monitoring and Logging: Provides a central point for tracking API usage and performance.

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=Labs64/labs64.io-api-gateway&type=Date)](https://www.star-history.com/#Labs64/labs64.io-api-gateway&Date)
