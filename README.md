##Kepler Distributed Service Framework
* 不仅是RPC  
`服务之殇是否记忆犹新？`
<img src="https://raw.githubusercontent.com/KimShen/Images/master/target.png" width="100%"/>
<br></br>
* 为什么造轮子  
`先有Dubbo修补天，ZeroC ICE还在前。巨人的肩膀上才能看的更远。`
<br></br>
* Kepler Style  
`透明代理，无缝集成。我中有你，你中无我。`
<img src="https://raw.githubusercontent.com/KimShen/Images/master/split.png" width="100%"/>
`天下代码，合合分分，分分合合。随时切换才能保持正确的姿势。`
<br></br>
* 上帝视角  
<img src="https://raw.githubusercontent.com/KimShen/Images/master/overview.png" width="100%"/>
* <a href="http://zookeeper.apache.org">`关于ZooKeeper`</a>
* <a href="https://www.mongodb.org/">`关于MongoDB(可选)`</a>
<br></br>
* 内部运转
<img src="https://raw.githubusercontent.com/KimShen/Images/master/workflow.png" width="100%"/>
`角色说明:`
	* Service:服务提供者
	* Client: 服务调用者		
	* Registry: 注册中心
	* Monitor: 数据收集服务, 收集Service/Client运行时状态
	* Admin: 服务管理中心, 提供服务治理统一入口(API) 
<br></br>
* 怎么玩
	* 如何构建
		* @See[<a href="install.html">如何安装</a>]
		* @See[<a href="quick_start.html">快速尝试</a>]
		* @See[<a href="maven.html">快速构建(Kepler Maven)</a>]
		* @See[<a href="deploy.html">部署服务</a>]
		* @See[<a href="eclipse.html">调试代码(Eclipse)</a>]	
	* 基础概念
		* @See[<a href="service.html">服务(Service)</a>]
		* @See[<a href="tag.html">标签(Tag)</a>]
		* @See[<a href="group.html">分组(Group)</a>]
		* @See[<a href="instance.html">实例(Instance)与主机(Host)</a>]
		* @See[<a href="sid.html">ServerID(SID)</a>]
	* 项目迁移
		* @See[<a href="migrate.html">从Dubbo迁移</a>]
