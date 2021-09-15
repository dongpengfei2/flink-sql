## JVM常用调试工具介绍

一、Linux ps （英文全拼：process status）命令用于显示当前进程的状态
```
ps -e : 所有的进程均显示出来
ps -f : 显示详细信息
ps aux : 查看进程占用内存和cpu
ps -ef | grep java : 查询Java进程的详细信息，一般用来查询Java进程的进程号
ps aux --sort -rss : 列出进程拿物理内存占用排序，一般用来查询哪个进程占有资源最多
ps aux --sort=-rss : 同上，按内存降序排列
ps aux --sort=+rss : 按内存升序排列
ps aux --sort=-%cpu : 为按cpu降序排列
ps aux --sort=+%cpu : 按cpu升序排列
```
ps aux 基本输出
```
USER       PID %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND
root     27487  0.1  0.1 1177836 109764 ?      SNl  03:20   0:46 /usr/bin/osqueryd
root      1459  0.0  0.1 191196 103600 ?       Ss   Jul28   1:24 /usr/lib/systemd/systemd-journald
root      3081  0.0  0.1 545396 66376 ?        Ssl  Jul28   1:59 /usr/sbin/rsyslogd -n
```
ps aux 列名解释
```
USER //用户名
%CPU //进程占用的CPU百分比
%MEM //占用内存的百分比
VSZ //该进程使用的虚拟內存量（KB）
RSS //该进程占用的固定內存量（KB）（驻留中页的数量）
STAT //进程的状态
START //该进程被触发启动时间
TIME //该进程实际使用CPU运行的时间
```
*注：优化机器资源主要从占用机器cpu和mem高的程序入手*

二、Linux top命令查看正在运行的进程和系统负载信息，包括cpu负载、内存使用、各个进程所占系统资源等
```
d 指定每两次屏幕信息刷新之间的时间间隔。当然用户可以使用s交互命令来改变之。
p 通过指定监控进程ID来仅仅监控某个进程的状态
s 使top命令在安全模式中运行。这将去除交互命令所带来的潜在危险。
i 使top不显示任何闲置或者僵死进程。
c 显示整个命令行而不只是显示命令名。
-n 与 -b 搭配，意义是，需要进行几次 top 的输出结果。
-p 指定某些个 PID 来进行观察监测而已

top -d 2 : 每两秒执行一次top的刷新
top -n 2 : 刷新2次后退出执行
```
基本输出
```
top - 15:17:12 up 48 days,  4:37,  1 user,  load average: 0.06, 0.05, 0.05
Tasks: 216 total,   1 running, 215 sleeping,   0 stopped,   0 zombie
%Cpu(s):  0.2 us,  0.1 sy,  0.0 ni, 99.7 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
KiB Mem : 64756908 total, 35147892 free,  6943464 used, 22665552 buff/cache
KiB Swap:        0 total,        0 free,        0 used. 53694472 avail Mem 

  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND                                                                                 
 2967 root      20   0  721404  21428   6684 S   1.7  0.0 525:13.46 node_exporter                                                                           
27058 yarn      20   0 3106652 690164  30056 S   1.3  1.1 863:58.17 java                                                                                    
 3171 root      20   0 1013820  17204   5796 S   0.7  0.0 180:00.47 /usr/local/clou                                                                         
22513 root      10 -10  147488  31112   9948 S   0.7  0.0 352:29.84 AliYunDun                                                                               
31528 root      20   0 1258444  49784   7960 S   0.7  0.1 368:42.25 python2                                                                                 
 3052 zookeep+  20   0 6466816 564708  14604 S   0.3  0.9  38:39.92 java                                                                                    
 4631 yarn      20   0 3740852   1.0g 107820 S   0.3  1.6  80:26.75 java                                                                                    
 5571 yarn      20   0 2843540 917988  41528 S   0.3  1.4 108:07.28 java                                                                                    
10534 root      20   0  154672   5548   4240 S   0.3  0.0   0:00.06 sshd                                                                                    
13723 yarn      20   0 3116408 978.1m  41568 S   0.3  1.5 117:49.00 java                                                                                    
23847 root      20   0  162168   2392   1580 R   0.3  0.0   0:00.01 top
```
字段解释
```
上半部分显示了整体系统负载情况:
top一行：从左到右依次为当前系统时间，系统运行的时间，系统在之前1min、5min和15min内cpu的平均负载值
Tasks一行：该行给出进程整体的统计信息，包括统计周期内进程总数、运行状态进程数、休眠状态进程数、停止状态进程数和僵死状态进程数
Cpu(s)一行：cpu整体统计信息，包括用户态下进程、系统态下进程占用cpu时间比，nice值大于0的进程在用户态下占用cpu时间比，cpu处于idle状态、wait状态的时间比，以及处理硬中断、软中断的时间比
Mem一行：该行提供了内存统计信息，包括物理内存总量、已用内存、空闲内存以及用作缓冲区的内存量
Swap一行：虚存统计信息，包括交换空间总量、已用交换区大小、空闲交换区大小以及用作缓存的交换空间大小

下半部分显示了各个进程的运行情况
PID: 进程pid
USER: 拉起进程的用户
PR: 该列值加100为进程优先级，若优先级小于100，则该进程为实时(real-time)进程，否则为普通(normal)进程，实时进程的优先级更高，更容易获得cpu调度，以上输出结果中，java进程优先级为120，是普通进程，had进程优先级为2，为实时进程，migration 进程的优先级RT对应于0，为最高优先级
NI: 进程的nice优先级值，该列中，实时进程的nice值为0，普通进程的nice值范围为-20~19
VIRT: 进程所占虚拟内存大小（默认单位kB）
RES: 进程所占物理内存大小（默认单位kB）
SHR: 进程所占共享内存大小（默认单位kB）
S: 进程的运行状态
%CPU: 采样周期内进程所占cpu百分比
%MEM: 采样周期内进程所占内存百分比
TIME+: 进程使用的cpu时间总计
COMMAND: 拉起进程的命令
```

三、Linux top -H -p pid。查看某个进程内部线程占用情况
```
top -H -p 4631

  PID USER      PR  NI    VIRT    RES    SHR S %CPU %MEM     TIME+ COMMAND                                                                                  
 4631 yarn      20   0 3740852   1.0g 107820 S  0.0  1.6   0:00.00 java                                                                                     
 4781 yarn      20   0 3740852   1.0g 107820 S  0.0  1.6   0:00.58 java                                                                                     
 4782 yarn      20   0 3740852   1.0g 107820 S  0.0  1.6   0:17.15 java                                                                                     
 4783 yarn      20   0 3740852   1.0g 107820 S  0.0  1.6   0:17.17 java                                                                                     
 4784 yarn      20   0 3740852   1.0g 107820 S  0.0  1.6   0:17.20 java                                                                                     
 4785 yarn      20   0 3740852   1.0g 107820 S  0.0  1.6   0:17.14 java                                                                                     
 4786 yarn      20   0 3740852   1.0g 107820 S  0.0  1.6   0:17.14 java                                                                                     
 4787 yarn      20   0 3740852   1.0g 107820 S  0.0  1.6   0:17.17 java 
```

四、jps(Java Virtual Machine Process Status Tool) 是java提供的一个显示当前所有java进程pid的命令
```
jps -m	启动时main()的参数
jps -l	输出主类全名
jps -v	输出虚拟机进程启动时的参数
```

五、jstat(Java Virtual Machine statistics monitoring tool)主要利用JVM内建的指令对Java应用程序的资源和性能进行实时的命令行的监控，包括了对Heap size和垃圾回收状况的监控。可见，Jstat是轻量级的、专门针对JVM的工具，非常适用
0. jstat -options
```
jstat -class (类加载器) 
jstat -compiler (JIT) 
jstat -gc (GC堆状态) 
jstat -gccapacity (各区大小) 
jstat -gccause (最近一次GC统计和原因) 
jstat -gcnew (新区统计)
jstat -gcnewcapacity (新区大小)
jstat -gcold (老区统计)
jstat -gcoldcapacity (老区大小)
jstat -gcpermcapacity (永久区大小)
jstat -gcutil (GC统计汇总)
jstat -printcompilation (HotSpot编译统计)
```
1. jstat –class<pid> : 显示加载class的数量，及所占空间等信息
```
jstat -class 27058 1000 5 : 一秒执行一次，执行5次之后退出

Loaded  Bytes  Unloaded  Bytes     Time   
  8991 17923.7       35    50.3       2.83
  8991 17923.7       35    50.3       2.83
  8991 17923.7       35    50.3       2.83
  8991 17923.7       35    50.3       2.83
  8991 17923.7       35    50.3       2.83
  
Loaded - 装载的类的数量
Bytes - 装载类所占用的字节数
Unloaded - 卸载类的数量
Bytes - 卸载类的字节数
Time - 装载和卸载类所花费的时间
```
2. jstat -gc <pid>: 可以显示gc的信息，查看gc的次数，及时间
```
jstat -gc 27058 1000 5 : 一秒执行一次，执行5次之后退出

 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT   
34944.0 34944.0 2187.1  0.0   279616.0 156340.1  699072.0   56809.5   57908.0 56899.0 6520.0 6243.6  97000  903.441   4      0.034  903.475
34944.0 34944.0 2187.1  0.0   279616.0 156842.5  699072.0   56809.5   57908.0 56899.0 6520.0 6243.6  97000  903.441   4      0.034  903.475
34944.0 34944.0 2187.1  0.0   279616.0 157202.9  699072.0   56809.5   57908.0 56899.0 6520.0 6243.6  97000  903.441   4      0.034  903.475
34944.0 34944.0 2187.1  0.0   279616.0 189623.8  699072.0   56809.5   57908.0 56899.0 6520.0 6243.6  97000  903.441   4      0.034  903.475
34944.0 34944.0 2187.1  0.0   279616.0 190124.3  699072.0   56809.5   57908.0 56899.0 6520.0 6243.6  97000  903.441   4      0.034  903.475

S0C - 年轻代中第一个survivor（幸存区）的容量 (字节)
S1C - 年轻代中第二个survivor（幸存区）的容量 (字节)
S0U - 年轻代中第一个survivor（幸存区）目前已使用空间 (字节)
S1U - 年轻代中第二个survivor（幸存区）目前已使用空间 (字节)
EC  - 年轻代中Eden（伊甸园）的容量 (字节)
EU  - 年轻代中Eden（伊甸园）目前已使用空间 (字节)
OC  - Old代的容量 (字节)
OU  - Old代目前已使用空间 (字节)
PC  - Perm(持久代)的容量 (字节)
PU  - Perm(持久代)目前已使用空间 (字节)
YGC - 从应用程序启动到采样时年轻代中gc次数
YGCT- 从应用程序启动到采样时年轻代中gc所用时间(s)
FGC - 从应用程序启动到采样时old代(全gc)gc次数
FGCT- 从应用程序启动到采样时old代(全gc)gc所用时间(s)
GCT - 从应用程序启动到采样时gc用的总时间(s)
```