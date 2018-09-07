# Git

- git remote
- git add
- git commit
- git log
- git relog
- git reset
- git pull 
- git merge
- git stash
- git tag

## Git Remote

- git remote add origin git@github.com:xxx/learngit.git

本地仓库与远程仓库关联

## Git Add

- git add a.md
- git add c.md d.md
- git add .


## Git Commit

- git commit -m "commit_message(can not be null)"


- 比较：
	
	- git add 将要提交的修改文件放到暂存区

	- git commit 将暂存区的文件提交到分支

## Git Checkout

- git checout -- your_filename.md

- 分两种情况：

	- 文件自修改后没有放到暂存区，撤销就回到和版本库一样的状态
	
	- 文件被修改并放入暂存区，撤销就回到刚添加到暂存区的状态 

## Git Diff


- git diff 比较工作区和暂存区

- git diff --cached 比较暂存区与最新本地版本库

- git diff HEAD 比较工作区和最新本地版本库


## Git Reset

使用场景：

- 使用git add添加错了文件
	
	- git reset Head \<filename>
	
- 本地回滚
	
	- git log 查看历史提交记录
	- git reflog 查看历史命令记录
	- git reset --hard 	skdasjhdkjsafaydnmsdh
	

## Git Branch

- git checkout -b dev 创建dev分支并切换过去

- git checkout dev 切换到dev分支

- git branch 查看所有分支

- git branch -d dev 删除dev分支

多人协作开发流程：

- 0. git branch --set-upstream-to \<localBranch> origin\<remoteBranch> 进行关联分支
- 1. 首先git pull同步远程dev分支到本地
- 2. 如果有冲突，解决冲突之后，合并my分支到本地dev分支
- 3. git push 本地dev分支推到远程dev分支

## Git Merge


- git merge --no-ff dev


![](http://p5s0bbd0l.bkt.clouddn.com/git_stream.png)

合并分支时，加上 --no-ff 参数就可以用普通模式合并，合并后的历史有分支，能看出曾经做过的合并，而默认的fast forward合并就看不出来曾经做过的合并


## Git Pull

使用场景：

取回远程主机某个分支的更新，再与本地的制定分支合并

- git pull origin remoteBranchName:myBranchName


## Git Stash

使用场景：

- Bug分支，先使用stash暂存工作区内容，创建新的Bug分支进行修复，之后再pop回到工作区。

- 写代码发现有一个Class类是多余的，想删掉又担心以后可能需要用上，想保存又不想增加一个脏的提交。

- 往往使用git的切换分支解决任务问题。当前在项目v2版本进行开发，而突然发现了v1版本有一个不得不修改的BUG。我们可以把当前完成一半的v2版本提交到本地仓库，然后切换分支去修改v1的代码。弊端：
	- 大量的不必要log
	- 提交不完善的代码很不爽

- git stash 储藏当前工作目录的状态，也就是将目前被追踪的文件（git add操作）暂存，并将其保存到一个未完结变更的堆栈中

- git stash save "git stash remark"

- git stash pop 弹出保存的状态

- git stash list 堆栈中所有的stash


## Git Tag

- git tag myTag_20180808 

	 默认标签是打在最新提交的commit上的。

- git tag anthorTag_xxx thisIsCommmitHash

	将标签打在 thisIsCommitHash 的这个commit上

- git tag 

	查看所有标签，按照字典排序
	
标签总是和某个commit挂钩，如果这个commit既出现在master分支，又出现dev分支，那么去两个分支都可以看到这个标签

- git tag -d myTag_20180808
	
	删除标签myTag_20180808
	
- git push origin myTag_20180808

	推送本地标签到远程仓库

- git push origin :refs/tags/myTag_20180808

	删除远程仓库标签

## 解决代码冲突

遇到和dev分支冲突的情况

- 本地拉取(fetch)最新的dev代码
- 在dev上切出一个新分支b_panda_20180808
- 将自己开发分支的代码merge到b_panda_20180808分支
- 解决b_panda_20180808分支上的冲突
- 重新提交一个b_panda_20180808到dev的merge请求




# Git 分支

**Git分支：Git的必杀技特性**


git分支的优越性：

- 创建新分支这一操作几乎能在瞬间完成，并且在不同分支之间的切换也是一样便捷。
- 在其他很多版本控制系统中，从开发主线上使用分支，常常需要创建一个源代码的目录的副本。

小结：

- 速度快
- 节省资源


## Git 保存数据


- 快照读：保存的某一时刻的数据内容
- 当前读：当前时刻的数据内容

Git保存的不是文件的变化或者差异，而是一系列不同时刻的文件快照。

在进行提交操作的时候，Git会保存一个提交对象（commit object），其中包括：

- 一个指向暂存内容快照的指针
- 指向它父对象的指针
	- 首次提交的提交对象没有父对象
	- 普通提交的提交对象有一个父对象（即上次提交的提交对象）
	- 多个分支合并产生的提交对象有多个父对象
- 提交时输入的信息
- 作者的姓名和邮箱


当使用git commit进行提交时，Git会先计算每一个子目录（下图只有项目根目录）的校验和，然后在Git仓库中这些校验和作为树节点的索引。

最后，Git创建一个提交对象，指向这个树根节点（92ec2）的指针。如此一来，Git就可以在需要的时候重现此次保存的文件快照。

![](http://owj98yrme.bkt.clouddn.com/branch1.png)


#### 首次提交

首次提交(98ca9)，没有指向父提交对象的指针。

之后的修改提交产生的提交对象，都会包含一个指向上次提交对象（父对象）的指针。

注意：每次的提交对象都产生一次文件快照。

![](http://owj98yrme.bkt.clouddn.com/branch2.png)


#### 提交对象及其父对象

Git分支的本质：仅仅只是指向提交对象的可变指针。

Git的默认分支名字是master，在个人开发的时候，进行多次提交操作之后，其实我们已经有了一个指向最后那个提交对象的master分支。它会在每次提交操作中自动向前移动。

![](http://owj98yrme.bkt.clouddn.com/branch3.png)

![](http://owj98yrme.bkt.clouddn.com/branch4.png)


值得一提的是：master分支并不是一个特殊分支，它与其他分支完全没有区别。master分支存在的原因是git init会默认创建它。那么，git所有的分支都应该是无特殊含义的，除非我们自定义它一定的功能与权限，比如dev,online。

#### Git之HEAD指针

当我们本地上有多个分支的时候，Git通过HEAD指针指向当前所在的本地分支。例如在下图中，HEAD指向testing分支，即我们工作在testing分支中。

![](http://owj98yrme.bkt.clouddn.com/branch5.png)

如果这时候（工作在testing分支），并且commmit新的提交对象，那么HEAD指针会随着testing向前移到，而master指针指向的提交对象正是testing提交对象的父对象，并且落后testing一个版本。



#### 双线操作

分支切换会改变工作目录中的文件，在切换分支的时候，一定要注意注意注意工作目录中的文件会被改变。如果是切换到一个较旧的分支，那么此时的工作目录
会恢复到改分支最后一次提交的样子。

如下图，切出两条分支进行双线操作，并且都执行了提交操作，生成了提交对象。我们可以在不同的分支上不断地来回切换和工作，并在时机成熟的时候将它们合并起来。

可能发生场景：在一个项目中，同时开发两个新的需求。

![](http://owj98yrme.bkt.clouddn.com/branch6.png)

## 分支合并

在合作开发中，需要合并代码，避免不了分支合并时候产生的冲突，这时会遇到和dev分支冲突的情况。

根本原因是：线上的代码和本地的代码有冲突。那么这时需要将线上代码拉去到本地，解决完冲突再重新提合并请求。

- 本地拉取最新的dev代码
- 在dev上切出一个新分支b\_panda\_20180808
- 将自己开发分支的代码merge到b\_panda\_20180808分支
- 解决b\_panda\_20180808分支上的冲突
- 重新提交一个b\_panda\_20180808到dev的merge请求


 ```java
 					merge
 	b_new_panda  一一一一一一 >  conflict 
 	   ↑							|
 	   |							|	new_merge_request
 	   | 							|   
 	   |							↓
      dev						  dev
 	
 ```


## 分支开发工作流

- master: 保存完全稳定的代码，已经发布或者即将发布的代码

- dev: 用来做后续开发或者测试用的分支，不必保持绝对稳定，但是一旦达到稳定状态，它们就可以被合并到master分支

- michael: dev切出的工作分支，用于合并多人的代码


![](http://p5s0bbd0l.bkt.clouddn.com/git_stream.png)


## Git变基...



