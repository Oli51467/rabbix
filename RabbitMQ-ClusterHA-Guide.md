# Docker 安装方法

安装依赖包

```
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
```

配置仓库

```
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
```

安装docker

```
sudo yum install docker-ce -y
```

设置docker自启动

```
sudo systemctl enable docker
```

启动docker服务

```
sudo systemctl start docker
```

查看docker版本

```
docker -v
```

运行hello-world容器

```
sudo docker run hello-world
```

# 启动单节点Rabbit MQ

```
docker run -d --rm --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

# 安装 Docker Compose

安装python2-pip

```
dnf install python2-pip
```

安装python3-pip

```
dnf install python3-pip
```

安装docker-compose

```
pip3 install docker-compose
```

查看版本

```
docker-compose version
```

# 使用 Docker Compose 启动3个 RabbitMQ 节点

```
vi docker-compose.yml
```

```yaml
version: "2.0"
services:
  rabbit1:
    image: rabbitmq:3-management
    hostname: rabbit1
    ports:
      - 5672:5672 #集群内部访问的端口
      - 15672:15672 #外部访问的端口
    environment:
      - RABBITMQ_DEFAULT_USER=guest #用户名
      - RABBITMQ_DEFAULT_PASS=guest #密码
      - RABBITMQ_ERLANG_COOKIE='imoocrabbitmq'

  rabbit2:
    image: rabbitmq:3-management
    hostname: rabbit2
    ports:
      - 5673:5672
    environment:
      - RABBITMQ_ERLANG_COOKIE='imoocrabbitmq'
    links:
      - rabbit1

  rabbit3:
    image: rabbitmq:3-management
    hostname: rabbit3
    ports:
      - 5674:5672
    environment:
      - RABBITMQ_ERLANG_COOKIE='imoocrabbitmq'
    links:
      - rabbit1
      - rabbit2

```

# 将3个 RabbitMQ 节点搭建为集群 

启动docker-compose，按照脚本启动集群

```
docker-compose up -d
```

进入2号节点

```
docker exec -it root_rabbit2_1 bash 
```

停止2号节点的rabbitmq

```
rabbitmqctl stop_app
```

配置2号节点，加入集群

```
rabbitmqctl join_cluster rabbit@rabbit1
```

启动2号节点的rabbitmq

```
rabbitmqctl start_app 
```

进入3号节点

```
docker exec -it root_rabbit3_1 bash 
```

停止3号节点的rabbitmq

```
rabbitmqctl stop_app
```

配置3号节点，加入集群

```
rabbitmqctl join_cluster rabbit@rabbit1
```

启动3号节点的rabbitmq

```
rabbitmqctl start_app 
```

# K8S编排脚本

```yaml
kind: Service
# 相当于负载均衡层
apiVersion: v1
# 元数据
metadata:
# 命名空间
  namespace: test-rabbitmq
  name: rabbitmq
  labels:
    app: rabbitmq
    type: LoadBalancer  
spec:
  type: NodePort
  ports:
   - name: http
     protocol: TCP
     port: 15672
     targetPort: 15672
     nodePort: 31672
   - name: amqp
     protocol: TCP
     port: 5672
     targetPort: 5672
     nodePort: 30672
  selector:
    app: rabbitmq
---
apiVersion: v1
# 用于注入配置文件
kind: ConfigMap
metadata:
  name: rabbitmq-config
  namespace: test-rabbitmq
data:
  enabled_plugins: |
      [rabbitmq_management,rabbitmq_peer_discovery_k8s].
  rabbitmq.conf: |
      cluster_formation.peer_discovery_backend  = rabbit_peer_discovery_k8s
      cluster_formation.k8s.host = kubernetes.default.svc.cluster.local
      cluster_formation.k8s.address_type = ip
      cluster_formation.node_cleanup.interval = 30
      cluster_formation.node_cleanup.only_log_warning = true
      cluster_partition_handling = autoheal
      loopback_users.guest = false
   
---
apiVersion: apps/v1beta1
kind: StatefulSet
metadata:
  name: rabbitmq
  namespace: test-rabbitmq
spec:
  serviceName: rabbitmq
  replicas: 3
  template:
    metadata:
      labels:
        app: rabbitmq
    spec:
      serviceAccountName: rabbitmq
      terminationGracePeriodSeconds: 10
      containers:        
      - name: rabbitmq
        image: rabbitmq:3-management
        volumeMounts:
          - name: config-volume
            mountPath: /etc/rabbitmq
        ports:
          - name: http
            protocol: TCP
            containerPort: 15672
          - name: amqp
            protocol: TCP
            containerPort: 5672
        livenessProbe:
          exec:
            command: ["rabbitmqctl", "status"]
          initialDelaySeconds: 60
          periodSeconds: 60
          timeoutSeconds: 10
        readinessProbe:
          exec:
            command: ["rabbitmqctl", "status"]
          initialDelaySeconds: 20
          periodSeconds: 60
          timeoutSeconds: 10
        imagePullPolicy: Always
        env:
          - name: MY_POD_IP
            valueFrom:
              fieldRef:
                fieldPath: status.podIP
          - name: RABBITMQ_USE_LONGNAME
            value: "true"
          - name: RABBITMQ_NODENAME
            value: "rabbit@$(MY_POD_IP)"
          - name: K8S_SERVICE_NAME
            value: "rabbitmq"
          - name: RABBITMQ_ERLANG_COOKIE
            value: "imoocrabbit" 
      volumes:
        - name: config-volume
          configMap:
            name: rabbitmq-config
            items:
            - key: rabbitmq.conf
              path: rabbitmq.conf
            - key: enabled_plugins
              path: enabled_plugins
```
