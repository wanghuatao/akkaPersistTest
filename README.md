1.git clone https://github.com/wanghuatao/akkaPersistTest.git
2. intellij idea import this gradle project
3. 
  docker run  --restart=always  --name cassandra -d -p 9042:9042 \
  -v /data/cadata:/var/lib/cassandra \
  -e CASSANDRA_CLUSTER_NAME=mycluster  \
  -p 7000:7000 cassandra:3.11
  
  
