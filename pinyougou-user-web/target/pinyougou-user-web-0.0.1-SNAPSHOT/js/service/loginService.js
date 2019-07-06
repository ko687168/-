//服务层
app.service('loginService',function($http){
	//读取列表数据保存在表单中
	this.showName=function(){
		return $http.get('../login/name.do');
	}
});