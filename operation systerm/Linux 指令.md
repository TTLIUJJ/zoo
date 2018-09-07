# Linux 指令


## top

参数操作

- top -d 10
	
	10秒刷新一下系统的健康状态
	
- [Shift] + p

	以CPU占有率排序
	
- [Shift] + M

	以内存占有率排序
	
- q

	退出监控



![](http://p5s0bbd0l.bkt.clouddn.com/11_42_51__08_16_2018.jpg)

- 第一行基本信息：

 字段 | 说明 
 --- | ---
 11.44.41 | 系统当前时间
 up 71 days, 20:15 | 系统的运行时间
 1 users | 当前登录了一个用户
 load average(1, 5, 15 min) | 0.00, 0.00, 0.00

- 第二行进程信息：

	字段 | 说明
	--- | ---
	Tasks: 98total | 系统中的进程总数
	1 running |  处于运行的进程数
	97 sleeping | 处于睡眠的进程数
	0 stoped | 处于停止的进程数
	0 zombie | 僵尸进程数

- 第三行CPU信息：

	 字段 | 说明 
	 --- | ---
	 0.5 us | 用户模式占用Cpu的百分比
	 0.6 sy | 系统模式占用Cpu的百分比
	 0.0 ni | 改变过优先级别的用户进程占用Cpu的百分比
	 0.0 wa | 等待输入/输出的进程占用Cpu的百分比
	 0.0 hi | 硬中断请求服务占用Cpu的百分比
	 0.0 si | 软中断请求服务占用Cpu的百分比
	 0.0 st | 虚拟时间百分比，虚拟Cpu等待实际Cpu的时间百分比
	 
- 第四行物理内存信息：

	字段 | 说明  
	--- | ---
	2062252 totals |  物理内存的总量（单位：kb）
	140956 free | 空闲的物理内存为140Mb
	1362108 used | 已使用的物理内存为 1.36Gb
	559308 buff/cache |  缓冲区内存为559Mb
	
## ps

查看进程的工作状态

- ef：标准的格式

- aux：BSD风格，可以查看进程占用的CPU和内存

```shell
# ps -ef | grep redis
UID       PID   PPID  C STIME TTY        TIME            CMD          
root     27638     1  0 Jun29 ?        00:45:58 redis-server 127.0.0.1:9000 [cluster]
root     27642     1  0 Jun29 ?        00:46:12 redis-server 127.0.0.1:8000 [cluster]
root     27646     1  0 Jun29 ?        00:45:55 redis-server 127.0.0.1:7000 [cluster]


# ps -aux | grep redis 
USER      PID   %CPU %MEM   VSZ   RSS TTY      STAT  START  TIME     COMMAND
root     27638  0.0  0.3  30360  6972 ?        Ssl  Jun29  45:58 redis-server 127.0.0.1:9000 [cluster]
root     27642  0.0  0.3  30360  6304 ?        Ssl  Jun29  46:12 redis-server 127.0.0.1:8000 [cluster]
root     27646  0.0  0.3  30360  6304 ?        Ssl  Jun29  45:55 redis-server 127.0.0.1:7000 [cluster]
```

## lsof

查看占有端口相关的命令

```shell
# lsof -i:6379
COMMAND   PID  USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
redis-ser 721 redis    4u  IPv4  13872      0t0  TCP localhost:6379 (LISTEN)

# lsof -i | grep 721
COMMAND   PID  USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
redis-ser 721 redis    4u  IPv4  13872      0t0  TCP localhost:6379 (LISTEN)
```

## netstat

netstat无用户权限控制，lsof只能看到本用户
	
## sed

```shell
/**
 * 在dirName及其子目录下，搜索包含str1的文件，然后将文件中的str2替换为str3
 * 当str1等于str2时，就是批量查找和替换了
 */
# grep -rl "str1" dirName | xargs sed -i 's/str2/str3/g'
```
	