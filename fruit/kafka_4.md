# 4. Design

## 4.1. Motivation

我们设计Kafka用来作为统一的平台来处理大公司可能拥有的所有实时数据源。为了做到这点，我们必须思考大量的使用场景。

- 它必须有高吞吐去支持大数据流，例如实时日志聚合。

- 它必须优雅的处理数据积压，以支持定期从离线系统加载数据。

- 这也意味着系统必须支持低延迟的分发来处理传统消息系统的场景。

- 我们想支持分区的、分布式的、实时的处理数据源并创建新的数据源，这推动了我们的分区和消费模型。


- 最后，将流反馈到其他系统进行服务的情况下，我们知道系统必须能够保证容错性，在部分机器故障的时候提供服务。

支持这些使用推动我们做了一些列特殊的元素设计，比起传统的消息系统更像是数据库日志。我们将在以下章节介绍一些设计要素。

## 4.2. Persistence

**Don’t fear the filesystem!**

Kafka很大程度上依赖文件系统存储和缓存消息。“磁盘是缓慢的”是一个通常的认知，这是人们怀疑持久化的结构能否提供强大的性能。事实上，磁盘比人们想象的更慢也更快，这基于如何去使用它们；一个合适的设计可以使磁盘和网络一样的快速。

影响磁盘性能的核心因素是磁盘驱动的吞吐和过去十年磁盘的查找方式不同了。使用六个7200rpm的SATA RAID-5阵列的JBOD配置线性写入能力为600MB/sec，而随机写的性能仅仅是100k/sec，相差了6000倍。线性写入和读取是最可预测的，并且被操作系统大量的优化。现代操作系统提供read-ahead和write-behind技术，他们大块预读数据，并将较小的罗机械合并成较大的物理写入。在ACM Queue的文章中可以找到此问题相关的进一步讨论；他们实际上发现顺序访问磁盘在某些情况下比随机访问内存还快。

为了弥补性能的差异，现代操作系统在使用主内存来做磁盘缓存时变的越来越激进。当内存被回收时，现代操作系统将乐意将所有可用内存转移到磁盘缓存，而且性能会降低很多。所有的磁盘读写都需要通过这层缓存。这个功能不会被轻易关闭，除非使用Direct IO，因此尽管在进程内缓存了数据，这些数据也有可能在操作系统的pagecache中缓存，从而被缓存了两次。

此外，我们建立在JVM之上，任何在Java内存上花费过时间的人都知道两件事情：

对象的内存开销非常大，通常将存储的数据大小翻倍（或更多）。

Java的内存回收随着堆内数据的增多变的越来越缓慢。

由于这些因素，使用文件系统并依赖于pagecache要优于维护内存中缓存或其他结构——我们至少可以通过直接访问内存来使可用内存增加一倍，并通过存储字节码而不是对象的方式来节约更多的内存。这样做将可以在32G的机器上使用28-30GB的内存，而不需要承受GC的问题。此外，及时重启服务，内存会保持有效，而进程内缓存将需要重建（对于10G的数据可能需要10分钟），否则需要从冷数据加载（可怕的初始化性能）。这也大大简化了代码，因为保持缓存和文件之间的一致性是由操作系统负责的，这比进程中操作更不容易出错。

这是一个简单的设计：在进程内尽量缓冲数据，空间不足时将所有数据刷写到磁盘，我们采用了相反的方式。数据并尽快写入一个持久化的日志而不需要立即刷到磁盘。实际上这只是意味着数据被转移到了内核的pagecache。

（以pagecache为中心的设计风格）

**Constant Time Suffices**

在消息系统中使用持久化数据通常是具有关联的BTree或其他随机访问的数据结构，以维护消息的元数据。BTree是最通用的数据结构，可以在消息系统中支持各种各样的语义。BTree的操作时间复杂度是O(log N)。通常O(log N)被认为是固定时间的，但是在磁盘操作中却不是。每个磁盘一次只能执行一个seek，所以并行度受到限制。因此即使少量的磁盘搜索也会导致非常高的开销。由于操作系统将快速的缓存操作和非常慢的磁盘操作相结合，所以观察到树结构的操作通常是超线性的，因为数据随固定缓存增加。

直观的，持久化队列可以像日志的解决方案一样，简单的读取和追加数据到文件的结尾。这个结构的优势是所有的操作都是O(1)的，并且读取是可以并行不会阻塞的。这具有明显的性能优势，因为性能与数据大小完全分离，可以使用低速的TB级SATA驱动器。虽然这些驱动器的搜索性能不佳，但是对于大量读写而言，他们的性能是可以接受的，并且价格是三分之一容量是原来的三倍。

无需任何的性能代价就可以访问几乎无限的磁盘空间，这意味着我们可以提供一些在消息系统中非寻常的功能。例如，在Kafka中，我们可以将消息保留较长的时间（如一周），而不是在消费后就尽快删除。这位消费者带来了很大的灵活性。

## 4.3. Efficiency

我们在效率上付出了很大的努力。主要的用例是处理web的数据，这个数据量非常大：每个页面可能会生成十几个写入。此外我们假设每个发布的消息至少被一个Consumer消费，因此我们尽可能使消费的开销小一些。

从构建和运行一些类似的系统的经验发现，效率是多租户操作的关键。如果下游基础服务成为瓶颈，那么应用程序的抖动将会引起问题。我们确保应用程序不会引起基础服务的Load问题，这个非常重要的，当一个集群服务上百个应用程序的时候，因为应用的使用模式的变化时非常频繁的。

我们在之前的章节中讨论过磁盘的效率。一旦不良的磁盘访问模式被消除，这种类型的系统有两个低效的原因：太多太小的IO操作和过多的数据拷贝。

太小的IO操作问题存在于客户端和服务端之间，也存在于服务端自身的持久化当中。

为了避免这个问题，我们的协议围绕“message set”抽象，通常是将消息聚合到一起。这允许网络请求将消息聚合到一起，并分摊网络往返的开销，而不是一次发送单个消息。服务端依次将大块消息追加到日志中，消费者一次线性获取一批数据。

这种简单的优化产生了一个数量级的加速。分批带来了更大的网络包，连续的磁盘操作，连续的内存块等等，这些都使得Kafka将随机消息写入转化为线性的写入并流向Consumer。

其他低效的地方是字符复制。在消息少时不是问题，但是对负载的影响是显而易见的。为了避免这种情况，我们采用被producer、broker、Consumer共享的标准的二进制消息格式（所以数据可以在传输时不需要进行修改）。

由Broker维护的消息日志本身只是一批文件，每个文件由一系列以相同格式写入的消息构成。保持相同的格式保证了最重要的优化：网络传输和持久化日志块。现在UNIX操作系统提供了高度优化的代码路径用于将pagecache的数据传输到网络；在Linux中，这有sendfile实现。

要了解sendfile的影响，了解从文件到网络传输数据的data path非常重要：

操作系统从磁盘读取文件数据到pagecache，在内核空间
用户从内核空间将数据读到用户空间的buffer
操作系统重新将用户buffer数据读取到内核空间写入到socket中
操作系统拷贝socket buffer数据到NIC buffer并发送到网络
这显然是低效的，有四个副本和两个系统调用。使用sendfile，允许操作系统直接将数据从pagecache写入到网络，而避免不必要的拷贝。在这个过程中，只有最终将数据拷贝到NIC buffer(网卡缓冲区)是必要的。

我们期望一个共同的场景是多个Consumer消费一个Topic数据，使用zero-copy优化，数据被拷贝到pagecache并且被多次使用，而不是每次读取的时候拷贝到内存。这允许以接近网络连接的速度消费消息。

pagecache和sendfile的组合意味着在消费者追上写入的情况下，将看不到磁盘上的任何读取活动，因为他们都将从缓存读取数据。


**End-to-end Batch Compression**

在一些场景下，CPU核磁盘并不是性能瓶颈，而是网络带宽。在数据中心和广域网上传输数据尤其如此。当然，用户可以压缩它的消息而不需要Kafka的支持，但是这可能导致非常差的压缩比，因为冗余的大部分是由于相同类型的消息之间的重复（例如JSON的字段名）。多个消息进行压缩比单独压缩每条消息效率更高。

Kafka通过允许递归消息来支持这一点。一批消息可以一起压缩并以此方式发送到服务端。这批消息将以压缩的形式被写入日志，只能在消费端解压缩。

## 4.4. The Producer

**Load balancing**

Producer直接向Leader Partition所在的Broker发送数据而不需要经过任何路由的干预。为了支持Producer直接向Leader Partition写数据，所有的Kafka服务节点都支持Topic Metadata的请求，返回哪些Server节点存活的、Partition的Leader节点的分布情况。

由客户端控制将数据写到哪个Partition。这可以通过随机或者一些负载均衡的策略来实现（即客户端去实现Partition的选择策略）。Kafka暴露了一个接口用于用户去指定一个Key，通过Key hash到一个具体的Partition。例如，如果Key是User id，那么同一个User的数据将被发送到同一个分区。这样就允许消费者在消费时能够对消费的数据做一些特定的处理。这样的设计被用于处理“局部敏感”的数据（结合上面的场景，Partition内的数据是可以保持顺序消费的，那么同一个用户的数据在一个分区，那么就可以保证对任何一个用户的处理都是顺序的）。

**Asynchronous send**

批处理是提升效率的主要方式一致，为了支持批处理，Kafka允许Producer在内存聚合数据并在一个请求中发出。批处理的大小可以是通过消息数量指定的，也可以是通过等待的时间决定的（例如64K或者10ms）。这样允许聚合更多的数据后发送，减少了IO操作。缓冲的数据大小是可以配置了，这样能适当增加延迟来提升吞吐。

## 4.5. The Consumer
Kafka Consumer通过给Leader Partition所在的Broker发送“fetch”请求来进行消费。Consumer在请求中指定Offset，并获取从指定的Offset开始的一段数据。因此Consumer对消费的位置有绝对的控制权，通过重新设置Offset就可以重新消费数据。

**Push vs Pull**

我们考虑的一个初步问题是Consumer应该从Broker拉取数据还是Broker将数据推送给Consumer。在这方面，Kafka和大多数消息系统一样，采用传统的设计方式，由Producer想Broker推送数据，Consumer从Broker上拉取数据。一些日志中心系统，如Scribe和Apache Flume，遵循数据向下游推送的方式。两种方式各有利弊。基于推送的方式，由于是由Broker控制速率，不能很好对不同的Consumer做处理。Consumer的目标通常是以最大的速率消费消息，不幸的是，在一个基于推送的系统中，当Consumer消费速度跟不上生产速度 时，推送的方式将使Consumer“过载”。基于拉取的系统在这方面做的更好，Consumer只是消费落后并在允许时可以追上进度。消费者通过某种协议来缓解这种情况，消费者可以通过这种方式来表明它的负载，这让消费者获得充分的利用但不会“过载”。以上原因最终使我们使用更为传统的Pull的方式。

Pull模型的另一个优势是可以聚合数据批量发送给Consumer。Push模型必须考虑是立即推送数据给Consumer还是等待聚合一批数据之后发送。如果调整为低延迟，这将导致每次只发送一条消息（增加了网络交互）。基于Pull的模式，Consumer每次都会尽可能多的获取消息（受限于可消费的消息数和配置的每一批数据最大的消息数），所以可以优化批处理而不增加不必要的延迟。

基于Pull模式的一个缺陷是如果Broker没有数据，Consumer可能需要busy-waiting的轮训方式来保证高效的数据获取（在数据到达后快速的响应）。为了避免这种情况，我们在Pull请求中可以通过参数配置“long poll”的等待时间，可以在服务端等待数据的到达（可选的等待数据量的大小以保证每次传输的数据量，减少网络交互）。

你可以想象其他一些从端到端，采用Pull的可能的设计。Producer把数据写到本地日志，Broker拉取这些Consumer需要的数据。一个相似的被称为“store-and-forward”的Producer经常被提及。这是有趣的，但是我们觉得不太适合我们可能会有成千上万个Producer的目标场景。我们维护持久化数据系统的经验告诉我们，在系统中使多应用涉及到上千块磁盘将会使事情变得不可靠并且会使操作它们变成噩梦。最后再实践中，我们发现可以大规模的运行强大的SLAs通道，而不需要生产者持久化。

**Consumer Position**

记录哪些消息被消费过是消息系统的关键性能点。

大多数消息系统在Broker上保存哪些消息已经被消费的元数据。也就是说，Broker可以在消费传递给Consumer后立即记录或等待消费者确认之后记录。这是一个直观的选择，并且对于单个服务器而言并没有更好的方式可以存储这个状态。大多数消息系统中的存储设备并不能很好的伸缩，所以这也是务实的选择——当Broker确认消息被消费后就立即删除，以保证存储较少的数据。

让Broker和Consumer关于那些消息已经被消费了达成一致并不是一个简单的问题。如果Broker在将消息写到网络之后就立即认为消息已经被消费，那么如果Consumer消费失败（Consumer在消费消息之前Crash或者网络问题等）消息将丢失。为了解决这个问题，一些消息系统增加了ACK机制，消息被标记为只是发送出去而不是已经被消费，Broker需要等待Consumer发送的ACK请求之后标记具体哪些消息已经被消费了。这个策略修复了消息丢失的问题，但是引起了新的问题。第一，如果Consumer处理了消息，但是在发送ACK给Broker之前出现问题，那么消息会被重复消息。第二，Broker需要维护每一条消息的多个状态（是否被发送、是否被消费）。棘手的问题是要处理被发送出去但是没有被ACK的消息。

Kafka采用不同的方式处理。Topic被划分为多个内部有序的分区，每个分区任何时刻只会被一个group内的一个Consumer消费。这意味着一个Partition的Position信息只是一个数字，标识下一条要消费的消息的偏移量。这使得哪些消息已经被消费的状态变成了一个简单的数据。这个位置可以定期做CheckPoint。这使得消息的ACK的代价非常小。


 
这个方案还有其他的好处。消费者可以优雅的指定一个旧的偏移量并重新消费这些数据。这和通常的消息系统的观念相违背，但对很多消费者来说是一个很重要的特性。比如，如果Consumer程序存在BUG，在发现并修复后，可以通过重新消费来保证数据都正确的处理。

Offline Data Load

可扩展的持久化存储的能力，是消费者可以定期的将数据导入到像Hadoop这样的离线系统或关系型数据仓库中。

在Hadoop的场景中，我们通过把数据分发到独立的任务中进行并行处理，按照node/topic/partition组合，充分使用另行能力加载数据。Hadoop提供任务管理，失败的任务可以重新启动，而不需要担心重复数据的危险——任务会从原始位置重新启动。