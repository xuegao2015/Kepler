### 文档持续更新中。。。

* 参与
	* [源码构建](#源码构建)
* 框架设计
	* [调用链信息](#调用链信息)
	* [暴露服务](#暴露服务)
	* [客户端调用](#客户端调用)
	* [序列化策略](#序列化策略)
	* [各种Invoker](#各种Invoker)
		* [ActualInvoker](#ActualInvoker)
		* [AsyncInvoker](#AsyncInvoker)
		* [BroadcastInvoker](#BroadcastInvoker)
		* [CompeteInvoker](#CompeteInvoker)
		* [DemoteInvoker](#DemoteInvoker)
	* [动态配置](#动态配置)
	* [扩展设计](#扩展设计)
	
### [源码构建](id:源码构建)
####Git:

用下面命令checkout代码:

```
git clone https://github.com/KimShen/Kepler
```

### [整体设计](id:整体设计)
### [调用链信息](id:调用链信息)
考虑一个调用过程：A->B、B->C、C->D。我们需要将一些信息沿着调用线路传下去，比如标签、调用者（RequestId）等信息。我们把这些信息叫做头信息。

Kepler 中，用HeadersContext来负责每个请求的Header的传播。当A执行B的RPC调用时会把Header信息序列化到请求中，这样B在接受到用户的请求后就可以将A的Header信息反序列化出来，并在调用C时候继续把它传递下去。
### [暴露服务](id:暴露服务)

服务通过Server暴露，默认使用ExportedDelegate进行暴露。暴露有两个过程，一个是调用Exported接口的export方法，将服务分别注册到ExportedContext和zookeeper中。二是开启Netty Server，接受用户的请求。

#### Export过程
服务的export一个责任链模式，每个实现Exported接口的类会按照自己的需要进行相应的操作，其中比较重要的类是DefaultContext, ZkContext和DefaultPromotion；

* DefaultContext把服务实例封装成invoker，DefaultContext同时实现了ExportedContext接口，管理服务类型和服务实例的对应关系。

* ZkContext把服务信息，比如标签、优先级等注册到Zookeeper
	
* DefaultPromotion将服务（到方法粒度）添加到自己的Table。Promotion组件会纪录每个方法调用的耗时，判断是否需要放入Netty的IO线程里执行


#### 服务调用过程

Server端会启动Netty线程来接受客户端的请求。详细流程如下

<pre>
channelRead流程
-> 读到客户端的请求
-> 通过Promotion判断在Netty的Executor线程里做还是在Kepler线程池做
-> 将请求放入线程池中执行
</pre>
至此，Netty的channelReady已结束，真正的执行会在别的线程池里异步执行。接下来分析真正任务执行时的过程：

<pre>
-> 设置HeadContext，将调用者的Header信息传递过来
-> 检查参数合法性
-> 执行服务调用
-> 异步写回结果
-> 在promotion中记录执行时间
</pre>


#### [客户端调用](id:客户端调用)
##### Imported接口
Imported接口有一个订阅服务的subscribe方法，这个接口通用采用责任链的设计，不同的Imported实现用于实现不同的功能逻辑。

比较重要的Imported实现如下：

* ZkContext

	ZkContext用于在Zookeeper上订阅服务，监听服务实例的增减。ZkContext是一个非常重要的类，用于服务发现；
* QuietException	

	获取需要订阅的服务的静默异常；
* Collector

	用来收集服务运行时信息；


##### 服务发现
	
* 服务发现依赖几个重要的全局的容器：HostsContext、Hosts和ChannelContext
	
	HostsContext可以看作键值对容器，健为服务的类型，值为Hosts。所谓服务同一类的服务是指服务的类名、版本号和catelog都相同的服务的集合。
	
	Hosts负责管理同一类服务下面每个服务实例的状态和标签。实例有以下几种状态：waiting、bans、active。转换关系如下图
	
	<pre>
	|-----------|     连接建立     |-------------| channelInactive |-------------|
	|           |                 |             | or connect失败   |             |
	|  waiting  | --------------> |   active    | --------------> |    ban      |
	|           |                 |             |                 |             |
	|-----------|	              |-------------|                 |-------------|                          
	      ^                              ^                               |
	      |                              |                               | 
	      |节点加入                       |         建立连接                |
	      |                              |------------------------------ |
	      |	
	</pre>
	
	ChannelContext用来原理主机Host与Netty链路的Mapping映射。
	
		
* 服务发现

	ZkContext来实现服务的发现。他会监听Zookeeper上服务节点的变化。当发现某个服务有新的节点加入时，会将服务实例（提供服务的进程）的信息放入HostsContext，设为等待链接状态，继而调用connect方法，与服务端建立连接，成功后将Host设为已连接状态。注意这里的连接是同步连接。紧接着将Netty的链路包装成Invoker放入全局的ChannelContext容器中共享。

##### 注入接口

通过spring的FactoryBean进行注入。主要类为ImportedServiceFactory。返回给用户的实际对象是通过cglib代理之后的代理类，用户调用这个类的方法其实经过了以下流程

* 通过上下文获取Header，并进行合并，将链路信息传递下去
* 将用户的请求封装成Request，并生成Ack
* 执行Invoker的invoker方法

##### 调用流程

Invoker是责任链模式，每种类型的Request由相应的Invoker处理。比如普通的RPC调用会用ActualInvoker，而异步调用会走AsyncInvoker。但最基本的Invoker是通过Netty进行通信的InvokerHandler.

InvokerHandler继承ChannelInboundHandlerAdapter接口，是Netty处理链的一环。

Invoke过程：

```
1. 创建AckFuture，AckFuture包含请求的ack，request等信息
2. 将AckFuture放入Acks容器中，管理ack和AckFuture的对应关系
3. 将request写入Netty通道
4. 如果是异步请求，则返回AckFuture，否则线程阻塞，直到有返回结果。
```

channelRead接受结果过程：

```
1. 拿到Response的ack，从Acks里移出对应的AckFuture
2. 调用AckFuture.response唤醒等待线程
```

### [各种Invoker](id:各种Invoker)
### [ActualInvoker](id:ActualInvoker)

普通的Invoker（ActualInvoker）在一次RPC调用（invoke）中（输入为Request），会经过几个关键的过程：

```
1. 通过HostsContext，找到与Request服务对应的一组Hosts
2. 找到与标签匹配的Hosts
3. 调用Filter.filter过滤符合特定条件的Hosts
4. 在过滤后的Hosts里调用Routing.route方法随机选出一个Host主机
5. 调用ChannelContext.get(Host)方法获取相应的InvokerHandler
6. 调用InvokerHandler的invoke方法
```

### [AsyncInvoker](id:AsyncInvoker)
### [BroadcastInvoker](id:BroadcastInvoker)
### [CompeteInvoker](id:CompeteInvoker)
### [DemoteInvoker](id:DemoteInvoker)	


### [动态配置](id:动态配置)
#### Profile
每个Service都可以设置自己的配置文件，不同服务在kepler中可以有不同的配置。比如不同的服务可以有不同的服务发现策略

#### 动态Config
Zookeeper上会存一份服务的配置信息。当Zookeeper上节点的内容发生变化时会调用相关的设置方法，并调用ConfigAware的changed方法。

具体实现：

* 在Spring注入的时候，会把设置@Config注释的方法放入Config类进行管理。@Config的value字段表示需要监听的配置项，当配置项发生改变时会调用该方法，方法的参数为配置项的值。
* ZkContext的ConfigWatcher负责监听配置的变化，当Zookeeper上的配置项发生变化时，会调用Config类的config方法，config方法会调用带有@Config注释，并且value字段为发生变化的配置项的方法。
* 之后Config类会调用实现了ConfigAware接口类的changed方法做后续处理，参数分别为就配置和新配置。

### [序列化策略](id:序列化策略)
#### Request协议

```
---------------------------------------------------------------------
|  MetaData   |    Lengther   |   Header(key-value)   |    args     |
---------------------------------------------------------------------
```

* metadata
 
	包含service,version,catalog,method,ack

* length
	
	高4位为Headers长度，低4位为Args长度

* header
	
	调用链头信息，key-value键值
* args
	
	调用参数
	
#### Response协议
```
--------------------------------------------------
|  ACK   |    Valid   |   Response/Throwable     |
--------------------------------------------------
```
* ACK：应答ID
* Valid：RPC调用是否返回异常
* Response/Throwable* 
 
### [扩展设计](id:扩展设计)

```
public interface Extension {

	/**
	 * 安装扩展点
	 * 
	 * @param instance
	 * @return
	 */
	public Extension install(Object instance);

	public Class<?> interested();
}

```
Extension接口用于安装扩展点，interested为该扩展感兴趣的类，install方法用于安装扩展。当Extension遇到感兴趣的类就会调用install方法将其装载。

整个过程由xtensions类实现，该类实现Spring的BeanPostProcessor接口，每个被Spring托管的类在加载的时候都会调用postProcessAfterInitialization方法进行处理。

```
