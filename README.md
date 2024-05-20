# 与你相伴（鱼皮-伙伴匹配系统）



## 需求分析

1、给用户添加各式各样的标签，比如学习的方向，性别，爱好。

2、允许用户根据标签搜索

​	Redis缓存

3、组队

​	创建队伍

​	加入队伍

​	根据标签查询队伍

​	邀请其他人

4、允许用户修改标签

5、推荐

​	 相似度匹配算法、本地实时计算



**标签列：**
性别：

方向：java、c++...

目标：成为计算机大牛、进大厂、能独立完成项目的开发、考研、考公

段位：

身份：学生、职业

状态：开心、悲伤、愤怒、卷



## 技术栈

**前端**

1、vue 3

2、vant UI （移动端组件库）相当于element

3、vite（打包工具）

4、nginx部署

**后端**

1、jdk1.8   springBoot  springMVC myBatis Mybaits-plus

3、Mysql数据库   Redis缓存

5、Swagger   Knife4j 接口文档



## 数据库设计

**标签表设计**

```sql
id bigInt  comment ‘主键‘

tagName varchar(256) not null  unique  comment'标签名'

userId bigInt comment'用户'

parentId BigInt comment'父标签id'

isParent tinyInt comment'是否是父标签'

createTime datetime default current_timestamp  comment'创建时间'

updateTime datetime default current_timestamp comment'更新时间'

idDelete tinyInt defualt 0 comment'是否删除' 
```

**添加标签表索引**

唯一索引：tagName	

```sql
create unique index uniIndex_tagName
    on beihong.tag (tagName)
    comment '唯一索引';
```

常规索引：userId


```sql
create index index_userId
    on tag (userId)
    comment '用户id索引';
```

**用户表**

添加标签字段，存储标签列表

alter table user add column tags vachar(1024) null comment'标签列表'



##**标签搜索用户 接口开发**

1、基于sql查询

2、通过全查之后对缓存数据操作，基于内存查询

​	Gson的使用：

​		对集合，官网给的操作是

​		

```java
Set<String> jsonSet = new Gson().fromJson(jsonString,new TypeToken<Set<String>>()			     {}.getType();
```

​       具体还得去了解具体如何实现，为何不直接用

```
Set<String>.class
```



​	并行流parallelStream（）的弊端：

​		可能会被占用资源影响进程，具体可看文档了解

​	

**推荐页**

​	在线推荐、离线推荐

数据查询慢？

**数据查询返回到页面速度优化**

1、缓存

分布式

redis、memcached、Etcd（to study）

---------------

caffeine	（java内存缓存、号称缓存之王）：to study

本地缓存java内存（java 集合）

Google Guava

## 用户信息缓存

Redis、Jedis、Lettuce、Redisson

项目采用redis缓存

不能无限缓存，缓存容器是有限的

1、根据用户id作为键缓存用户信息

2、查询缓存是否有信息，有则直接取出返回给前端、没有则直接查库

3、从数据库查到数据之后然后存入缓存，并返回给前端

### 缓存预热

**定时任务实现**

Spring Scheduler（spring boot 默认整合了） 

Quartz（独立于 Spring 存在的定时任务框架）

XXL-Job 之类的分布式任务调度平台（界面 + sdk）

第一种方式：

1. 主类开启 @EnableScheduling
2. 给要定时执行的方法添加 @Scheduling 注解，指定 cron 表达式或者执行频率



不要去背 cron 表达式！！！！！

- https://cron.qqe2.com/
- https://www.matools.com/crontab/

**缓存雪崩 、缓存穿透、缓存击穿**

这几类问题都是针对于有大量请求直接打进数据库，导致数据库流量陡增，发生宕机拒绝访问请求等情况

雪崩：缓存中有大量的key突然找不到了，导致需要到数据库查表，此时会有大量流量打进数据库

穿透：请求的key是无效的不存在的，这时会直接请求数据库

击穿：一个热点key突然失效了，也会导致数据库流量突增

数据库 乐观锁 悲观锁 自旋锁

## 分布式 锁

利用redession实现分布式锁

引入redission 3.28.0 坐标

```xml
<dependency>
   <groupId>org.redisson</groupId>
   <artifactId>redisson</artifactId>
   <version>3.28.0</version>
</dependency>  
```



配置类RedissonConfig

```java
  		// 1. 创建配置
        Config config = new Config();
        String serverAddress = String.format("redis://%s:%s", host, port);
        config.useSingleServer().setAddress(serverAddress).setDatabase(3);

        //2. 创建Redisson客户端
        RedissonClient redisson = Redisson.create(config);
        return redisson;
```



实现分布式锁

**步骤**

1、尝试获取锁 --> 执行方法     no则等待

2、执行之后释放锁

其中包括，锁过期时间，等待时间，方法还没执行完锁续期，操作是否原子的问题

```java
 RLock lock = redisson.getLock("accompanying:precachejob:docache:lock");

    try {
        //尝试获取锁 等待or获取 debug模式下线程会直接判定为宕机，无法认定为等待，所以不会续期
        if (lock.tryLock(0,-1,TimeUnit.MICROSECONDS)) {
            log.info("currentThreadId:{}",Thread.currentThread().getId());
            //构造键
            String keyString = String.format("accompanying:user:recommend:%s", id);
            ValueOperations valueOperations = redisTemplate.opsForValue();
            //没找到信息 查库
            IPage<User> page = new Page<>(1,20);
            List<User> usersList = userService.list(page);

            log.info("cache successful---------------------------------------");

            //存入缓存
            try {
                valueOperations.set(keyString,usersList,30000, TimeUnit.MICROSECONDS);
            } catch (Exception e) {
                log.error("redis set key error:{}",e);
            }
        }
    } catch (InterruptedException e) {
        log.error("cache set error:{}",e.getMessage());
    } finally {
        //只能释放自己的锁
        if(lock.isHeldByCurrentThread()){
            log.info("currentThreadId:{}",Thread.currentThread().getId());
            lock.unlock();
        }
    }
}
```

### 注意事项

1. 用完锁要释放（腾地方）√

2. **锁一定要加过期时间 √**

3. 如果方法执行时间过长，锁提前过期了？

   问题：

   1. 连锁效应：释放掉别人的锁
   2. 这样还是会存在多个方法同时执行的情况

​	解决方案：续期

```java
boolean end = false;

new Thread(() -> {
    if (!end)}{
    续期
})

end = true;

```

4. 释放锁的时候，有可能先判断出是自己的锁，但这时锁过期了，最后还是释放了别人的锁

   ```java
   // 原子操作
   if(get lock == A) {
       // set lock B
       del lock
   }
   ```

   Redis + lua 脚本实现

5. Redis 如果是集群（而不是只有一个 Redis），如果分布式锁的数据不同步怎么办？

https://blog.csdn.net/feiying0canglang/article/details/113258494

## 组队功能

p0

自增：方便 怕爬虫

防爬虫：UUID    机器校验

### 需求分析

队伍唯一标识id、队伍名称、队伍隐私权限（是否公开）、队伍最大人数、过期时间、队伍情况描述

包括数据库老三样，创建时间、更新时间、是否删除

## 系统设计

**数据传输对象 dto**

### 接口设计

#### 1、**新增接口**

1. 请求参数是否为空？
2. 是否登录，未登录不允许创建
3. 校验信息
   1. 队伍人数 > 1 且 <= 20
   2. 队伍标题 <= 20
   3. 描述 <= 512
   4. status 是否公开（int）不传默认为 0（公开）
   5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
   6. 超时时间 > 当前时间
   7. 校验用户最多创建 5 个队伍
4. 插入队伍信息到队伍表
5. 插入用户  => 队伍关系到关系表

#### 2、查询队伍列表

​	1、查询请求参数是否为空

​	2、根据各个字段条件查询数据库

​	3、关联查询创建人

​	4、过期的队伍不展示

​	5、关键字查询 到描述信息 队伍名称

​	6、状态查询 只有管理员才能查看加密和私密的队伍

​	7、关联查询已加入队伍的用户信息（比较耗费性能，推荐用mybatis xml配置的方式自行实现）

返回对象的封装xxxVO

**获取我创建的队伍**

访问路径：/team/myTeam

请求参数：teamQuery

根据队伍的createUser查询

![image-20240515171216634](C:\Users\14136\AppData\Roaming\Typora\typora-user-images\image-20240515171216634.png)

**获取我加入的队伍**

访问路径：/team/myJoin

请求参数：teamQuery

复用之前写过的查询队伍的接口，增加通过id列表查询



**查询我已加入的队伍用户人数**

根据teamIdList查询UserTeam表，

然后按teamid分组，每个组下的list长度就是人数。

#### 3、队伍更新

可更新字段：id、名称、描述、状态、密码、最大人数、过期时间

新值与老值一致，不进行修改，提高数据没有发生变化时的响应速度

如何实现？

#### 4、加入队伍

人数限制、过期时间、已加入过的队伍、加密队伍条件、私密队伍

1、已经满员的队伍不能加入 一个人限制最多加5只队伍

2、过期的队伍不能加入

3、重复的队伍不能加入，自己的队伍不能加入

4、加入加密队伍需要校验密码

5、不能加入私密队伍

6、加入队伍更新队伍_用户关系表

**注意可能出现并发加入队伍异常 **todo

对加入队伍的代码进行锁，但是普通的锁只能锁单机部署的线程，如果是分布式的就需要采用分布式锁，在之前缓存预热有使用到过。

todo 由于直接锁会影响不同的用户之前的执行效率，比如不同的用户加入队伍，就不需要对用户重复加入队伍进行锁。

队伍人数校验时，不同的队伍之间不应该锁住，应该让他们同时拿到资源执行。

tostudy：规则引擎

#### 5、退出队伍

请求参数：队伍id

1、参数校验

2、检查队伍是否存在 （需要查队伍表）

3、本人是否已加入队伍 （需要查队伍表）

4、队伍只剩一人，解散队伍 （更新队伍表 队伍_用户关系表）

5、队长退出，队长位置就给最早加入队伍的那个人


> 为了找出除了自己的最早加入的用户

```sql
select * from user_team where teamId = ? order by id asc limit 1,1;
```

>这里涉及查表，只需要取前两条数据，通过加入时间降序排序，去前两条数据

6、其它正常退出

>删除队伍_用户关系表中该用户的关联信息

#### 6、队长解散队伍

1、校验参数

2、队伍要存在

3、不是队长不能删除

4、删除队伍后，队伍关系也要删除

5、解散删除队伍

**更新操作加入事务注解！**

##**随机匹配**

接口地址：/user/match

接收参数：long num(显示用户个数) 

用户个数限定在20个

匹配一个还是多个？

> 多个

怎么匹配？	根据什么匹配？

> 根据标签进行匹配，还可以根据相同队伍进行匹配

本质上都是通过标签进行匹配

数据库查询尽量只查需要的字段

> 查询id tags

> tags转为List 遍历userList 利用算法计算相似度存入集合
>
> 相似度作为value id作为key存入pair中，然后依次将pair存入集合 
>
> 对集合进行排序根据value相似度值，同时限制容量num
>
> 重新根据筛选好的id查询数据库，然后排好序，返回list

编辑距离算法

余弦相似度算法









### 数据库表设计：

id 主键 bigint 

name 队伍名称

description 描述

maxNum 最大人数

expireTime 过期时间

userId 用户id

status 0-公开 1-私人 2-加密

password 密码

createTime 创建时间

updateTime 更新时间

isDelete 是否删除

```sql
create table team
(
    id          bigint auto_increment comment 'id' primary key,
    name        varchar(256)                       not null comment '队伍名称',
    description varchar(1024)                      null comment '队伍描述',
    Password    varchar(512)                       null comment '密码',
    maxNum      int      default 1                 not null comment '最大人数',
    expireTime  datetime                           null comment '创建时间',
    userId      bigint comment '用户id',
    status      tinyInt  default 0                 not null comment ' 0 - 公开，1 - 私有，2 		- 加密',
    createTime  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP 			comment '更新时间',
    isDelete    tinyint  default 0                 null comment '是否删除'
)comment '队伍表';

```



用户-队伍表

id 主键 bigint 

userId 用户id

teamId 队伍id

joinTime 加入时间

createTime 创建时间

updateTime 更新时间

isDelete 是否删除

```sql
create table user_team
(
    id          bigint auto_increment comment 'id' primary key,
    userId      bigint comment '用户id',
    teamId      bigint comment '队伍id',
  	joinTime    datetime                           null comment '加入时间',
    createTime  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP 			comment '更新时间',
    isDelete    tinyint  default 0                 null comment '是否删除'
)comment '用户-队伍';
```

### 前端设计

#### 添加队伍页

vite指定启动端口

"dev" ："vite --port 3001"，

1、队伍页

新增按钮 ：添加队伍 点击之后跳转到TeamAdd页面

2、TeamAdd页

与后端接受字段信息，对应创建集合，添加对应的表单项，响应式绑定字段



**上线**

区分开发环境和线上环境

nginx运行后 到 usr/local/nginx.conf下了	



ps -ef | grep 'nginx'



**创建工作目录**

下载nginx  命令：			

```bash
curl -o nginx-1.26.0.tar.gz https://nginx.org/download/nginx-1.26.0.tar.gz
```

```bash
 tar -zxvf nginx-1.26.0.tar.gz
```

 ./configure

 yum install openssl openssl-devel -y

./configure --with-http_ssl_module --with-http_v2_module --with-stream

```bash
make
```





sudo vi /etc/yum.repos.d/nginx.repo



1. **Nginx 主程序文件**

   - ```
     /usr/sbin/nginx
     ```

     - Nginx 主程序执行文件。

2. **配置文件**

   - ```
     /etc/nginx/
     ```

     - Nginx 配置文件目录。

   - ```
     /etc/nginx/nginx.conf
     ```

     - 主配置文件。

   - ```
     /etc/nginx/conf.d/
     ```

     - 额外的配置文件，可以在这里添加自定义的配置文件，通常以 `.conf` 结尾。

   - ```
     /etc/nginx/sites-available/
     ```

      和 

     ```
     /etc/nginx/sites-enabled/
     ```

     （可能需要手动创建）

     - 这些目录可以用于站点配置文件的管理，但在默认安装中不会自动创建。

3. **日志文件**

   - ```
     /var/log/nginx/
     ```

     - Nginx 日志文件目录。
     - `access.log`：访问日志。
     - `error.log`：错误日志。

4. **网页根目录**

   - ```
     /usr/share/nginx/html
     ```

     - 默认的网页根目录。Nginx 默认的欢迎页面和其他静态网页文件存放在此。

5. **缓存和临时文件**





**环境变量**

```bash
/etc/profile
export PATH=$PATH:/usr/local/nginx/sbin
source /etc/profile 重新激活
location ^~ /api/ {
	proxy_pass http://127.0.0.1:8080/api/;
add_header 'Access-Control-Allow-Origin' $http_origin;
add_header 'Access-Control-Allow-Credentials' 'true';
if($request_method = 'OPTION'){
	add_header 'Access-Control-Allow-Origin' $http_origin;
	add_header 'Access-Control-Allow-Credentials' 'true';
}



}
```



netstat -ntlp 端口查看

chmod a+x jar包

nohup 运行命令 &  后台运行

注意访问nginx的权限 user root

yum install -y java-1.8.0-openjdk*



***打包之后出现的问题***

```url
http://localhost:3000/user/login?redirect=http://localhost:3000/
```

```url
http://localhost:3000/user/login?redirect=http://localhost:3000/
```

前端：vercel  

vercel.com

后端：微信云托管 按需使用 按量计费

tostudy 容器

**Docker部署**

安装docker 或者宝塔安装
