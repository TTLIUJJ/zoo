# loonflow 源码解析


单页面：入口　

- index.html

- main.js


```html
<div id="app"></div>
```

==> 找到实例:

 ```html
 new Vue({
 	el: '#app',
 	router,
 	stroe,
 	i18n,
 	components: { App },
 	template: '<App/>'
 })
 ```

在实例中，引入了

- router
- stroe
- i18n

可以查看main.js文件，发现这个三个模块/对象的定义出处

```html
import { router } from './router/index'
import store from './store/index'
import i18n from './locale'
```

在 /src/store/index.js中，可以看到stroe模块的定义

```html
const store = new Vuex.Stroe({
	// 保存全局变量，修改时需要调用mutations中的函数
	// 值得一提的是，dispatch也需要通过调用mutations中的函数进行来修改state中的变量
	state: {
		currentPath: [],	// 当前页面的path, 比如/ticket/new
		oepnNames: [],	// 当前页面的title, 比如/工单系统/新建工单
		userid: null,		// 用户id
		expire: '',		// token过期时间
		username: '',		// 用户昵称
		token: ''			// cookie值
	},
	// 同步函数集合，调用函数时使用stroe.commit('fooFunction')
	// 主要目的是修改 state 中的值
	mutations: {
		//解析token值
	    decodeToken: (state, token) => {
	      state.userid = jwt(localStorage.token).user_id
	      state.expire = new Date(1000 * jwt(localStorage.token).exp)
	      state.username = jwt(localStorage.token).username
	    },
	},
	// 异步函数集合，调用函数时使用store.dispatch('barFunction')
	// 值得一提的是，如果想要修改 state 中的值， 需要通过调用mutations中的函数
	actions: {
	
	}
})
```


## 访问页面流程

在/src/router/index.js中，可以看到router的定义，使用history API，并且使用routes（路由规则，即页面之间的跳转）。

在每次进行路由跳转的时候，都会执行以下两个函数

- router.beforeEach
- router.afterEach

```html
/**
 * @param: to 即将要进入目标的路由对象
 * @param: from 当前导航正要离开的路由
 * @next: 一定要调用该方法来resolve这个钩子
 */
router.beforeEach((to, from, next) => {
	let now = new Date()
	//如果即将要跳转的路由不是 login或者404 
	//那么需要进行验证，token是否有效
	if(!['login', '404'].includes(to.name)){
		if(localStorage.token){
			//调用src/stroe/index.js中的decodeToken函数解析token
			stroe.commit('decodeToken')
			if(stroe.state.expire >= now){
				next()
			}
			else{
				//token过期，或者根本没有token值，跳转到login页面
				router.push({name: 'login'})
			}
		}
		//找不到token，跳转到login页面
		else{
			router.push({name: 'login'})
		}
	}
	else{
		next()
	}
})

```




在完成页面路由跳转之后，执行router.afterEach()，需要获取页面名称和当前页面数，需要查看Utils组件中定义的openNewPage和pagePath函数

```html
router.afterEach(to => {
	let newPage = Utils.openNewPage(router.app, to.name)
	// 找到 login、other或者ticket中的任一父页面
	if(newPage){
		if(newPage.children.length > 1){
			router.app.$store.commit('setOpenNames', newPage.name)
		}
		let PathArr = Utils.pagePath(router.app, to.name)
		router.app.$store.commit('setCurrentPath', pathArr)
	}
	iView.LoadingBar.finish()
})
```


## 工具类

在上面，有提到过获取当前页面的名称和路径，下面来看看具体是如何实现的


```html
/**
 * 过滤器得到的是数组, 并且只会有一个值符合要求
 * 在demo中，被过滤的元素有3个：otherRouter ,appRounters[0], appRouters[1], 
 * @Param vm 路由实例
 * @Param name 路由目的地
 */
Utils.openNewPage = function(vm, name){
	let openName = vm.$stroe.state.routers.filter(item => {
		if(item.children.length > 1){
			for(let i = 0; i < item.children.length; ++i){
				if(item.chidlren[i].name == name){
					return true
				}
			}
		}
		else{
			// demo中，Router中的子组件都是大于1的
			// 也就是说这行代码不会被执行
			return item.chdilren[0].name = name
		}
	})[0]
	
	// 返回一个 {} 或者 undefined
	return openName
}



Utils.pagePath = function(vm, name){
	let currentPath = []
	let pathArr = Utils.openNewPage(vm, name)
	if(pathArr.name == 'otherRouter'){
		for(let i = 0; i < pathArr.children.length; ++i){
			if(pathArr.children[i].name == name){
				currentPath = [
					{ path: '', name: name, title: pathArr.children[i].title }
				]
			}
		}
	}
	// 如果是 appRouters
	else{
		// /ticket和/manage的两个children数组都是大于1的
		if(pathArr.children.length > 1){
			for(let i = 0; i < pathArr.children.length; ++i){
				if(pathArr.children[i].name == name){
					// 假设 name == 'new'
					//currentPath[0] = { path: '', name: '/ticket', title: '工单系统' } 
					//currentPath[1] = { path: '', name: '/new', title: '新建工单' }
					currentPath = [
						{ path: '', name: pathArr.name, title: pathArr.title },
						{ path: '', name: name, title: pathArr.children[i].title }
					]
				}
			}
		}
		// 该分支语句在demo中不会被访问		
		else{
			if(pathArr.children[0].name == 'home_index'){
				currentPath = [
					{ path: '', name: name, title: pathArr.children[0].title }
				]
			}
			else{
				currentPath = [
					{ path: '', name: name, title: pathArr.children[0].title }
				]
			}
		}
	}
	return currentPath
}
```

从上面代码可以看出，得到的currentPath并不能直接使用，需要从currentPath数组中拼装path


