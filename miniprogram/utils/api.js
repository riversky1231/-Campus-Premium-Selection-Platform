function request(path, method='GET', data, admin=false) {
 const token=wx.getStorageSync(admin?'adminToken':'userToken')
 return new Promise((resolve,reject)=>wx.request({url:getApp().globalData.baseUrl+path,method,data,header:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},success:r=>{if(r.statusCode>=200&&r.statusCode<300)resolve(r.data);else{if(r.statusCode===401&&!admin)wx.removeStorageSync('userToken');const e=new Error((r.data&&r.data.message)||'请求失败');wx.showToast({title:e.message,icon:'none'});reject(e)}},fail:e=>{wx.showToast({title:'连接失败，请检查后端地址',icon:'none'});reject(e)}}))
}
module.exports={request}
