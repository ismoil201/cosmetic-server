# PRODUCTION DEPLOYMENT CHECKLIST

## ⚠️ CRITICAL: BEFORE DEPLOYING TO PRODUCTION

### 1. SECURITY CONFIGURATION

#### 1.1 Generate Strong JWT Secret
```bash
# Generate a cryptographically secure random secret (512 bits recommended)
openssl rand -base64 64
```
- Set this value as `JWT_SECRET` environment variable
- **NEVER** commit this to version control
- Rotate this secret periodically (requires re-login for all users)

#### 1.2 Secure Cloudflare R2 Credentials
- Create new R2 API tokens with minimal required permissions
- Set environment variables:
  - `R2_ACCOUNT_ID`
  - `R2_BUCKET_NAME`
  - `R2_PUBLIC_URL`
  - `R2_ACCESS_KEY`
  - `R2_SECRET_KEY`
- **IMMEDIATELY REVOKE** the exposed credentials in application.yml history

#### 1.3 Database Credentials
- Use strong passwords (16+ characters, random)
- Set `DB_URL`, `DB_USER`, `DB_PASSWORD` as environment variables
- Enable SSL/TLS for database connections (add `useSSL=true&requireSSL=true` to DB_URL)

#### 1.4 CORS Configuration
- Update `CORS_ALLOWED_ORIGINS` with actual frontend domains
- **NEVER** use wildcards or localhost in production
- Example: `https://yourdomain.com,https://www.yourdomain.com`

### 2. DATABASE MIGRATION

#### 2.1 Schema Management
- **CRITICAL:** Change `spring.jpa.hibernate.ddl-auto=validate` (already done)
- Install Flyway or Liquibase for schema versioning
- Create baseline migration from current schema
- Test migrations on staging before production

#### 2.2 Database Indexes
Ensure these indexes exist (should be auto-created by JPA @Index, but verify):
```sql
-- Products table
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_brand ON products(brand);
CREATE INDEX idx_products_sold_count ON products(sold_count);
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_products_search_text ON products(search_text);
CREATE INDEX idx_products_active_sold_view ON products(active, sold_count, view_count);

-- Product variants table
CREATE INDEX idx_variant_product ON product_variants(product_id);
CREATE INDEX idx_variant_active ON product_variants(active);
```

### 3. INFRASTRUCTURE SETUP

#### 3.1 Database Connection Pool
- HikariCP configured with:
  - `maximum-pool-size: 20` (adjust based on load testing)
  - Connection leak detection enabled
  - Connection validation enabled

#### 3.2 Redis Setup
- Deploy Redis with persistence (RDB + AOF)
- Configure max memory policy: `maxmemory-policy allkeys-lru`
- Enable password authentication
- Set `REDIS_PASSWORD` environment variable

#### 3.3 Application Server
Minimum requirements:
- **CPU:** 2 cores (4+ recommended for production)
- **Memory:** 2GB RAM minimum (4GB+ recommended)
- **JVM Options:** 
  ```bash
  -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
  ```

### 4. MONITORING & OBSERVABILITY

#### 4.1 Health Checks
- **Liveness:** `GET /actuator/health/liveness`
- **Readiness:** `GET /actuator/health/readiness`
- Configure load balancer/Kubernetes to use these endpoints

#### 4.2 Metrics
- Prometheus metrics available at `/actuator/prometheus`
- Set up Grafana dashboards for:
  - Request rate, latency, error rate
  - Database connection pool usage
  - Redis hit/miss rate
  - JVM memory/GC metrics

#### 4.3 Logging
- Use structured JSON logging (uncomment in application.yml)
- Ship logs to centralized logging (ELK, CloudWatch, etc.)
- Set up alerts for:
  - ERROR log rate > threshold
  - Database connection pool exhaustion
  - Redis connection failures

### 5. OPTIONAL BUT RECOMMENDED

#### 5.1 Rate Limiting
Add rate limiting for public endpoints:
```java
// Using Bucket4j or Spring Cloud Gateway
@RateLimiter(name = "api", fallbackMethod = "rateLimitFallback")
```

#### 5.2 API Gateway
Consider using an API gateway (Kong, AWS API Gateway) for:
- Rate limiting
- DDoS protection
- Request/response transformation
- Centralized authentication

#### 5.3 CDN for Static Assets
- Use Cloudflare CDN in front of R2 public URL
- Enable cache headers on images

### 6. PRE-LAUNCH TESTING

#### 6.1 Load Testing
- Use JMeter, Gatling, or k6
- Test scenarios:
  - Homepage load: 100+ concurrent users
  - Product search: 50+ queries/sec
  - Order creation: 10+ orders/sec
- Monitor:
  - Response times (p50, p95, p99)
  - Error rates
  - Database connection pool usage
  - Redis hit rate

#### 6.2 Security Testing
- Run OWASP ZAP or Burp Suite
- Test for:
  - SQL injection
  - XSS vulnerabilities
  - Authentication bypass
  - Authorization bypass (IDOR)
  - Rate limiting effectiveness

#### 6.3 Failure Testing
- Kill Redis → verify app continues (without cache)
- Kill database → verify graceful error handling
- Overload server → verify thread pool limits work

### 7. DEPLOYMENT PROCESS

#### 7.1 Environment Variables Checklist
Before starting the application, ensure ALL these are set:
- [ ] `DB_URL`
- [ ] `DB_USER`
- [ ] `DB_PASSWORD`
- [ ] `REDIS_HOST`
- [ ] `REDIS_PORT`
- [ ] `REDIS_PASSWORD`
- [ ] `JWT_SECRET` (256+ bits)
- [ ] `R2_ACCOUNT_ID`
- [ ] `R2_BUCKET_NAME`
- [ ] `R2_PUBLIC_URL`
- [ ] `R2_ACCESS_KEY`
- [ ] `R2_SECRET_KEY`
- [ ] `CORS_ALLOWED_ORIGINS`
- [ ] `SWAGGER_ENABLED=false`

#### 7.2 Startup Verification
After deployment:
1. Check health: `curl https://your-api.com/actuator/health`
2. Verify Redis connection in logs
3. Verify database connection in logs
4. Test login endpoint
5. Test public homepage endpoint
6. Monitor error logs for 15 minutes

### 8. POST-LAUNCH MONITORING

#### 8.1 First 24 Hours
Monitor closely:
- Error rate
- Response times
- Database connection pool usage
- Redis memory usage
- Disk space (logs)

#### 8.2 Set Up Alerts
- Error rate > 1% for 5 minutes
- Response time p95 > 2 seconds
- Database connections > 80% pool size
- Redis memory > 80%
- Disk space < 20%

### 9. INCIDENT RESPONSE

#### 9.1 Rollback Plan
- Keep previous version deployable
- Database migrations must be backward-compatible
- Test rollback procedure in staging

#### 9.2 Emergency Contacts
- Database admin
- DevOps/infrastructure team
- Security team contact

### 10. KNOWN LIMITATIONS & FUTURE WORK

#### Still Missing (Medium Priority):
1. **Rate Limiting** - Add per-IP/per-user limits
2. **Input Validation** - Add @Valid annotations to all controllers
3. **Custom Exception Types** - Replace generic RuntimeException
4. **Request ID Tracing** - Add correlation IDs for debugging
5. **Async Event Processing** - Move event tracking to message queue
6. **Redis Fallback** - Graceful degradation when cache unavailable

#### Performance Optimizations (Low Priority):
1. **Elasticsearch** - Replace fuzzy search for better performance at scale
2. **Read Replicas** - Separate read/write database connections
3. **CDN Integration** - Cache API responses at edge
4. **GraphQL** - Consider for mobile clients to reduce overfetching

---

## EMERGENCY PROCEDURES

### Application Won't Start

1. Check logs: `tail -f logs/application.log`
2. Verify all environment variables are set: `env | grep -E '(DB_|REDIS_|JWT_|R2_)'`
3. Test database connection: `mysql -h $DB_HOST -u $DB_USER -p`
4. Test Redis connection: `redis-cli -h $REDIS_HOST ping`

### High Memory Usage

1. Check JVM metrics: `curl localhost:8080/actuator/metrics/jvm.memory.used`
2. Trigger heap dump: `jmap -dump:format=b,file=heap.bin <pid>`
3. Analyze with VisualVM or Eclipse MAT

### High CPU Usage

1. Check active threads: `curl localhost:8080/actuator/metrics/jvm.threads.live`
2. Thread dump: `jstack <pid> > threads.txt`
3. Check for runaway queries in database

### Database Connection Pool Exhausted

1. Check active connections: `curl localhost:8080/actuator/metrics/hikaricp.connections.active`
2. Check for long-running queries: `SHOW FULL PROCESSLIST;` in MySQL
3. Restart application as last resort

---

## SECURITY INCIDENT RESPONSE

### JWT Secret Compromised

1. Generate new secret immediately
2. Deploy with new `JWT_SECRET`
3. All users will need to re-login
4. Invalidate all sessions (if session tracking implemented)
5. Audit logs for suspicious activity

### R2 Credentials Leaked

1. Revoke compromised credentials in Cloudflare dashboard
2. Generate new credentials
3. Deploy with new environment variables
4. Audit R2 bucket for unauthorized access/modifications
5. Check billing for unexpected usage

---

## CONTACT & SUPPORT

- **Production Issues:** [Your on-call contact]
- **Security Incidents:** [Security team contact]
- **Database Issues:** [DBA contact]

---

**Last Updated:** [Current Date]
**Reviewed By:** [Your Name]
